import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageOps

from benchmark_candidates import OnnxClassifier


def square(image, fraction):
    side = round(min(image.size) * fraction)
    left, top = (image.width - side) // 2, (image.height - side) // 2
    return (left, top, left + side, top + side)


def overlap(a, b):
    return max(0, min(a[2], b[2]) - max(a[0], b[0])) * max(0, min(a[3], b[3]) - max(a[1], b[1]))


def expected_center(row, box):
    if row['dataset'] == 'realwaste':
        return row['expected'], 'image_label'
    area = (box[2] - box[0]) * (box[3] - box[1])
    targets = sorted([(overlap(box, t['box']) / area, t) for t in row['targets']],
                     key=lambda item: item[0], reverse=True)
    significant = [item for item in targets if item[0] >= 0.05]
    if not significant:
        return None, 'no_significant_annotated_object'
    best, target = significant[0]
    x, y = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
    contains_center = target['box'][0] <= x <= target['box'][2] and target['box'][1] <= y <= target['box'][3]
    if best < 0.20 or not contains_center or len(significant) > 1:
        return None, 'small_offcenter_or_multiple'
    return target['expected'], 'single_dominant_center_object'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--data', type=Path, required=True)
    parser.add_argument('--model', type=Path, required=True)
    parser.add_argument('--metadata', type=Path, required=True)
    parser.add_argument('--cached', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    rows = [row for row in json.loads((args.data / 'manifest.json').read_text())
            if row['dataset'] in ['realwaste', 'taco']]
    old = json.loads(args.cached.read_text())
    assert hashlib.sha256(args.model.read_bytes()).hexdigest() == old['model_sha256']
    cached = {row['id']: row for row in old['results']}
    engine = OnnxClassifier(args.model, args.metadata)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    saved = {row['id']: row for row in map(json.loads, args.output.read_text().splitlines())} if args.output.exists() else {}
    for number, row in enumerate(rows, 1):
        if row['id'] in saved:
            continue
        with Image.open(args.data / row['path']) as file:
            image = ImageOps.exif_transpose(file).convert('RGB')
        center = square(image, 0.8)
        context = square(image, 1.0)
        prediction, elapsed = engine.predict(image.crop(center))
        if context == (0, 0, image.width, image.height) and cached[row['id']].get('predictions'):
            outer = cached[row['id']]['predictions'][0]
            outer_elapsed = None
        else:
            result, outer_elapsed = engine.predict(image.crop(context))
            outer = result[0]
        expected, policy = expected_center(row, center)
        record = dict(id=row['id'], dataset=row['dataset'], category=row['category'],
                      expected=expected, target_policy=policy, center_box=center,
                      context_box=context, center=prediction[0], context=outer,
                      center_ms=elapsed, context_ms=outer_elapsed,
                      context_reused=outer_elapsed is None)
        with args.output.open('a') as output:
            output.write(json.dumps(record) + '\n')
        if number % 40 == 0:
            print(f'{number}/{len(rows)}', flush=True)


if __name__ == '__main__':
    main()
