# New-model experimental checkpoint

Only `checkpoint` is used for this work. Verified `development`, `main`, `backup` and all previous APKs remain unchanged at the beginning of this experiment. An unverified experimental APK is now packaged for phone testing; see [delivery results and instructions](DELIVERY.md). No new-model APK has been qualified for reliable real-time use.

## Error audit

Inspection of existing predictions found that 8 of the 13 DETR-pipeline wrong-bin RealWaste images have the correct whole-image SigLIP2 material prediction (some below the acceptance threshold). DETR often crops a printed fruit/food picture on a container, a tiny background patch, or part of a logo. Selecting the highest classifier confidence chooses these details over the held object. This is a proposal-selection and task-definition problem, not evidence of a normalized-box decoding or wet/dry lookup-table swap. The wrong-bin rows and all unmatched TACO predictions at 0.80 are saved in `detr-error-audit.json`.

The previous 512-square resize also differs from DETR's published aspect-preserving processor. That is a separate localization limitation; changing it must be measured, not assumed to solve printed-object detections. TACO measures all annotated clutter objects, whereas a center scanner must explicitly decline multiple, small or off-center objects and must not claim their precise boxes.

## Center evaluation

`tools/evaluate_center_scanner.py` evaluates an 80%-of-short-side center square and a full short-side context square using the preserved SigLIP2 int4 weights. Existing whole-image predictions are reused whenever the context is the same image. Results append incrementally and are reused on resume.

RealWaste image labels are evaluated as before. TACO center targets are defined before reading predictions: one annotation overlapping at least 20% of the center square, containing its center, with no second annotation covering 5% or more. Other scenes are marked ambiguous or without a significant annotated object and must be treated conservatively. This is a task-specific classification test, not localization accuracy or a substitute for real camera video. Thresholds and any context-agreement gate must be reported alongside counts and rejections.

## Android diagnosis

The existing probe now records runtime-loading, session-creation and inference stages to an app-private progress file. It disables worker spinning, permits one cold inference, and no longer spends time rerunning the original model. This addresses the missing diagnostic information in the previous aborted four-call test. No Android success or phone timing is assumed before numerical outputs are checked.

## Center variant implementation checkpoint

The separate `newModel` build type now uses the actual SigLIP2 int4 model for material classification and the original small detector only as a conservative center-presence gate. No original material label is used as fallback. It runs classification on a separate bounded worker, shares one classifier session across camera owners, checks current object appearance/generation before accepting asynchronous results, and requires two independent high-confidence inference results before the four-second lock. Cached camera frames cannot acquire a lock. Unsupported classes stay uncertain; new-model mode draws no object outlines. Bin inspection checks the same centered object. Verified model label “Biological” is retained rather than asserting all organic material is food.

The new variant compiles; its 85 JVM tests passed and lint has no errors. The earlier standalone Android probe returned numerically correct logits but took 23.6 minutes. A final bounded production call timed out at 60 seconds after successfully creating the bundled model session. The experimental APK is therefore explicitly unverified for live use; see DELIVERY.md. The completed center measurements and preprocessing audit are preserved here; no old-model APK was rebuilt. The published DETR processor localized 21/87 targets in the fixed 16-image diagnostic, versus 17/87 for the earlier square processor: better, but not a complete localization fix.
