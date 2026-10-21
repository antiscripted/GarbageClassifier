# Preserved candidate weights

`material_classifier.onnx` is the actual SigLIP2 int4 export, 56,733,021 bytes, SHA-256 `fec39a575bd1a2d2434a1f763989bba0c2ecdd3c2cbfd52a27d9f923f53abd10`. It has been moved out of application assets so an unused model does not inflate the stable APK. The source and export attribution are in NOTICE.txt; Apache-2.0 is included in `../../app/src/main/assets/licenses/ANDROIDX-LITERT-APACHE-2.0.txt`.

This candidate is not connected to live camera inference. Complete-pipeline results and the deployment decision are in `../../docs/experiments/2026-10-21/`. DETR's original and float-exported weights exceed GitHub's single-file limit; the pinned source checksum, export script, parity check and export checksum are retained there. No training was performed.
