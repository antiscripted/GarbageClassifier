import argparse
import hashlib
import json
from pathlib import Path
import time

import numpy as np
import onnx
import onnxruntime as ort
import torch
from transformers import DetrForObjectDetection


class ProposalDetector(torch.nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model

    def forward(self, pixels):
        output = self.model(pixel_values=pixels)
        return output.logits, output.pred_boxes


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    torch.set_num_threads(2)
    model = DetrForObjectDetection.from_pretrained(
        args.source, local_files_only=True, use_pretrained_backbone=False).eval()
    wrapper = ProposalDetector(model).eval()
    torch.manual_seed(42)
    example = torch.rand(1, 3, 512, 512)
    path = args.output / 'detector.onnx'
    if not path.exists():
        torch.onnx.export(wrapper, example, path, input_names=['pixels'],
                          output_names=['logits', 'boxes'], opset_version=17,
                          dynamo=False, do_constant_folding=True)
    onnx.checker.check_model(str(path))
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    options.inter_op_num_threads = 1
    session = ort.InferenceSession(str(path), options, providers=['CPUExecutionProvider'])
    with torch.inference_mode():
        expected = [item.numpy() for item in wrapper(example)]
    start = time.perf_counter()
    actual = session.run(None, {'pixels': example.numpy()})
    elapsed = (time.perf_counter() - start) * 1000
    errors = [float(np.abs(a - b).max()) for a, b in zip(actual, expected)]
    assert max(errors) < 0.001, errors
    black = session.run(None, {'pixels': np.zeros((1, 3, 512, 512), dtype=np.float32)})
    report = dict(source_revision='12c9760517571e60d5372d013021a509f555e6cc',
                  input_shape=[1, 3, 512, 512], output_shapes=[list(item.shape) for item in actual],
                  labels=model.config.id2label, bytes=path.stat().st_size,
                  sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                  max_absolute_errors=errors, first_host_inference_ms=elapsed,
                  black_input_outputs=[item.tolist() for item in black],
                  training_performed=False, quantization=None)
    (args.output / 'export-report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({key: value for key, value in report.items() if key != 'black_input_outputs'}), flush=True)


if __name__ == '__main__':
    main()
