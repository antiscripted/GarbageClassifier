import argparse
from collections import Counter
import json
from pathlib import Path

from benchmark_candidates import ROOT, SUPPORTED, intersection_over_union


def eligible(prediction, threshold):
    return prediction.get("material") in SUPPORTED and prediction["score"] >= threshold and (
        prediction.get("margin") is None or prediction["margin"] >= 0.15)


def recommend(material):
    return "wet" if material == "Food waste" else "dry" if material in SUPPORTED else None


def image_metrics(rows, threshold, field="predictions"):
    counts = Counter(total=len(rows))
    classes = {}
    confusion = {}
    for row in rows:
        expected = row["expected"]
        counts["known" if expected else "unsupported"] += 1
        key = row["category"]
        per_class = classes.setdefault(key, Counter(total=0))
        per_class["total"] += 1
        candidates = row.get(field, [])
        best = max(candidates, key=lambda item: item["score"]) if candidates else None
        accepted = best is not None and eligible(best, threshold)
        if not accepted:
            counts["rejected"] += 1
            counts["known_rejected" if expected else "unsupported_rejected"] += 1
            per_class["rejected"] += 1
            if not candidates:
                counts["undetected"] += 1
            continue
        counts["accepted"] += 1
        counts["known_accepted" if expected else "unsupported_accepted"] += 1
        per_class["accepted"] += 1
        predicted = best["material"]
        confusion.setdefault(expected or "Unsupported", Counter())[predicted] += 1
        if expected == predicted:
            counts["correct"] += 1
            per_class["correct"] += 1
        else:
            counts["wrong_material" if expected else "unsupported_acceptance"] += 1
            per_class["wrong"] += 1
            if best["score"] >= 0.8:
                counts["confident_wrong" if expected else "confident_unsupported_acceptance"] += 1
        if expected and recommend(predicted) != recommend(expected):
            counts["wrong_bin"] += 1
            per_class["wrong_bin"] += 1
    return dict(counts=counts, per_class=classes, confusion=confusion)


def detection_metrics(rows, threshold, field="predictions"):
    counts = Counter(images=len(rows))
    classes = {}
    for row in rows:
        targets = row["targets"]
        counts["targets"] += len(targets)
        counts["known_targets"] += sum(target["expected"] is not None for target in targets)
        counts["multiple_object_images"] += len(targets) > 1
        predictions = [item for item in row.get(field, []) if eligible(item, threshold)]
        counts["accepted_boxes"] += len(predictions)
        available = set(range(len(targets)))
        for prediction in sorted(predictions, key=lambda item: item["score"], reverse=True):
            matches = [(intersection_over_union(prediction["box"], targets[index]["box"]), index)
                       for index in available]
            overlap, index = max(matches, default=(0, -1))
            if overlap < 0.5:
                counts["unmatched_boxes"] += 1
                continue
            available.remove(index)
            target = targets[index]
            counts["localized"] += 1
            per_class = classes.setdefault(target["category"], Counter())
            per_class["localized"] += 1
            if target["expected"] == prediction["material"]:
                counts["correct_material_and_box"] += 1
                per_class["correct"] += 1
            else:
                counts["wrong_material" if target["expected"] else "unsupported_acceptance"] += 1
                per_class["wrong"] += 1
                if prediction["score"] >= 0.8:
                    counts["confident_wrong" if target["expected"] else "confident_unsupported_acceptance"] += 1
            if target["expected"] and recommend(target["expected"]) != recommend(prediction["material"]):
                counts["wrong_bin"] += 1
        for index, target in enumerate(targets):
            per_class = classes.setdefault(target["category"], Counter())
            per_class["targets"] += 1
            if index in available:
                counts["missed_targets"] += 1
                per_class["missed"] += 1
    return dict(counts=counts, per_class=classes, matching="class-agnostic greedy IoU >= 0.5; not mAP")


def oracle_rows(rows):
    return [dict(expected=crop["target"]["expected"], category=crop["target"]["category"],
                 predictions=[crop["prediction"]])
            for row in rows for crop in row.get("oracle_crops", [])]


def historical(path):
    return [dict(id="trashnet/" + row["image"], dataset="trashnet",
                 category=row["image"].split("/")[1],
                 expected=None if row["expected"] == "Unspecified trash" else row["expected"],
                 predictions=[dict(label=row["predicted"],
                                   material=row["predicted"] if row["predicted"] in SUPPORTED else None,
                                   score=row["confidence"], margin=row["margin"])])
            for row in json.loads(path.read_text())]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    summary = {}
    for name in ["baseline", "float", "efficientnet", "siglip", "siglip-int8", "mobile-detector"]:
        path = args.directory / (name + ".json")
        if not path.exists():
            continue
        report = json.loads(path.read_text())
        rows = list(report["results"])
        if name in ["baseline", "float"]:
            filename = "best_10_imgsz320_int8" if name == "baseline" else "best_20_float32"
            rows += historical(ROOT / "docs/validation" / (filename + "-240-results.json"))
        summary[name] = {key: value for key, value in report.items() if key != "results"}
        for threshold in [0.6, 0.8]:
            metrics = {}
            for dataset in ["trashnet", "realwaste"]:
                images = [row for row in rows if row["dataset"] == dataset]
                metrics[dataset] = image_metrics(images, threshold)
                if any("hybrid" in row for row in images):
                    metrics[dataset + "_hybrid"] = image_metrics(images, threshold, field="hybrid")
            scenes = [row for row in rows if row["dataset"] == "taco"]
            if report["kind"] == "detector":
                metrics["taco"] = detection_metrics(scenes, threshold)
            else:
                metrics["taco_oracle_crops"] = image_metrics(oracle_rows(scenes), threshold)
                metrics["taco_hybrid"] = detection_metrics(scenes, threshold, field="hybrid")
            summary[name][str(threshold)] = metrics
    args.output.write_text(json.dumps(summary, indent=2) + "\n")
    for name, model in summary.items():
        print(name, "host ms", round(model["host_inference_median_ms"], 2))
        for threshold in ["0.6", "0.8"]:
            for dataset, result in model[threshold].items():
                print(threshold, dataset, dict(result["counts"]))


if __name__ == "__main__":
    main()
