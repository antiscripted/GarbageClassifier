import argparse
import hashlib
import json
from pathlib import Path
import statistics
import time
from zipfile import ZipFile

import numpy as np
from PIL import Image, ImageOps

from benchmark_candidates import LiteDetector, intersection_over_union, material


def unique_proposals(proposals, limit=5):
    kept = []
    for proposal in sorted(proposals, key=lambda item: item['score'], reverse=True):
        if all(intersection_over_union(proposal['box'], item['box']) <= 0.45 for item in kept):
            kept.append(proposal)
            if len(kept) == limit:
                break
    return kept


class YoloProposals:
    def __init__(self, path):
        import torch
        from ultralytics import YOLO
        torch.set_num_threads(2)
        self.model = YOLO(path)
        self.torch = torch
        self.metadata = dict(input_size=512, preprocessing='RGB uniform letterbox 114',
                             labels=self.model.names, proposal_threshold=0.25)

    def predict(self, image):
        output = self.model.predict(image, imgsz=512, conf=0.25, iou=0.45, max_det=20,
                                    agnostic_nms=True, rect=False, device='cpu', verbose=False)[0]
        self.torch.set_num_threads(2)
        predictions = []
        for box, label, score in zip(output.boxes.xyxy, output.boxes.cls, output.boxes.conf):
            name = self.model.names[int(label)]
            predictions.append(dict(label=name, material='Food waste' if name == 'organic' else material(name),
                                    score=float(score), margin=None, box=box.tolist()))
        return predictions


class CocoProposals:
    def __init__(self, path):
        import tensorflow as tf
        self.engine = tf.lite.Interpreter(model_path=str(path), num_threads=2)
        self.engine.allocate_tensors()
        self.input = self.engine.get_input_details()[0]
        self.outputs = self.engine.get_output_details()
        with ZipFile(path) as archive:
            self.labels = archive.read('labelmap.txt').decode().splitlines()
        self.supported = {'bottle', 'wine glass', 'cup', 'fork', 'spoon', 'bowl', 'banana', 'apple',
                          'sandwich', 'orange', 'broccoli', 'carrot', 'hot dog', 'pizza', 'donut',
                          'cake', 'book', 'vase'}
        self.metadata = dict(input_shape=self.input['shape'].tolist(), input_type='uint8',
                             preprocessing='RGB bilinear resize, raw uint8', labels=self.labels,
                             allowed_proposal_labels=sorted(self.supported), proposal_threshold=0.25)

    def predict(self, image):
        data = np.asarray(image.resize((320, 320), Image.Resampling.BILINEAR), dtype=np.uint8)[None]
        self.engine.set_tensor(self.input['index'], data)
        self.engine.invoke()
        boxes, labels, scores, count = [self.engine.get_tensor(item['index'])[0] for item in self.outputs]
        predictions = []
        for index in range(int(count)):
            name = self.labels[int(labels[index])]
            if scores[index] < 0.25 or name not in self.supported:
                continue
            top, left, bottom, right = boxes[index]
            box = np.clip([left * image.width, top * image.height, right * image.width, bottom * image.height],
                          0, [image.width, image.height, image.width, image.height]).tolist()
            if box[2] - box[0] >= 2 and box[3] - box[1] >= 2:
                predictions.append(dict(label=name, material=None, score=float(scores[index]),
                                        margin=None, box=box))
        return predictions


class TiledProposals:
    def __init__(self, path):
        self.detector = LiteDetector(path)
        self.metadata = dict(self.detector.metadata, tiles='full image and four overlapping 60% corner crops',
                             proposal_threshold=0.25)

    def predict(self, image):
        predictions = self.detector.predict(image)[0]
        width, height = round(image.width * 0.6), round(image.height * 0.6)
        for left, top in [(0, 0), (image.width - width, 0),
                          (0, image.height - height), (image.width - width, image.height - height)]:
            for proposal in self.detector.predict(image.crop((left, top, left + width, top + height)))[0]:
                a, b, c, d = proposal['box']
                predictions.append(dict(proposal, box=[a + left, b + top, c + left, d + top]))
        return predictions


