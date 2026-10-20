# Material recognition experiment — 20 October 2026

Starting point: `finish/review-build-2026-10-20`, commit `5021e619e63750cf603975dc0f9057061e22c54a`.

The owner reports physical Galaxy M51 testing: the camera and app work, but uncertain results are frequent, glass and plastic bottles are confused, Plastic is overpredicted, and food recognition depends on the viewing angle. These are owner-reported observations, not cloud-agent measurements.

This experiment preserves the existing Android application, model, approved wording, offline operation and delivery APK while investigating materially better pretrained models. No training is authorized. No replacement or threshold reduction will be made merely to increase the number of detections.

## Evaluation plan

Reuse the recorded TrashNet probes and existing model-contract tests. Audit Android preprocessing against the model export. Research actual downloadable weights, label definitions, licenses and deployment costs before benchmarking promising candidates. Add food, realistic backgrounds, unsupported objects, multiple objects and annotated boxes where data is available. Record dataset provenance, sampling, possible training overlap and the difference between material classification and object localization.

Compare correct, wrong, rejected and confidently wrong material predictions and wet/dry errors, including class-specific outcomes. Record model size, measured host CPU timing and memory separately from unmeasured phone performance. A candidate must demonstrate a convincing improvement before Android integration. If none does, retain the current model and explain the evidence and training trade-off.

Retain API 24, ARM64, ARMv7 and x86_64 CPU support where feasible. Verify native-library packaging and 16 KB alignment. Use available device configurations without repeatedly rebuilding the environment or restarting unreliable emulators.

## Status

Experiment branch created from the clean review build. The existing working APK remains in `delivery/`.

The corrected comparison is saved in `docs/experiments/2026-10-20/`. On 240 known-material RealWaste images, at the exploratory 0.60 cutoff:

| Model | Correct | Wrong material | Rejected | Wrong wet/dry |
| --- | ---: | ---: | ---: | ---: |
| Existing detector | 86 | 80 | 74 | 5 |
| EfficientNetB0 classifier | 42 | 58 | 140 | 41 |
| MobileNetV3 Faster R-CNN detector | 77 | 39 | 124 | 4 |
| SigLIP2 whole-image classifier | 182 | 27 | 31 | 3 |
| Existing detector + SigLIP2 crops | 176 | 25 | 39 | 2 |

Whole-image and cropped-object classification do not establish localization accuracy. See the separate TACO box results, confidence thresholds, per-class counts and limitations in `summary.json` and `corrected/README.md`. The classifiers do not provide their own boxes. SigLIP2 is promising for material recognition, but its 371.6 MB float weights and approximately 293 ms host inference per crop require a deployment feasibility check. No replacement is integrated yet. EfficientNet and MobileNetV3 are rejected for this upgrade.

The next bounded step is to check whether quantized SigLIP2 inference preserves the improvement and can run with a portable Android CPU runtime. ONNX Runtime Android 1.22.0 declares minimum API 24 and packages ARM64, ARMv7 and x86_64; its 64-bit libraries have 16 KB ELF alignment. These static checks are not device tests.

The owner also requested a four-second minimum result hold, conservative physical-object association, smoothed boxes, temporary-occlusion tolerance and reset tests. This stability change is required even if a model upgrade proves impractical. Existing recognition thresholds and unsupported-material rules must not be loosened.
