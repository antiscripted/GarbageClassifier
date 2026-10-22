import argparse
import hashlib
import json
from pathlib import Path
import statistics
import time

import numpy as np
import onnx
import onnxruntime as ort
from PIL import Image, ImageOps

from benchmark_candidates import class_result, classifier_input
from summarize_accuracy import image_metrics
from summarize_center import gate


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', type=Path, required=True)
    parser.add_argument('--reference-model', type=Path, required=True)
    parser.add_argument('--data', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    original = onnx.load(args.reference_model)
    assert hashlib.sha256(args.reference_model.read_bytes()).hexdigest() == 'fec39a575bd1a2d2434a1f763989bba0c2ecdd3c2cbfd52a27d9f923f53abd10'
    candidate = onnx.load(args.model)
    assert [v.SerializeToString() for v in original.graph.initializer] == [v.SerializeToString() for v in candidate.graph.initializer]
    changed = 0
    for before, after in zip(original.graph.node, candidate.graph.node, strict=True):
        if before.op_type == 'MatMulNBits':
            attributes = {a.name: a for a in after.attribute}
            assert attributes['accuracy_level'].i in (1, 4)
            changed += attributes['accuracy_level'].i == 4
            attributes['accuracy_level'].i = 1
        assert before.SerializeToString() == after.SerializeToString()
    assert changed in (60, 72)
    args.output.mkdir(parents=True, exist_ok=True)
    manifest = {r['id']: r for r in json.loads((args.data / 'manifest.json').read_text())}
    old = [json.loads(line) for line in (root / 'docs/experiments/center-scanner/center-results.jsonl').read_text().splitlines()]
    baseline = {r['id']: r for r in json.loads((root / 'docs/experiments/2026-10-20/corrected/baseline.json').read_text())['results']}
    labels = list(json.loads((root / 'docs/experiments/2026-10-21/siglip-export-report.json').read_text())['labels'].values())
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    options.inter_op_num_threads = 1
    options.add_session_config_entry('session.intra_op.allow_spinning', '0')
    options.add_session_config_entry('session.inter_op.allow_spinning', '0')
    session = ort.InferenceSession(str(args.model), options, providers=['CPUExecutionProvider'])
    zero = session.run(None, {'pixels': np.zeros((1, 3, 224, 224), np.float32)})[0][0]
    rows = []
    for index, previous in enumerate(old):
        source = manifest[previous['id']]
        with Image.open(args.data / source['path']) as file:
            pixels = classifier_input(ImageOps.exif_transpose(file).convert('RGB').crop(previous['center_box']))
        started = time.perf_counter()
        logits = session.run(None, {'pixels': pixels})[0][0]
        elapsed = (time.perf_counter() - started) * 1000
        assert np.isfinite(logits).all()
        probabilities = np.exp(logits - logits.max())
        prediction = class_result(probabilities / probabilities.sum(), labels)
        selected = gate(baseline[previous['id']]['predictions'], previous['center_box'])
        row = dict(previous, center=prediction, center_ms=elapsed,
                   predictions=[prediction] if selected else [], logits=logits.tolist())
        rows.append(row)
        if (index + 1) % 80 == 0:
            print(index + 1, '/', len(old), flush=True)
    report = dict(model_sha256=hashlib.sha256(args.model.read_bytes()).hexdigest(),
                  changed_compute_attributes=changed, all_weight_bytes_identical=True,
                  runtime=ort.__version__, provider='CPUExecutionProvider', threads=2,
                  device='desktop x86_64; not Android or physical phone',
                  host_median_ms=statistics.median(r['center_ms'] for r in rows),
                  host_p95_ms=float(np.percentile([r['center_ms'] for r in rows], 95)),
                  zero_logits=zero.tolist(),
                  top_label_changes=sum(a['center']['label'] != b['center']['label'] for a, b in zip(old, rows)),
                  maximum_probability_change=max(abs(a['center']['score'] - b['center']['score']) for a, b in zip(old, rows)),
                  realwaste=image_metrics([r for r in rows if r['dataset'] == 'realwaste'], 0.8),
                  taco=image_metrics([r for r in rows if r['dataset'] == 'taco'], 0.8))
    (args.output / 'results.jsonl').write_text(''.join(json.dumps(r) + '\n' for r in rows))
    (args.output / 'summary.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({k: v for k, v in report.items() if k not in ['realwaste', 'taco', 'operator_total_us']}, indent=2))
    print('RealWaste', report['realwaste']['counts'])
    print('TACO', report['taco']['counts'])


if __name__ == '__main__':
    main()
