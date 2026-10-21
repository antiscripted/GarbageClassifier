import argparse
import json
from pathlib import Path

from benchmark_candidates import intersection_over_union
from summarize_accuracy import detection_metrics, image_metrics


def read_rows(paths):
    rows = [row for path in paths for row in json.loads(path.read_text())['results']]
    assert len({row['id'] for row in rows}) == len(rows)
    return rows


def metrics(rows, field):
    scenes = [row for row in rows if row['dataset'] == 'taco']
    images = [row for row in rows if row['dataset'] == 'realwaste']
    return {str(threshold): dict(taco=detection_metrics(scenes, threshold, field),
                                realwaste=image_metrics(images, threshold, field))
            for threshold in [0.6, 0.8]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--proposals', type=Path, nargs='+', required=True)
    parser.add_argument('--classified', type=Path, nargs='+', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    proposals = read_rows(args.proposals)
    classified = read_rows(args.classified)
    selected = {row['id'] for row in classified}
    baseline = [row for row in read_rows([args.baseline]) if row['id'] in selected]
    assert {row['id'] for row in baseline} == selected == {row['id'] for row in proposals}
    coverage = {}
    for name, rows in [('original', baseline), ('detr', proposals)]:
        scenes = [row for row in rows if row['dataset'] == 'taco']
        coverage[name] = {
            str(iou): sum(any(intersection_over_union(target['box'], p['box']) >= iou
                             for p in row['predictions'][:5])
                          for row in scenes for target in row['targets'])
            for iou in [0.25, 0.5]}
    strict = {}
    for proposal_threshold in [0.6, 0.8, 0.9]:
        rows = [dict(row, hybrid=[p for p in row['hybrid'] if p['proposal_score'] >= proposal_threshold])
                for row in classified]
        strict[str(proposal_threshold)] = metrics(rows, 'hybrid')['0.8']
    report = dict(method='Identical EXIF-corrected images; greedy class-agnostic IoU >= 0.5, not mAP. '
                         'Still frames omit temporal confirmation. RealWaste has image labels, not box annotations.',
                  proposal_coverage=coverage, original=metrics(baseline, 'predictions'),
                  detr_int4=metrics(classified, 'hybrid'), stricter_proposal_diagnostic=strict)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    for name in ['original', 'detr_int4']:
        for threshold, result in report[name].items():
            for dataset, data in result.items():
                print(name, threshold, dataset, dict(data['counts']))


if __name__ == '__main__':
    main()
