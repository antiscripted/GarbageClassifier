import json
from pathlib import Path

import numpy as np
import onnx
from onnx import TensorProto, helper, numpy_helper
import onnxruntime as ort


def main():
    root = Path(__file__).resolve().parents[1]
    model = onnx.load(root / 'tools/models/material_classifier.onnx')
    tensors = {v.name: numpy_helper.to_array(v) for v in model.graph.initializer}
    layer = next(n for n in model.graph.node if n.op_type == 'MatMulNBits' and '/layers.1/self_attn/q_proj/' in n.name)
    packed = tensors[layer.input[1]][:16, :1, :].copy()
    scales = tensors[layer.input[2]].reshape(tensors[layer.input[1]].shape[0], -1)[:16, :1].copy()
    nodes = [helper.make_node('MatMulNBits', ['pixels', 'weights', 'scales'], [name],
                             domain='com.microsoft', K=32, N=16, bits=4, block_size=32,
                             accuracy_level=level) for name, level in [('original', 1), ('optimized', 4)]]
    graph = helper.make_graph(nodes, 'real_weight_kernel_check',
                              [helper.make_tensor_value_info('pixels', TensorProto.FLOAT, [1, 4, 32])],
                              [helper.make_tensor_value_info(name, TensorProto.FLOAT, [1, 4, 16])
                               for name in ['original', 'optimized']],
                              [numpy_helper.from_array(packed, 'weights'), numpy_helper.from_array(scales, 'scales')])
    fixture = helper.make_model(graph, opset_imports=[helper.make_opsetid('', 17), helper.make_opsetid('com.microsoft', 1)], ir_version=8)
    onnx.checker.check_model(fixture)
    directory = root / 'app/src/androidTest/assets/int4'
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / 'kernel.onnx'
    onnx.save(fixture, path)
    inputs = (np.arange(128, dtype=np.float32) / np.float32(63.5) - 1).reshape(1, 4, 32)
    unpacked = np.stack((packed & 15, packed >> 4), axis=-1).reshape(16, 32).astype(np.float32)
    expected = inputs @ ((unpacked - 8) * scales).T
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    outputs = ort.InferenceSession(str(path), options, providers=['CPUExecutionProvider']).run(None, {'pixels': inputs})
    errors = [float(np.max(np.abs(output - expected))) for output in outputs]
    assert errors[0] < 0.0001 and errors[1] < 0.02, errors
    report = dict(input=inputs.reshape(-1).tolist(), expected=expected.reshape(-1).tolist(),
                  shape=[1, 4, 16], original_tolerance=0.0001, optimized_tolerance=0.02,
                  host_maximum_absolute_errors=errors, source_layer=layer.name)
    (directory / 'expected.json').write_text(json.dumps(report, indent=2) + '\n')
    print(errors)


if __name__ == '__main__':
    main()
