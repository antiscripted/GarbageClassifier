import json
from pathlib import Path

from benchmark_candidates import ROOT, intersection_over_union
from evaluate_center_scanner import overlap
from summarize_accuracy import image_metrics


def gate(predictions, box):
    area = (box[2] - box[0]) * (box[3] - box[1])
    significant = sorted((p for p in predictions if p['score'] >= 0.25 and
                          overlap(p['box'], box) / area >= 0.15),
                         key=lambda p: p['score'], reverse=True)
    kept = []
    for candidate in significant:
        if all(intersection_over_union(candidate['box'], p['box']) <= 0.45 for p in kept):
            kept.append(candidate)
    if len(kept) != 1:
        return False
    target = kept[0]['box']
    x, y = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
    return target[0] <= x <= target[2] and target[1] <= y <= target[3] and overlap(target, box) / area >= 0.25


def main():
    directory = ROOT / 'docs/experiments/center-scanner'
    rows = [json.loads(line) for line in (directory / 'center-results.jsonl').read_text().splitlines()]
    baseline = json.loads((ROOT / 'docs/experiments/2026-10-20/corrected/baseline.json').read_text())['results']
    by_id = {row['id']: row for row in baseline}
    gated = [dict(row, predictions=[row['center']] if gate(by_id[row['id']]['predictions'],
                  row['center_box']) else []) for row in rows]
    report = {'method': 'Cached still images, not live video. Center classifier threshold 0.80, margin 0.15; '
                        'original baseline 0.60, margin 0.15. No temporal lock simulated. '
                        'Cardboard maps to Paper; Biological maps to Food waste for benchmark comparison.',
              'original_realwaste': image_metrics([r for r in baseline if r['dataset'] == 'realwaste'], 0.6),
              'center_gated_realwaste': image_metrics([r for r in gated if r['dataset'] == 'realwaste'], 0.8),
              'center_gated_taco': image_metrics([r for r in gated if r['dataset'] == 'taco'], 0.8),
              'taco_policy_groups': {policy: image_metrics([r for r in gated if r['dataset'] == 'taco' and
                  r['target_policy'] == policy], 0.8)['counts']
                  for policy in sorted({r['target_policy'] for r in gated if r['dataset'] == 'taco'})}}
    (directory / 'center-summary.json').write_text(json.dumps(report, indent=2) + '\n')
    for name, result in report.items():
        if isinstance(result, dict) and 'counts' in result:
            print(name, dict(result['counts']))


if __name__ == '__main__':
    main()
