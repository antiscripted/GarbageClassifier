import argparse
import hashlib
import json
from pathlib import Path
import statistics
import time

import numpy as np
import onnx
import onnxruntime as ort
from onnxruntime.quantization import QuantType, quantize_dynamic
import torch
from transformers import SiglipForImageClassification


class MaterialClassifier(torch.nn.Module):
    def __init__(self, model):
        super().__init__()
        self.model = model

    def forward(self, pixels):
        return self.model(pixel_values=pixels).logits


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--weight-only", action="store_true")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    torch.set_num_threads(2)
    model = SiglipForImageClassification.from_pretrained(
        args.source, local_files_only=True, attn_implementation="eager").eval()
    wrapper = MaterialClassifier(model).eval()
    float_path = args.output / "material-float.onnx"
    quantized_path = args.output / ("material-int4.onnx" if args.weight_only else "material-int8.onnx")
    example = torch.zeros(1, 3, 224, 224)
    if not float_path.exists():
        torch.onnx.export(wrapper, example, float_path, input_names=["pixels"],
                          output_names=["logits"], opset_version=17, dynamo=False,
                          do_constant_folding=True)
    onnx.checker.check_model(str(float_path))
    if not quantized_path.exists():
        if args.weight_only:
            from onnxruntime.quantization.matmul_nbits_quantizer import (
                DefaultWeightOnlyQuantConfig, MatMulNBitsQuantizer
            )
            config = DefaultWeightOnlyQuantConfig(block_size=32, is_symmetric=True,
                                                   accuracy_level=1, bits=4,
                                                   op_types_to_quantize=("MatMul",))
            quantizer = MatMulNBitsQuantizer(str(float_path), algo_config=config)
            quantizer.process()
            quantizer.model.save_model_to_file(str(quantized_path), use_external_data_format=False)
        else:
            quantize_dynamic(str(float_path), str(quantized_path), per_channel=True,
                             weight_type=QuantType.QInt8, op_types_to_quantize=["MatMul", "Gemm"])
    onnx.checker.check_model(str(quantized_path))
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    options.inter_op_num_threads = 1
    options.execution_mode = ort.ExecutionMode.ORT_SEQUENTIAL
    data = np.zeros((1, 3, 224, 224), dtype=np.float32)
    reports = []
    for path in ([quantized_path] if args.weight_only else [float_path, quantized_path]):
        session = ort.InferenceSession(str(path), options, providers=["CPUExecutionProvider"])
        times = []
        for index in range(25):
            start = time.perf_counter()
            result = session.run(None, {"pixels": data})[0]
            if index >= 5:
                times.append((time.perf_counter() - start) * 1000)
        assert result.shape == (1, 10) and np.isfinite(result).all()
        reports.append(dict(file=path.name, bytes=path.stat().st_size,
                            sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                            input_shape=[1, 3, 224, 224], output_shape=[1, 10],
                            host_cpu_median_ms=statistics.median(times),
                            black_input_logits=result.tolist()))
        print(reports[-1], flush=True)
    report = dict(source=str(args.source), parameters=sum(p.numel() for p in model.parameters()),
                  labels=model.config.id2label, training_performed=False,
                  quantization="int4 weight-only, block 32, float activations" if args.weight_only else
                  "dynamic int8 per-channel MatMul/Gemm weights; float input/output",
                  exports=reports)
    report_name = "export-int4-report.json" if args.weight_only else "export-report.json"
    (args.output / report_name).write_text(json.dumps(report, indent=2) + "\n")


if __name__ == "__main__":
    main()
