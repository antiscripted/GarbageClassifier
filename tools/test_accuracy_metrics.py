import unittest

from summarize_accuracy import detection_metrics, image_metrics


def prediction(material="Glass", score=0.9, box=None):
    return dict(material=material, score=score, margin=0.2, box=box or [0, 0, 100, 100])


class AccuracyMetricsTest(unittest.TestCase):
    def test_material_error_does_not_imply_wrong_dry_bin(self):
        rows = [dict(expected="Glass", category="glass", predictions=[prediction("Plastic")])]
        counts = image_metrics(rows, 0.6)["counts"]
        self.assertEqual(1, counts["wrong_material"])
        self.assertEqual(1, counts["confident_wrong"])
        self.assertEqual(0, counts["wrong_bin"])

    def test_food_confusion_is_wrong_bin(self):
        rows = [dict(expected="Glass", category="glass", predictions=[prediction("Food waste")])]
        self.assertEqual(1, image_metrics(rows, 0.6)["counts"]["wrong_bin"])

    def test_unlabelled_material_is_not_scored_as_known_wrong(self):
        rows = [dict(expected=None, category="unspecified", predictions=[prediction()])]
        counts = image_metrics(rows, 0.6)["counts"]
        self.assertEqual(1, counts["unsupported_acceptance"])
        self.assertEqual(0, counts["confident_wrong"])
        self.assertEqual(0, counts["known_accepted"])

    def test_rejection_and_missing_detections_are_distinct(self):
        rows = [dict(expected="Glass", category="glass", predictions=[prediction(score=0.4)]),
                dict(expected="Glass", category="glass", predictions=[])]
        counts = image_metrics(rows, 0.6)["counts"]
        self.assertEqual(2, counts["known_rejected"])
        self.assertEqual(1, counts["undetected"])

    def test_wrong_label_can_still_be_localized(self):
        rows = [dict(targets=[dict(expected="Glass", category="glass", box=[0, 0, 100, 100])],
                     predictions=[prediction("Plastic")])]
        counts = detection_metrics(rows, 0.6)["counts"]
        self.assertEqual(1, counts["localized"])
        self.assertEqual(1, counts["wrong_material"])
        self.assertEqual(0, counts["correct_material_and_box"])

    def test_duplicate_box_cannot_match_same_object_twice(self):
        rows = [dict(targets=[dict(expected="Glass", category="glass", box=[0, 0, 100, 100])],
                     predictions=[prediction(), prediction()])]
        counts = detection_metrics(rows, 0.6)["counts"]
        self.assertEqual(1, counts["correct_material_and_box"])
        self.assertEqual(1, counts["unmatched_boxes"])

    def test_right_material_on_wrong_object_is_not_correct(self):
        rows = [dict(targets=[dict(expected="Glass", category="glass", box=[200, 200, 300, 300])],
                     predictions=[prediction()])]
        counts = detection_metrics(rows, 0.6)["counts"]
        self.assertEqual(0, counts["correct_material_and_box"])
        self.assertEqual(1, counts["missed_targets"])


if __name__ == "__main__":
    unittest.main()
