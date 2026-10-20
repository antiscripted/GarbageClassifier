import argparse
import hashlib
import json
import statistics
import time
from pathlib import Path

import numpy as np
import tensorflow as tf
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def prepare_image(image, size):
    image = image.convert("RGB")
    scale = min(size / image.width, size / image.height)
    resized = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.BILINEAR)
    square = Image.new("RGB", (size, size), (114, 114, 114))
    square.paste(resized, ((size - resized.width) // 2, (size - resized.height) // 2))
    return np.asarray(square, dtype=np.float32)[None] / 255


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path)
    parser.add_argument("--write-fixture", action="store_true")
    args = parser.parse_args()
    contract = json.loads((ROOT / "docs/model/contract.json").read_text())
    model = ROOT / "app/src/main/assets/trash_detector.tflite"
    digest = hashlib.sha256(model.read_bytes()).hexdigest()
    assert digest == contract["sha256"]
    engine = tf.lite.Interpreter(model_path=str(model), num_threads=2)
    engine.allocate_tensors()
    inputs = engine.get_input_details()
    outputs = engine.get_output_details()
    assert len(inputs) == len(outputs) == 1
    for tensor, expected in [(inputs[0], contract["input"]), (outputs[0], contract["output"])]:
        assert tensor["shape"].tolist() == expected["shape"]
        assert tensor["dtype"] == np.float32
        assert tensor["quantization"] == (0.0, 0)
    quantized = [t for t in engine.get_tensor_details() if t["quantization_parameters"]["scales"].size]
    assert len(quantized) == 65
    assert all(t["dtype"] == np.int8 for t in quantized)
    samples = []
    for filename in ["plastic1.jpg", "metal1.jpg", "paper1.jpg", "glass1.jpg", "trash1.jpg", "cardboard1.jpg"]:
        path = ROOT / "app/src/androidTest/assets" / filename
        image = Image.open(path).convert("RGB")
        engine.set_tensor(inputs[0]["index"], prepare_image(image, 320))
        engine.invoke()
        output = engine.get_tensor(outputs[0]["index"])
        assert np.isfinite(output).all()
        scores = output[0, 4:]
        class_id, candidate = np.unravel_index(np.argmax(scores), scores.shape)
        confidence = float(scores[class_id, candidate])
        box = output[0, :4, candidate].tolist()
        samples.append({"file": filename, "label": contract["labels"][class_id],
                        "confidence": confidence, "normalized_xywh": box})
        if filename == "metal1.jpg" and args.write_fixture:
            output.astype("<f4").tofile(ROOT / "app/src/test/resources/metal1-output.f32")
    engine.set_tensor(inputs[0]["index"], np.zeros(inputs[0]["shape"], dtype=np.float32))
    durations = []
    for index in range(35):
        start = time.perf_counter()
        engine.invoke()
        elapsed = (time.perf_counter() - start) * 1000
        if index >= 5:
            durations.append(elapsed)
    blank = engine.get_tensor(outputs[0]["index"])
    assert float(blank[0, 4:].max()) < 0.25
    report = {"sha256": digest, "input_shape": inputs[0]["shape"].tolist(),
              "output_shape": outputs[0]["shape"].tolist(), "float32_io": True,
              "int8_weight_tensors": len(quantized), "black_image_max_score": float(blank[0, 4:].max()),
              "host_cpu_median_ms": statistics.median(durations), "host_cpu_runs": len(durations),
              "host_timing_is_not_phone_timing": True, "samples": samples}
    rendered = json.dumps(report, indent=2) + "\n"
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered)


if __name__ == "__main__":
    main()
