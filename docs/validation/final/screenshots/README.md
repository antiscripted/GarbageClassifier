# Screenshot evidence

`portrait-bin-inspection.png` is the valid capture from 19 October for the same APK SHA-256 delivered on 20 October. It shows the synthetic emulator camera scene, not actual waste or a phone test.

`emulator-framebuffer-failure.png` records an all-black capture returned by the long-running emulator on 20 October. Android reported the display ON, the UI hierarchy remained readable and the emulator logged an openColorBuffer error. Fresh capture remained unreliable after restarting the emulator, so it is not used as evidence of application rendering.

The older file named landscape-bin-inspection.png was captured during a transition and was actually portrait. It is omitted from final delivery evidence; it remains preserved in the 20 October preservation checkpoint branch. Current orientation/bin-selection results are recorded by actual Android view-bound and state assertions in ../orientation-review.json. Fresh visual/box-alignment checks on a physical Galaxy M51 remain pending.
