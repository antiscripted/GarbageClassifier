# Model comparison — completed evaluation, integration not qualified

The stable APK retains **alexNova/YOLO-TrashDet-Pruned**, the original 320×320 TFLite detector. **No new-model APK is being claimed.** The strongest tested localization candidate, DETR + SigLIP2 int4, still gives too many misplaced boxes and wrong bin recommendations to replace it responsibly. No training was performed.

## Equivalent pipeline evaluation

The final comparison uses the same 360 RealWaste images and 104 EXIF-corrected TACO scenes for both pipelines. TACO has 494 annotated objects, including 86 multi-object scenes. RealWaste contributes 240 known-material and 120 unsupported/ambiguous images. All images and previous baseline predictions were reused; only new proposal crops were classified.

DETR uses a fixed 512×512 RGB bilinear square resize and ImageNet normalization, ten queries, proposal confidence ≥0.25, class-agnostic NMS at 0.45 and at most five proposals. SigLIP2 classifies those predicted crops at 224×224, RGB bilinear resize, normalization to −1…1, softmax and margin ≥0.15. Supported labels map to the existing Food waste/Glass/Metal/Paper/Plastic categories. No detector material labels substitute for classifier results.

The original pipeline uses its verified letterbox/decoder and the same material confidence/margin acceptance rules. These are equivalent **still-image pipeline** comparisons, not a simulation of temporal confirmation or proof of live phone accuracy. RealWaste has image labels, not box annotations; its material result uses the highest-confidence predicted crop. TACO uses one-to-one, class-agnostic greedy IoU ≥0.5 matching, not mAP. Unknown training-set overlap remains a limitation.

| Metric at material confidence 0.60 | Original | DETR + int4 SigLIP2 |
| --- | ---: | ---: |
| RealWaste known material: correct / wrong / rejected, out of 240 | 86 / 80 / 74 | 184 / 33 / 23 |
| Wrong wet/dry recommendation among known images | 5 | 13 |
| Accepted on 120 unsupported/ambiguous images | 39 | 63 |
| TACO accepted boxes | 44 | 264 |
| TACO localized boxes, regardless of material | 22 | 89 |
| TACO correct material **and** box | 11 | 66 |
| TACO unmatched accepted boxes | 22 | 175 |
| TACO missed targets | 472 | 405 |

At classifier confidence 0.80, DETR + SigLIP2 gives 172/23/45 on known RealWaste images, but still 11 wrong bins; on TACO it gives 62 correct material-and-box matches, 143 unmatched boxes and 415 misses. Raising proposal confidence to 0.80 as well yields 57 correct matches, 79 unmatched boxes and 425 misses. These stricter thresholds were diagnostic checks on the same data, not independent tuning validation.

At 0.60, correct localized TACO glass bottles improve from 0/27 to 3/27, clear plastic bottles from 0/23 to 5/23, other plastic bottles from 3/15 to 8/15, and food waste from 0/8 to 2/8. This remains poor coverage of the requested classroom objects. RealWaste glass improves from 10/40 to 31/40 and food from 20/40 to 39/40, while plastic improves only from 15/40 to 20/40. Whole-image classification results must not be described as detection accuracy.

## Localization candidates completed

At most five proposals, IoU ≥0.5 coverage of 494 TACO targets: original 34; YOLOv8m 2; EfficientDet-Lite0 COCO 27; tiled original 76; tiled + COCO 80; DETR 123. These are any-proposal coverage counts, not one-to-one precision. DETR is best among these candidates but covers only 24.9% of targets. Its upstream processor uses an 800/1333 aspect-preserving resize; the 512-square deployment probe is a documented change, not an assertion that this measures upstream maximum accuracy.

The earlier combined-proposal experiment used tiling for TACO only, and original + COCO for RealWaste. It is retained as exploratory evidence, not presented as one uniform application pipeline. YOLOv8m initially reset its CPU thread count to eight; that timing is marked accordingly. Other host timings include some shared CPU activity and are not controlled phone comparisons.

## Smaller two-stage fallback