class DetrProposals:
    def __init__(self, path):
        import torch
        from transformers import DetrForObjectDetection
        torch.set_num_threads(2)
        self.torch = torch
        self.model = DetrForObjectDetection.from_pretrained(
            path.parent, local_files_only=True, use_pretrained_backbone=False).eval()
        self.labels = self.model.config.id2label
        self.metadata = dict(input_shape=[1, 3, 512, 512], labels=self.labels,
                             preprocessing='RGB bilinear square resize; ImageNet mean/std; full valid mask',
                             upstream_preprocessing='aspect-preserving shortest edge 800, longest edge 1333',
                             output_shapes=[[1, 10, 8], [1, 10, 4]], proposal_threshold=0.25,
                             parameter_count=sum(p.numel() for p in self.model.parameters()))

    def predict(self, image):
        pixels = np.asarray(image.resize((512, 512), Image.Resampling.BILINEAR), dtype=np.float32) / 255
        pixels = (pixels - np.array([0.485, 0.456, 0.406], dtype=np.float32)) / np.array(
            [0.229, 0.224, 0.225], dtype=np.float32)
        tensor = self.torch.from_numpy(pixels.transpose(2, 0, 1).copy()[None])
        with self.torch.inference_mode():
            output = self.model(pixel_values=tensor)
            scores, labels = output.logits.softmax(-1)[0, :, :-1].max(-1)
        predictions = []
        for raw_box, label, score in zip(output.pred_boxes[0], labels, scores):
            if float(score) < 0.25:
                continue
            x, y, width, height = raw_box.tolist()
            box = np.clip([(x - width / 2) * image.width, (y - height / 2) * image.height,
                           (x + width / 2) * image.width, (y + height / 2) * image.height],
                          0, [image.width, image.height, image.width, image.height]).tolist()
            if box[2] - box[0] < 2 or box[3] - box[1] < 2:
                continue
            name = self.labels[int(label)]
            predictions.append(dict(label=name, material='Food waste' if name == 'bio' else material(name),
                                    score=float(score), margin=None, box=box))
        return predictions


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--engine', choices=['yolo', 'coco', 'tiled', 'detr'], required=True)
    parser.add_argument('--model', type=Path, required=True)
    parser.add_argument('--data', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--dataset', choices=['taco', 'realwaste', 'both'], default='taco')
    args = parser.parse_args()
    engine = {'yolo': YoloProposals, 'coco': CocoProposals, 'tiled': TiledProposals,
              'detr': DetrProposals}[args.engine](args.model)
    for _ in range(3):
        engine.predict(Image.new('RGB', (512, 384), (114, 114, 114)))
    rows = json.loads((args.data / 'manifest.json').read_text())
    rows = [row for row in rows if row['dataset'] == args.dataset or
            args.dataset == 'both' and row['dataset'] in ['taco', 'realwaste']]
    results, durations = [], []
    for number, row in enumerate(rows, 1):
        with Image.open(args.data / row['path']) as file:
            image = ImageOps.exif_transpose(file).convert('RGB')
        if row['dataset'] == 'taco':
            assert list(image.size) == row['annotated_size'], row['id']
        start = time.perf_counter()
        predictions = engine.predict(image)
        elapsed = (time.perf_counter() - start) * 1000
        durations.append(elapsed)
        result = {key: row[key] for key in ['id', 'dataset', 'category', 'expected']}
        result.update(predictions=unique_proposals(predictions), all_proposals=predictions, total_ms=elapsed)
        if 'targets' in row:
            result['targets'] = row['targets']
        results.append(result)
        if number % 50 == 0:
            print(f'{args.engine}: {number}/{len(rows)}', flush=True)
    report = dict(kind='detector', engine=args.engine, model_sha256=hashlib.sha256(args.model.read_bytes()).hexdigest(),
                  model_bytes=args.model.stat().st_size, metadata=engine.metadata, cpu_threads=2,
                  host_inference_median_ms=statistics.median(durations),
                  host_inference_p95_ms=float(np.percentile(durations, 95)),
                  timing_is_not_phone_timing=True, proposal_limit=5, results=results)
    args.report.write_text(json.dumps(report, indent=2) + '\n')
    for threshold in [0.25, 0.5]:
        targets = localized = scenes = 0
        for row in results:
            if row['dataset'] != 'taco':
                continue
            scenes += bool(row['predictions'])
            for target in row['targets']:
                targets += 1
                localized += any(intersection_over_union(target['box'], item['box']) >= threshold
                                 for item in row['predictions'])
        print(dict(box_iou_threshold=threshold, targets=targets, targets_with_proposal=localized,
                   scenes_with_proposal=scenes, host_ms=report['host_inference_median_ms']), flush=True)


if __name__ == '__main__':
    main()
