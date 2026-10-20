# Model verification and accuracy limits

## Bundled artifact

The app uses the requested `alexNova/YOLO-TrashDet-Pruned` 320×320 TFLite export. No model was trained or paid API integrated.

- Hugging Face revision: `fbb540cd6b415c08387f4c305d153a3c358ea78b`
- File: `TFLite Format/YOLOv8n-TrashDet-10-imgsz320-int8.tflite`
- Bytes: **2,836,973**
- SHA-256: **9c258ac8a0344cd07deb28ddcedb096e5bd30c092b807ff7e442f40279d8f54d**
- License declared by model card and export metadata: **AGPL-3.0**
- Obtained from the author's byte-identical GitHub mirror at commit `14a626f9654603237c86a8064805f50c4a659921` after the Hugging Face CDN was blocked. The downloaded bytes were verified against the Hugging Face LFS manifest; this is not a substitute model.

Machine-readable details are in [model/contract.json](model/contract.json), with the original card and export metadata beside it. The Android runtime validates the asset checksum, tensor counts, shapes and data types before inference.

| Tensor | Verified contract |
| --- | --- |
| Input `inputs_0` | float32 `[1,320,320,3]`, RGB, normalized to 0…1, NHWC |
| Output `Identity` | float32 `[1,14,2100]`, channels first |
| Channels 0–3 | Normalized center-x, center-y, width, height |
| Channels 4–13 | Ten already activated class probabilities |
| Objectness / sigmoid / embedded NMS | No separate objectness, no extra sigmoid required, no embedded NMS |
| Quantization | 65 per-channel int8 weight tensors; float32 input/output and activations. Input/output quantization scale 0 and zero-point 0. The filename does **not** imply integer I/O. |

Verified label order: Not Recyclable, Food waste, Glass, Textile, Metal, Wooden, Leather, Plastic, Ceramic, Paper. Unsupported labels remain uncertain for disposal purposes. RGB is not changed to BGR.

The app uses uniform letterboxing with RGB padding 114 and reverses that geometry when decoding. Candidate scores below 0.25 are dropped; class-aware NMS uses IoU 0.45 and keeps at most 20 boxes. Final recommendations require confidence 0.60, best-versus-second class margin 0.15 and two nearby detections within 700 ms. Contradictory overlapping classes are rejected. These checks reduce instability; they cannot eliminate confident model errors.

## Actual inference checks

Desktop TensorFlow 2.17.1 loaded the real file. The tensor inspection found 343 float32, 65 int8 and 17 int32 tensors. A black input's maximum class score was approximately 0.00335. The recorded warm host median was about 6.42 ms with two CPU threads (the final recheck measured 6.08 ms); this is neither an M51 benchmark nor an end-to-end camera frame rate.

The Android tests execute LiteRT 1.4.1 against three real TrashNet samples: plastic1, metal1 and paper1. They also test an uncertain glass example, an empty image, sensor rotations and CameraX mappings. The metal sample's decoded box is checked near `[0,0,308,284]` in its 512×384 source image. These are regression fixtures, not an accuracy dataset.

Run the independent host verification with Python 3.11 or 3.12:

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r tools\model-requirements.txt
.\.venv\Scripts\python.exe tools\verify_model.py --report model-report.json
```

On Linux, use `.venv/bin/python`. Python is optional development tooling and is not used by the Android app.

## Material probes and alternatives

The probes use TrashNet's first ten images in each of six folders, followed by a separate check of images 11–50 in each folder. The five known-material folders are glass, plastic, metal, paper and cardboard. Cardboard is compared with Paper for the requested model. The `trash` folder is excluded from material-correctness counts because it has no precise material ground truth. There are no wet-food examples in these probes.

Each image contributes its highest-scoring candidate. These are single-image material probes, not detection mAP: the dataset has no box ground truth, temporal confirmation is not exercised, and overlap with upstream training data is unknown. The samples are ordered, not randomly sampled. Do not quote these figures as validated field accuracy.

| Export and threshold | First 50 known-material images: accepted / correct | Next 200 known-material images: accepted / correct |
| --- | --- | --- |
| Bundled 320 hybrid-int8, 0.60 + margin 0.15 | 22 / 17 | 125 / 82 |
| Bundled 320 hybrid-int8, 0.80 + margin 0.15 | 20 / 15 | 88 / 63 |
| Author's 480 hybrid-int8, 0.60 + margin 0.15 | 36 / 27 | Not run |
| Author's 320 float32 `best_20`, 0.60 + margin 0.15 | 36 / 27 | 130 / 98 |
| Author's 320 float32 `best_20`, 0.80 + margin 0.15 | 20 / 19 | 90 / 68 |

The float32 export is 9,599,453 bytes and improves some results, but its apparently strong first small sample does not generalize to the next images. At threshold 0.60 the bundled model incorrectly recommends wet for five of the next 200 dry examples; float32 does so for three. At 0.80 those counts are three and one. Neither export establishes the reliability needed for unattended sorting. Increasing confidence alone does not solve this.

A separate pretrained alternative, [HrutikAdsare/waste-detection-yolov8](https://huggingface.co/HrutikAdsare/waste-detection-yolov8), was downloaded and actually run with Ultralytics 8.3.221 and CPU PyTorch 2.8.0. Revision `ebc11ab3d6bc146f2894a022bf2abfb331c0734d`; checkpoint SHA-256 `13237db8cb205d0911007c750139c83f81027e471a2209d887a10e41c3583b6b`. It is YOLOv8m, 25,860,952 parameters, 51,992,715 checkpoint bytes, with eight labels. Its card declares MIT, but that declaration alone does not remove underlying Ultralytics license considerations.

At score 0.60, on the first 50 known-material images it accepted 14 and correctly named 12 at 320; at its training size 512 it accepted 29 and correctly named 17. Warm host medians were about 131 ms and 280 ms respectively. This uses that model's normal postprocessing without the current app's class-margin filter, so it is not an identical end-to-end comparison. No TFLite version was supplied by that repository, and it was not integrated. Its medical/e-waste labels were not enabled in this app.

Additional cards inspected: `turhancan97/yolov5-detect-trash-classification` (PyTorch YOLOv5s, card says MIT), `keremberke/yolov5n-garbage` (no clear card license; generic garbage detection), and `sharktide/waste-detection` (no clear card license). These were not run and are not represented as tested alternatives.

**Decision:** retain the requested, verified 2.84 MB 320 export for the lightweight educational review build. The alternatives investigated have not established a sufficiently reliable replacement. The application is operational, but general recognition reliability remains unresolved; it must be demonstrated with honest successes and failures and reviewed on real local waste before deployment.

Raw per-image results are in `docs/validation/*results.json`. The separate broad probe is called `240` because it includes 40 unspecified-trash samples as well as 200 known-material samples. It is a follow-up probe, not a guaranteed unseen test set. No training or threshold fitting was performed on these images.

To reproduce the first material probe, obtain TrashNet's `data/dataset-resized.zip` from https://github.com/garythung/trashnet (archive SHA-256 `0bf472790f8b20e5c950d5b5012a9d38af0d3392efd65f8ce171334fc16b07c2`) and run:

```bash
python tools/probe_materials.py --archive /path/to/dataset-resized.zip --report probe.json
python tools/probe_materials.py --archive /path/to/dataset-resized.zip --first 11 --count 40 --report probe-240.json
```

Use the model verification virtual environment. `--model /path/to/variant.tflite` supports the same ten-class float-I/O contract at another square input size. The committed script was executed and exactly reproduced the first 60 stored per-image results.