The original detector + SigLIP2 int4 was also evaluated on the same images. At 0.60 it gives **175 correct / 25 wrong / 40 rejected** known-material RealWaste images, with **2 wrong bins**, and **27 correct material-and-box matches, 20 unmatched boxes and 463 misses** on TACO. This is a more promising small fallback than DETR for bin-error rate, but its original proposals still cover only 34/494 targets. It needs roughly 59.6 MB of weights before runtime. The SigLIP2 Android probe below applies to this fallback too: its latency and output parity were not verified, and simply putting it in the current synchronous analyzer would risk exceeding the 700/900 ms confirmation/freshness limits. It is preserved as a candidate for a measured ARM test, not silently integrated or advertised as working.

## Models, licensing and conversion

- SigLIP2: `prithivMLmods/Augmented-Waste-Classifier-SigLIP2`, revision `d203b52311f70ac3245d5fb8345d88344bec138e`, Apache-2.0. The actual 56,733,021-byte int4 export is preserved in `tools/models/material_classifier.onnx`, SHA-256 `fec39a575bd1a2d2434a1f763989bba0c2ecdd3c2cbfd52a27d9f923f53abd10`. Float activations, MatMulNBits block size 32, symmetric four-bit weights; float input `[1,3,224,224]`, ten logits. Full precision is ~343 MB; the ~89 MB dynamic-int8 variant regressed badly and was rejected.
- DETR: `Yorai/detr-resnet-50_finetuned_detect-waste`, revision `12c9760517571e60d5372d013021a509f555e6cc`, declared Apache-2.0. Original weights: 166,528,661 bytes, SHA-256 `f5e9cb4ec7bdc3274417ce8c5bcfb8787fad6c4eefb5321add70e8966c078065`. Float ONNX export: 167,539,174 bytes, SHA-256 `c09a172f3f8579ba7549dcb2fa1b997d5eaf0b9aa96f82eab9159017f7d9d08b`; input `[1,3,512,512]`, outputs `[1,10,8]` logits including no-object and `[1,10,4]` normalized cxcywh boxes. Export parity max absolute errors were 0.00000525 and 0.00000048. Fixed-shape export warnings are retained. Large DETR files remain in the existing experiment workspace; pinned metadata and the reproducible export script are committed.
- The two exported weight files total 224,272,195 bytes, before runtime, temporary buffers and activations. Model size alone is not a RAM estimate.
- ONNX Runtime Android 1.22.0, MIT with bundled third-party notices, is preserved as a test-only AAR in `tools/android-runtime/`. It is excluded from the scanner APK. ARM64, ARMv7 and x86_64 libraries exist; ARM execution has not been tested here.

RealWaste attribution, TACO image-rights limitations and earlier dataset/model provenance remain in the 20 October experiment folder. Evaluation photos are not redistributed in the application. SigLIP2 attribution is in `tools/models/NOTICE.txt`; the stable app's existing AGPL-3.0 and Apache notices remain unchanged.

## Evidence and remaining work

`pipeline-summary.json` contains full per-class metrics; its corresponding raw reports preserve every prediction. `tools/summarize_pipeline.py` reproduces the comparison without running inference again. `detr-export-report.json` records tensor parity. Android probe results are recorded separately in this folder; software-emulator timing cannot establish Snapdragon 730G latency.

Integration remains unfinished because localization is unreliable and the full pipeline worsens wet/dry errors. A compatible runtime does not resolve those accuracy failures. The existing camera confirmation expects observations within 700 ms and discards results older than 900 ms; a slow two-stage model also needs measured ARM performance and safe scheduling before integration. Android crop-resize parity, combined-model peak memory, real-camera tracking and sustained thermal behavior remain unverified. No model training, threshold relaxation in the app or silent fallback was introduced.

## Android probe outcome

The SigLIP2 int4 CPU instrumentation probe was deliberately stopped after **531 host seconds**, before its four-call run completed. Its final instrumentation “Process crashed” message results from that force-stop, not an observed spontaneous runtime failure. No completed latency or output-parity result was obtained. A process-memory snapshot before stopping reported 166,058 KiB PSS (about 162 MiB); this is neither peak RAM nor a two-model requirement. The DETR Android probe was not run after the complete pipeline failed the accuracy gate.

This establishes an environment limit, not that the model is impossible to run on a Galaxy M51. Android CPU performance, numerical outputs and combined-model memory remain **unverified**. Host ONNX parity succeeded; an ARM phone test would still be required before any deployment. No extra emulator setup or restart loop was attempted.
