import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

import numpy as np
import tensorflow as tf
from PIL import Image

from verify_model import ROOT, prepare_image


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', type=Path, default=ROOT / 'app/src/main/assets/trash_detector.tflite')
    parser.add_argument('--archive', type=Path, required=True)
    parser.add_argument('--first', type=int, default=1)
    parser.add_argument('--count', type=int, default=10)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    assert args.first > 0 and args.count > 0
    labels = json.loads((ROOT / 'docs/model/contract.json').read_text())['labels']
    engine = tf.lite.Interpreter(model_path=str(args.model), num_threads=2)
    engine.allocate_tensors()
    inputs = engine.get_input_details()[0]
    outputs = engine.get_output_details()[0]
    size = int(inputs['shape'][1])
    assert inputs['dtype'] == outputs['dtype'] == np.float32
    assert int(outputs['shape'][1]) == len(labels) + 4
    results = []
    with ZipFile(args.archive) as archive:
        for label in ['glass', 'plastic', 'metal', 'paper', 'cardboard', 'trash']:
            for index in range(args.first, args.first + args.count):
                member = f'dataset-resized/{label}/{label}{index}.jpg'
                with archive.open(member) as file:
                    image = Image.open(file).convert('RGB')
                engine.set_tensor(inputs['index'], prepare_image(image, size))
                engine.invoke()
                scores = engine.get_tensor(outputs['index'])[0, 4:]
                class_id, candidate = np.unravel_index(np.argmax(scores), scores.shape)
                confidence = float(scores[class_id, candidate])
                expected = 'Paper' if label == 'cardboard' else label.capitalize()
                if label == 'trash':
                    expected = 'Unspecified trash'
                results.append(dict(image=member, expected=expected, predicted=labels[class_id],
                                    confidence=confidence, margin=confidence - float(np.sort(scores[:, candidate])[-2])))
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(results, indent=2) + '\n')
    print('Model SHA256:', hashlib.sha256(args.model.read_bytes()).hexdigest())
    print('Archive SHA256:', hashlib.sha256(args.archive.read_bytes()).hexdigest())
    for threshold in [0.6, 0.7, 0.8]:
        accepted = [row for row in results if row['expected'] != 'Unspecified trash'
                    and row['confidence'] >= threshold and row['margin'] >= 0.15
                    and row['predicted'] in ['Food waste', 'Glass', 'Metal', 'Plastic', 'Paper']]
        correct = sum(row['predicted'] == row['expected'] for row in accepted)
        print(f'Threshold {threshold}: {len(accepted)} accepted, {correct} matching material; not detection mAP')


if __name__ == '__main__':
    main()
