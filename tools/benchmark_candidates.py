import argparse
import hashlib
import json
import os
from pathlib import Path
import resource
import statistics
import time

import numpy as np
from PIL import Image, ImageOps

from verify_model import prepare_image


ROOT = Path(__file__).resolve().parents[1]
SUPPORTED = {"Food waste", "Glass", "Metal", "Paper", "Plastic"}
EFFICIENTNET_LABELS = [
    "aerosol_cans", "aluminum_food_cans", "aluminum_soda_cans", "cardboard_boxes",
    "cardboard_packaging", "clothing", "coffee_grounds", "disposable_plastic_cutlery",
    "eggshells", "food_waste", "glass_beverage_bottles", "glass_cosmetic_containers",
    "glass_food_jars", "magazines", "newspaper", "office_paper", "paper_cups",
    "plastic_cup_lids", "plastic_detergent_bottles", "plastic_food_containers",
    "plastic_shopping_bags", "plastic_soda_bottles", "plastic_straws", "plastic_trash_bags",
    "plastic_water_bottles", "shoes", "steel_food_cans", "styrofoam_cups",
    "styrofoam_food_containers", "tea_bags",
]


def material(label):
    if label in SUPPORTED:
        return label
    if label in {"Biological", "biodegradable", "food_waste", "coffee_grounds", "eggshells", "tea_bags"}:
        return "Food waste"
    if label in {"Cardboard", "cardboard", "paper", "cardboard_boxes", "cardboard_packaging",
                 "magazines", "newspaper", "office_paper"}:
        return "Paper"
    if label in {"glass", "glass_beverage_bottles", "glass_cosmetic_containers", "glass_food_jars"}:
        return "Glass"
    if label in {"metal", "aluminum_food_cans", "aluminum_soda_cans", "steel_food_cans"}:
        return "Metal"
    if label == "plastic" or label.startswith("plastic_") or label == "disposable_plastic_cutlery":
        return "Plastic"
    return None


def intersection_over_union(a, b):
    intersection = max(0, min(a[2], b[2]) - max(a[0], b[0])) * max(0, min(a[3], b[3]) - max(a[1], b[1]))
    union = (a[2] - a[0]) * (a[3] - a[1]) + (b[2] - b[0]) * (b[3] - b[1]) - intersection
    return intersection / union if union > 0 else 0


def class_result(scores, labels):
    order = np.argsort(scores)
    best, second = int(order[-1]), int(order[-2])
    return dict(label=labels[best], material=material(labels[best]),
                score=float(scores[best]), margin=float(scores[best] - scores[second]))


def classifier_input(image):
    data = np.asarray(image.resize((224, 224), Image.Resampling.BILINEAR), dtype=np.float32)
    return ((data / 255 - 0.5) / 0.5).transpose(2, 0, 1).copy()[None]


class LiteDetector:
    kind = "detector"

    def __init__(self, path):
        import tensorflow as tf
        self.engine = tf.lite.Interpreter(model_path=str(path), num_threads=2)
        self.engine.allocate_tensors()
        self.input = self.engine.get_input_details()[0]
        self.output = self.engine.get_output_details()[0]
        self.size = int(self.input["shape"][1])
        self.labels = json.loads((ROOT / "docs/model/contract.json").read_text())["labels"]
        self.metadata = dict(input_shape=self.input["shape"].tolist(),
                             output_shape=self.output["shape"].tolist(), parameter_count=None)

    def predict(self, image):
        data = prepare_image(image, self.size)
        self.engine.set_tensor(self.input["index"], data)
        start = time.perf_counter()
        self.engine.invoke()
        elapsed = (time.perf_counter() - start) * 1000
        output = self.engine.get_tensor(self.output["index"])[0]
        scale = min(self.size / image.width, self.size / image.height)
        pad_x = (self.size - image.width * scale) / 2
        pad_y = (self.size - image.height * scale) / 2
        candidates = []
        for index in np.flatnonzero(output[4:].max(axis=0) >= 0.25):
            result = class_result(output[4:, index], self.labels)
            x, y, width, height = output[:4, index] * self.size
            box = [(x - width / 2 - pad_x) / scale, (y - height / 2 - pad_y) / scale,
                   (x + width / 2 - pad_x) / scale, (y + height / 2 - pad_y) / scale]
            box = np.clip(box, 0, [image.width, image.height, image.width, image.height]).tolist()
            if box[2] - box[0] < 2 or box[3] - box[1] < 2:
                continue
            candidates.append(dict(**result, box=box))
        kept = []
        for candidate in sorted(candidates, key=lambda row: row["score"], reverse=True):
            if any(row["label"] == candidate["label"] and
                   intersection_over_union(row["box"], candidate["box"]) > 0.45 for row in kept):
                continue
            kept.append(candidate)
            if len(kept) == 20:
                break
        return kept, elapsed


