# Model integration checkpoint — 21 October 2026

This checkpoint is unfinished work. Verified `development` remains at `f9e0b5842e816d59b9d34fe5c7623092c26beaf7`; its stability APK is unchanged. `main` and `backup` are untouched. No new-model APK has been verified yet.

The actual 56,733,021-byte SigLIP2 weight-only int4 export is saved as `app/src/main/assets/material_classifier.onnx` to preserve progress. SHA-256: `fec39a575bd1a2d2434a1f763989bba0c2ecdd3c2cbfd52a27d9f923f53abd10`. It is **not yet connected to camera inference**. It derives without training from `prithivMLmods/Augmented-Waste-Classifier-SigLIP2`, revision `d203b52311f70ac3245d5fb8345d88344bec138e`, Apache-2.0. The model card, original tensor contract and export details are in the previous experiment folder.

New results: int4 whole-image classification gives 184 correct, 26 wrong and 30 rejected among 240 known-material RealWaste images at 0.60. The original detector gives 86/80/74, but detector and whole-image classifier scores are not equivalent end-to-end tests. Int4 using original detector crops gives 175/25/40. Its host median is 243 ms per crop, not phone timing.

On 104 TACO scenes with 494 annotated targets, maximum-five proposals cover the following targets at IoU ≥ 0.5: original 34; YOLOv8m 2; EfficientDet-Lite0 27; tiled original 76; tiled + EfficientDet 80; DETR 123. These are proposal coverage counts, not material accuracy, one-to-one detection precision, mAP or validated field accuracy. All scenes and EXIF handling reuse the existing pinned dataset manifest. Training overlap remains unknown.

Tiled + EfficientDet + int4 yields 53 correct material-and-box matches at 0.80, but 93 unmatched accepted boxes and 431 missed targets. This does not establish reliable localization. That provisional report tiles TACO only; RealWaste uses the original plus EfficientDet proposals, so it must not be presented as one uniformly implemented application pipeline.

The already-researched `Yorai/detr-resnet-50_finetuned_detect-waste` is the next candidate. Its 166,528,661-byte weights were downloaded once and verified against pinned SHA-256 `f5e9cb4ec7bdc3274417ce8c5bcfb8787fad6c4eefb5321add70e8966c078065`. They remain outside Git because the file exceeds GitHub's per-file limit; pinned metadata is retained here. The proposal test used a documented fixed 512×512 input, different from upstream's 800/1333 aspect-preserving processor. Next: classify those predicted boxes, evaluate complete-pipeline results, and establish Android runtime/CPU/memory feasibility before integration.

A source audit found that UI result expiry did not invalidate tracker state. The local fix now resets tracking at expiry, with a new Android regression test. The test has not yet run. Existing 75 JVM and 8 Android passing results apply to the prior delivery, not this modified source.

YOLOv8m's initial proposal run used eight threads after Ultralytics reset the configured two; its report records this and concurrent CPU activity. The harness now restores two after predictor initialization. No duplicate accuracy run was performed merely to replace that timing. Candidate metrics and raw files are retained; no new training or broad model search was performed.
