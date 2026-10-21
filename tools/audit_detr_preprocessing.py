import argparse
import hashlib
import json
from pathlib import Path
import time

from PIL import Image, ImageOps
import torch
from transformers import DetrForObjectDetection, DetrImageProcessor

from benchmark_candidates import intersection_over_union
from benchmark_proposals import unique_proposals


def covered(targets, proposals):
    return sum(any(intersection_over_union(target['box'], proposal['box']) >= 0.5
                   for proposal in proposals) for target in targets)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', type=Path, required=True)
    parser.add_argument('--data', type=Path, required=True)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    torch.set_num_threads(2)
    model = DetrForObjectDetection.from_pretrained(
        args.model, local_files_only=True, use_pretrained_backbone=False).eval()
    processor = DetrImageProcessor.from_pretrained(args.model, local_files_only=True)
    baseline = {row['id']: row for row in json.loads(args.baseline.read_text())['results']}
    rows = [row for row in json.loads((args.data / 'manifest.json').read_text()) if row['dataset'] == 'taco']
    rows = sorted(rows, key=lambda row: hashlib.sha256(row['id'].encode()).hexdigest())[:16]
    results = []
    for row in rows:
        with Image.open(args.data / row['path']) as file:
            image = ImageOps.exif_transpose(file).convert('RGB')
        pixels = processor(images=image, return_tensors='pt')
        start = time.perf_counter()
        with torch.inference_mode():
            output = model(**pixels)
        elapsed = (time.perf_counter() - start) * 1000
        detections = processor.post_process_object_detection(
            output, threshold=0.25, target_sizes=torch.tensor([[image.height, image.width]]))[0]
        proposals = []
        for box, score, label in zip(detections['boxes'], detections['scores'], detections['labels']):
            box = box.tolist()
            box = [max(0, min(value, image.width if index % 2 == 0 else image.height))
                   for index, value in enumerate(box)]
            if box[2] - box[0] >= 2 and box[3] - box[1] >= 2:
                proposals.append(dict(box=box, score=float(score), label=model.config.id2label[int(label)]))
        kept = unique_proposals(proposals)
        result = dict(id=row['id'], input_shape=list(pixels['pixel_values'].shape),
                      predictions=kept, targets=row['targets'], host_ms=elapsed,
                      upstream_covered=covered(row['targets'], kept),
                      square_covered=covered(row['targets'], baseline[row['id']]['predictions']))
        results.append(result)
        args.output.write_text(json.dumps(dict(
            policy='First 16 TACO IDs by SHA-256, fixed before inference; published processor; no training',
            results=results), indent=2) + '\n')
        print(row['id'], result['square_covered'], result['upstream_covered'], flush=True)


if __name__ == '__main__':
    main()
