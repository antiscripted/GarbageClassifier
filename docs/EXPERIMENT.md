# Material recognition experiment — 20 October 2026

Starting point: `finish/review-build-2026-10-20`, commit `5021e619e63750cf603975dc0f9057061e22c54a`.

The owner reports physical Galaxy M51 testing: the camera and app work, but uncertain results are frequent, glass and plastic bottles are confused, Plastic is overpredicted, and food recognition depends on the viewing angle. These are owner-reported observations, not cloud-agent measurements.

This experiment preserves the existing Android application, model, approved wording, offline operation and delivery APK while investigating materially better pretrained models. No training is authorized. No replacement or threshold reduction will be made merely to increase the number of detections.

## Evaluation plan

Reuse the recorded TrashNet probes and existing model-contract tests. Audit Android preprocessing against the model export. Research actual downloadable weights, label definitions, licenses and deployment costs before benchmarking promising candidates. Add food, realistic backgrounds, unsupported objects, multiple objects and annotated boxes where data is available. Record dataset provenance, sampling, possible training overlap and the difference between material classification and object localization.

Compare correct, wrong, rejected and confidently wrong material predictions and wet/dry errors, including class-specific outcomes. Record model size, measured host CPU timing and memory separately from unmeasured phone performance. A candidate must demonstrate a convincing improvement before Android integration. If none does, retain the current model and explain the evidence and training trade-off.

Retain API 24, ARM64, ARMv7 and x86_64 CPU support where feasible. Verify native-library packaging and 16 KB alignment. Use available device configurations without repeatedly rebuilding the environment or restarting unreliable emulators.

## Status

Experiment branch created from the clean review build. Research and evaluation are pending. The existing working APK remains in `delivery/`.