class EfficientNet:
    kind = "classifier"

    def __init__(self, path):
        import tensorflow as tf
        import keras
        tf.config.threading.set_intra_op_parallelism_threads(2)
        tf.config.threading.set_inter_op_parallelism_threads(1)
        self.model = keras.models.model_from_json((path.parent / "config.json").read_text())
        self.model.load_weights(path)
        self.infer = tf.function(lambda value: self.model(value, training=False),
                                 input_signature=[tf.TensorSpec([1, 224, 224, 3], tf.float32)])
        self.metadata = dict(parameter_count=self.model.count_params(), input_shape=[1, 224, 224, 3],
                             output_shape=[1, 30], normalization="RGB 0..255; embedded EfficientNet rescaling")

    def predict(self, image):
        data = np.asarray(image.resize((224, 224), Image.Resampling.NEAREST), dtype=np.float32)[None]
        start = time.perf_counter()
        scores = self.infer(data).numpy()[0]
        elapsed = (time.perf_counter() - start) * 1000
        return [class_result(scores, EFFICIENTNET_LABELS)], elapsed


class Siglip:
    kind = "classifier"

    def __init__(self, path):
        import torch
        from transformers import SiglipForImageClassification
        torch.set_num_threads(2)
        self.torch = torch
        self.model = SiglipForImageClassification.from_pretrained(path.parent, local_files_only=True).eval()
        self.labels = [self.model.config.id2label[index] for index in range(self.model.config.num_labels)]
        self.metadata = dict(parameter_count=sum(p.numel() for p in self.model.parameters()),
                             input_shape=[1, 3, 224, 224], output_shape=[1, len(self.labels)],
                             normalization="RGB bilinear resize; (pixel/255 - 0.5)/0.5")

    def predict(self, image):
        tensor = self.torch.from_numpy(classifier_input(image))
        start = time.perf_counter()
        with self.torch.inference_mode():
            scores = self.model(pixel_values=tensor).logits.softmax(-1)[0].numpy()
        elapsed = (time.perf_counter() - start) * 1000
        return [class_result(scores, self.labels)], elapsed


class OnnxClassifier:
    kind = "classifier"

    def __init__(self, path, metadata=None):
        import onnxruntime as ort
        options = ort.SessionOptions()
        options.intra_op_num_threads = 2
        options.inter_op_num_threads = 1
        options.execution_mode = ort.ExecutionMode.ORT_SEQUENTIAL
        self.session = ort.InferenceSession(str(path), options, providers=["CPUExecutionProvider"])
        report = json.loads((metadata or path.parent / "export-report.json").read_text())
        self.labels = [report["labels"][str(index)] for index in range(10)]
        self.metadata = dict(parameter_count=report["parameters"], input_shape=[1, 3, 224, 224],
                             output_shape=[1, 10], runtime=ort.__version__)

    def predict(self, image):
        data = classifier_input(image)
        start = time.perf_counter()
        logits = self.session.run(None, {"pixels": data})[0][0]
        elapsed = (time.perf_counter() - start) * 1000
        probabilities = np.exp(logits - logits.max())
        probabilities /= probabilities.sum()
        return [class_result(probabilities, self.labels)], elapsed


