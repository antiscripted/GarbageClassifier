# Earlier material recognition comparison — 20–21 October 2026

**Latest results and delivery decision:** [DETR + int4 SigLIP2 complete-pipeline evaluation](experiments/2026-10-21/README.md). The original model is retained; improved-model integration remains unfinished.

Starting point: `finish/review-build-2026-10-20`, commit `5021e619e63750cf603975dc0f9057061e22c54a`.

The owner reports physical Galaxy M51 testing: the camera and app work, but uncertain results are frequent, glass and plastic bottles are confused, Plastic is overpredicted, and food recognition depends on the viewing angle. These are owner-reported observations, not cloud-agent measurements.

This experiment preserves the existing Android application, model, approved wording, offline operation and delivery APK while investigating materially better pretrained models. No training is authorized. No replacement or threshold reduction will be made merely to increase the number of detections.

## Evaluation plan

Reuse the recorded TrashNet probes and existing model-contract tests. Audit Android preprocessing against the model export. Research actual downloadable weights, label definitions, licenses and deployment costs before benchmarking promising candidates. Add food, realistic backgrounds, unsupported objects, multiple objects and annotated boxes where data is available. Record dataset provenance, sampling, possible training overlap and the difference between material classification and object localization.

Compare correct, wrong, rejected and confidently wrong material predictions and wet/dry errors, including class-specific outcomes. Record model size, measured host CPU timing and memory separately from unmeasured phone performance. A candidate must demonstrate a convincing improvement before Android integration. If none does, retain the current model and explain the evidence and training trade-off.

Retain API 24, ARM64, ARMv7 and x86_64 CPU support where feasible. Verify native-library packaging and 16 KB alignment. Use available device configurations without repeatedly rebuilding the environment or restarting unreliable emulators.

## Completed comparison and deployment decision

**Retain the original model in the experimental stability APK.** No model was retrained, replaced or added to the application, and no confidence threshold was lowered. The deployment comparison is closed for this build. SigLIP2 is a promising future accuracy upgrade, but Android CPU performance and full pipeline behavior have not been established. Continuing that integration is outside this delivery.

Corrected raw results, pinned model cards, export checks and dataset provenance are in [experiments/2026-10-20](experiments/2026-10-20/). On 240 known-material RealWaste images at the exploratory 0.60 cutoff:

| Model or pipeline | Correct | Wrong material | Rejected | Wrong wet/dry |
| --- | ---: | ---: | ---: | ---: |
| Existing detector | 86 | 80 | 74 | 5 |
| EfficientNetB0 classifier | 42 | 58 | 140 | 41 |
| MobileNetV3 Faster R-CNN detector | 77 | 39 | 124 | 4 |
| SigLIP2 float whole-image classifier | 182 | 27 | 31 | 3 |
| Existing detector + SigLIP2 float crops | 176 | 25 | 39 | 2 |
| SigLIP2 dynamic-int8 whole-image classifier | 109 | 12 | 119 | 2 |
| Existing detector + SigLIP2 dynamic-int8 crops | 121 | 22 | 97 | 3 |

Whole-image classification does not measure localization. Hybrid classification uses up to five current-detector proposals. At 0.60 the int8 hybrid correctly classified only 10/40 glass and 5/40 plastic images; its overall reduction in mistakes partly reflects more rejections. At 0.80 the float hybrid yielded 168 correct, 18 wrong and 54 rejected, but detector coverage remained poor in clutter: only 23 correct material-and-box matches among 494 TACO targets (333 with known supported materials), with 469 unmatched targets. This is class-agnostic IoU ≥ 0.5 matching, not mAP. Ground-truth classifier crops are separately labelled oracle results.

The evaluation reuses 240 TrashNet images and adds 360 RealWaste images and 104 TACO scenes. Thresholds are fixed exploratory comparisons, not calibrated probabilities; training overlap is unknown, especially for TrashNet. Unsupported/ambiguous dataset labels are not automatically negative ground truth. Dataset photos and candidate weights are not redistributed. See [evaluation caveats and attribution](experiments/2026-10-20/corrected/README.md).

## CPU and size findings

All timings below use two CPU threads on the cloud x86_64 host. They are **not Android or Galaxy M51 timings**, nor camera frame rates. Framework process memory is not comparable with Android application memory.

| Artifact | Weight size | Host median | Decision |
| --- | ---: | ---: | --- |
| Current TFLite detector | 2.84 MB | 6.66 ms | Keep existing offline integration |
| EfficientNetB0 | 28.04 MB | 24.23 ms | Reject: weak accuracy and wet/dry errors |
| MobileNetV3 Faster R-CNN | 76.09 MB checkpoint | 92.86 ms | Reject: insufficient improvement and low detection coverage |
| SigLIP2 float ONNX | 343.38 MB | 262.33 ms/crop | Defer: large, expensive, no Android CPU validation |
| SigLIP2 dynamic-int8 ONNX | 89.12 MB | 94.12 ms/crop on real images | Reject: substantial accuracy regression, particularly glass/plastic |
| SigLIP2 weight-only int4 ONNX | 56.73 MB | 246.80 ms/crop on blank input | Defer: little CPU saving; accuracy and Android execution untested |

SigLIP2 has 92,891,914 parameters. Its original PyTorch classifier measured approximately 293 ms/crop; several crops add work. The final bounded int4 export saved storage but did not establish a practical CPU improvement, so no further accuracy sweep or Android runtime integration was undertaken. Exporting and quantizing weights did not train a model.

ONNX Runtime Android 1.22.0 statically supports API 24, ARM64, ARMv7 and x86_64, with 16 KB-aligned 64-bit libraries. It would add native libraries and an additional inference stage. It was inspected but **not added to Gradle or the APK**. Static compatibility is insufficient evidence of responsive phone inference.

The existing API 29 app's recorded memory snapshot was 83,566 KB total PSS; this is a synthetic-camera emulator snapshot, not a physical-phone measurement or a candidate comparison.

## Stability delivery

The experimental APK changes tracking and display persistence only. See [STABILITY.md](STABILITY.md) for acquisition, four-second holds, smoothing, reset behavior and limitations. These changes reduce brief visual instability; they do not fix repeated confident material mistakes or improve the underlying detector's accuracy.

The original working APK remains unchanged in `delivery/`. New APKs are in `apks/experimental/`; complete source archives are in `source/`; active code stays in the standard Android `app/` folder. `backup` preserves the pre-finishing checkpoint, `development` contains this delivery, and `main` and all previous branches remain unchanged.
