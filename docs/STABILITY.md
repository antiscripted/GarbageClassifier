# Stable recognition

The experimental APK keeps the original detector, thresholds, segregation rules, screens and approved wording. Stability is implemented after detection, without another model or runtime dependency.

## What happens to an object

1. `RecognitionFilter` checks confidence ≥ 0.60, class margin ≥ 0.15 and two nearby detections within 700 ms. Unsupported material and contradictory overlapping detections remain uncertain.
2. `AppearanceSampler` takes a small 4×4 RGB grid inside each box. `RecognitionTracker` associates observations using position, area and appearance. Three confirmed observations spanning at least 400 ms are required to lock. With regular 200 ms frames, the combined pipeline needs four sightings over 600 ms; slower or unstable frames take longer.
3. A locked result survives brief confidence drops and one contradictory frame. Box coordinates move 35% of the remaining distance toward each new detection, reducing jitter with some motion lag. The result card retains the primary tracked object instead of switching whenever confidence rankings change.
4. After an object disappears, its box disappears immediately. Its card can remain until both the four-second minimum since confirmation and 1.25 seconds since last sighting have elapsed. Thus removal shortly after confirmation may leave the card until the four-second hold finishes. Continuous camera frames are required to update this timer.
5. A new visible object takes priority over a missing object's held card. Changed appearance, ambiguous crossings, three confident contradictory frames spanning 400 ms, mode/bin changes, camera restart, rotation and leaving the app invalidate stale identity or results. These safety resets can end the four-second hold early. A frame-stream stall also clears results rather than presenting a frozen classification.

## Limits

The RGB grid and box overlap are inexpensive identity heuristics, not proof that two sightings are the same physical object. Similar objects, lighting changes, rotation, occlusion and fast motion can break association. An identical-looking replacement may be mistaken for the same object. Conservative resets may increase the time spent showing “Object not clear”. Smoothing can lag fast motion. A consistently wrong model can still produce a stable wrong result.

No physical M51 tracking performance has been measured by Codex. The owner previously tested the original APK and reported working camera/app behavior but unreliable material recognition. The experimental build needs the same-object comparison below before competition acceptance.

## Phone review

Compare original and experimental APKs using the same safe objects, distances, lighting and backgrounds. They share one package name, so installing one updates the other; they do not install side by side. Keep both APK files.

- Present food, glass, plastic, metal and paper individually; record correct, wrong and uncertain outcomes, and time to confirmation.
- Briefly move/occlude a confirmed object, then remove it. Verify the box disappears and the card resets after the applicable hold/removal interval.
- Replace an object with a different one in the same place. Cross two objects. Confirm no old bin recommendation transfers to a clearly different visible object.
- Compare Wet/Dry inspection, uncertain items and multiple objects. Toggle bin/mode, rotate, background/resume and open/close Licenses; previous holds must clear.
- Run in airplane mode for ten minutes and record preview smoothness, delay, temperature and errors. Desktop timing is not a phone FPS estimate.

The 22 deterministic tracker tests exercise timing boundaries, confirmation, brief uncertainty, occlusion, contradictions, appearance/position replacement, smoothing, independent bins, crossing ambiguity, resets and frame stalls. They validate logic, not real-camera recognition accuracy.