class MobileDetector:
    kind = "detector"

    def __init__(self, path):
        import torch
        from torchvision.models.detection import fasterrcnn_mobilenet_v3_large_320_fpn
        torch.set_num_threads(2)
        self.torch = torch
        checkpoint = torch.load(path, weights_only=True, map_location="cpu")
        self.model = fasterrcnn_mobilenet_v3_large_320_fpn(
            weights=None, weights_backbone=None, num_classes=checkpoint["num_model_classes"]).eval()
        self.model.load_state_dict(checkpoint["model_state_dict"])
        self.labels = checkpoint["model_label_to_name"]
        self.metadata = dict(parameter_count=sum(p.numel() for p in self.model.parameters()),
                             input_shape="RGB CHW 0..1; internal min-size 320 max-size 640",
                             output_shape="boxes[N,4], labels[N], scores[N]", margin_available=False)

    def predict(self, image):
        data = np.asarray(image, dtype=np.float32) / 255
        tensor = self.torch.from_numpy(data.transpose(2, 0, 1).copy())
        start = time.perf_counter()
        with self.torch.inference_mode():
            outputs = self.model([tensor])[0]
        elapsed = (time.perf_counter() - start) * 1000
        result = []
        for box, label, score in zip(outputs["boxes"], outputs["labels"], outputs["scores"]):
            if float(score) < 0.25:
                continue
            name = self.labels[int(label)]
            result.append(dict(label=name, material=material(name), score=float(score),
                               margin=None, box=box.tolist()))
        return result[:20], elapsed


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--engine", choices=["lite", "efficientnet", "siglip", "siglip-onnx", "mobile-detector"], required=True)
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--data", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--skip-trashnet", action="store_true")
    parser.add_argument("--only-ids", type=Path)
    parser.add_argument("--proposals-only", action="store_true")
    parser.add_argument("--reuse", type=Path)
    parser.add_argument("--metadata", type=Path)
    args = parser.parse_args()
    rows = json.loads((args.data / "manifest.json").read_text())
    if args.skip_trashnet:
        rows = [row for row in rows if row["dataset"] != "trashnet"]
    if args.only_ids:
        selected = set(json.loads(args.only_ids.read_text()))
        rows = [row for row in rows if row["id"] in selected]
    baseline = {row["id"]: row for row in json.loads(args.baseline.read_text())["results"]} if args.baseline else {}
    model_sha = hashlib.sha256(args.model.read_bytes()).hexdigest()
    reused = {}
    if args.reuse:
        cached = json.loads(args.reuse.read_text())
        assert cached["model_sha256"] == model_sha
        reused = {row["id"]: row for row in cached["results"]}
    constructors = {"lite": LiteDetector, "efficientnet": EfficientNet, "siglip": Siglip,
                    "siglip-onnx": lambda path: OnnxClassifier(path, args.metadata),
                    "mobile-detector": MobileDetector}
    start = time.perf_counter()
    engine = constructors[args.engine](args.model)
    init_ms = (time.perf_counter() - start) * 1000
    for _ in range(5):
        engine.predict(Image.new("RGB", (512, 384), (114, 114, 114)))
    results = []
    durations = []
    reused_crops = 0
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.with_suffix(".partial.jsonl").write_text("")
    for number, row in enumerate(rows, 1):
        with Image.open(args.data / row["path"]) as file:
            image = ImageOps.exif_transpose(file).convert("RGB")
        if row["dataset"] == "taco":
            assert list(image.size) == row["annotated_size"], row["id"]
        record = dict(id=row["id"], dataset=row["dataset"], category=row["category"], expected=row["expected"])
        start = time.perf_counter()
        if not args.proposals_only and (row["dataset"] != "taco" or engine.kind == "detector"):
            predictions, elapsed = engine.predict(image)
            record["predictions"] = predictions
            durations.append(elapsed)
        if row["dataset"] == "taco":
            sx = image.width / row["annotated_size"][0]
            sy = image.height / row["annotated_size"][1]
            record["targets"] = [dict(target, box=[target["box"][0] * sx, target["box"][1] * sy,
                                                     target["box"][2] * sx, target["box"][3] * sy])
                                 for target in row["targets"]]
            if engine.kind == "classifier" and not args.proposals_only:
                record["oracle_crops"] = []
                for target in record["targets"]:
                    if target["box"][2] - target["box"][0] < 2 or target["box"][3] - target["box"][1] < 2:
                        continue
                    predictions, elapsed = engine.predict(image.crop(tuple(target["box"])))
                    durations.append(elapsed)
                    record["oracle_crops"].append(dict(target=target, prediction=predictions[0]))
        if engine.kind == "classifier" and row["id"] in baseline:
            record["hybrid"] = []
            cached_crops = {tuple(item["box"]): item for item in reused.get(row["id"], {}).get("hybrid", [])}
            for proposal in baseline[row["id"]]["predictions"][:5]:
                saved = cached_crops.get(tuple(proposal["box"]))
                if saved is None:
                    predictions, elapsed = engine.predict(image.crop(tuple(proposal["box"])))
                    durations.append(elapsed)
                else:
                    predictions = [saved]
                    reused_crops += 1
                record["hybrid"].append(dict(predictions[0], box=proposal["box"],
                                             proposal_score=proposal["score"]))
        record["total_ms"] = (time.perf_counter() - start) * 1000
        results.append(record)
        with args.report.with_suffix(".partial.jsonl").open("a") as checkpoint:
            checkpoint.write(json.dumps(record) + "\n")
        if number % 50 == 0:
            print(f"{args.engine}: {number}/{len(rows)}", flush=True)
    report = dict(engine=args.engine, kind=engine.kind,
                  model_sha256=model_sha,
                  model_bytes=args.model.stat().st_size, metadata=engine.metadata, init_ms=init_ms,
                  host_inference_median_ms=statistics.median(durations) if durations else None,
                  host_inference_p95_ms=float(np.percentile(durations, 95)) if durations else None,
                  host_peak_rss_mib=resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1024,
                  cpu_threads=2, timing_is_not_phone_timing=True, environment=os.uname().machine,
                  reused_crops=reused_crops, measured_inferences=len(durations),
                  proposals_only=args.proposals_only, results=results)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({key: value for key, value in report.items() if key != "results"}, indent=2), flush=True)


if __name__ == "__main__":
    main()
