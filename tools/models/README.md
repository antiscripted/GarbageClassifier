# Bundled SigLIP2 center classifier

`material_classifier.onnx` is the real SigLIP2 int4 model used by the `newModel` Android build. Size: 56,733,021 bytes. SHA-256: `63802ff39c00c52663846e4bf7c0dfe511e4e4242398a24240bce733cdbdf366`.

The 22 October CPU optimization changes 60 MatMulNBits compute-mode attributes to accuracy level 4, keeping the first and last transformer blocks at level 1. All initializer/weight bytes are unchanged from the preserved `fec39a...` export. ORT selects integer dot-product kernels on supported CPUs and falls back to float computation elsewhere. This is not the previously rejected whole-model int8 conversion. No training occurred.

`tools/configure_int4_compute.py` reproduces this file from the original export contained in the previous source archive or commit `efb8299e168a6edab0a18f4c769d0ddd743808ad`. The original file is not duplicated in the current source archive. The old APK and source remain preserved.

See `docs/experiments/int4-cpu/` for cached pipeline accuracy, timing and compatibility evidence. API 24+, ARM64, ARMv7 and x86_64 libraries are packaged; physical phone performance remains unmeasured. Attribution is in NOTICE.txt and Apache-2.0 in `app/src/main/assets/licenses/ANDROIDX-LITERT-APACHE-2.0.txt`.
