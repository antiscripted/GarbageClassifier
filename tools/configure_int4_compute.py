import argparse
import hashlib
from pathlib import Path

import onnx


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    assert hashlib.sha256(args.source.read_bytes()).hexdigest() == 'fec39a575bd1a2d2434a1f763989bba0c2ecdd3c2cbfd52a27d9f923f53abd10'
    assert not args.output.exists(), 'Refusing to overwrite a model'
    model = onnx.load(args.source)
    modified = 0
    for node in model.graph.node:
        if node.op_type == 'MatMulNBits' and '/layers.0/' not in node.name and '/layers.11/' not in node.name:
            next(a for a in node.attribute if a.name == 'accuracy_level').i = 4
            modified += 1
    assert modified == 60
    onnx.checker.check_model(model)
    onnx.save(model, args.output)
    assert hashlib.sha256(args.output.read_bytes()).hexdigest() == '63802ff39c00c52663846e4bf7c0dfe511e4e4242398a24240bce733cdbdf366'


if __name__ == '__main__':
    main()
