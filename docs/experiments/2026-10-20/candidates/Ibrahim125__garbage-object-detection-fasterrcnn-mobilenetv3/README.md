---
library_name: pytorch
pipeline_tag: object-detection
license: cc-by-4.0
tags:
- object-detection
- pytorch
- torchvision
- faster-rcnn
- garbage-detection
datasets:
- keremberke/garbage-object-detection
---

# Garbage Object Detection — Faster R-CNN MobileNetV3

This repository contains a TorchVision Faster R-CNN model fine-tuned on
[`keremberke/garbage-object-detection`](https://huggingface.co/datasets/keremberke/garbage-object-detection).

## Architecture

- Faster R-CNN
- MobileNetV3-Large 320-FPN backbone
- 6 foreground classes plus background

## Classes

- `1`: biodegradable
- `2`: cardboard
- `3`: glass
- `4`: metal
- `5`: paper
- `6`: plastic

## Repository files

- `model.pth`: model weights and inference metadata
- `config.json`: readable configuration and evaluation metrics

## Bounding boxes

Predictions use absolute pixel coordinates in
`[x_min, y_min, x_max, y_max]` format.

## Evaluation

```json
{
  "map": 0.12462564557790756,
  "map_50": 0.2073580026626587,
  "map_75": 0.12029992789030075,
  "map_small": 0.0007594031631015241,
  "map_medium": 0.011886218562722206,
  "map_large": 0.22296744585037231,
  "mar_1": 0.152291402220726,
  "mar_10": 0.2860201895236969,
  "mar_100": 0.29847797751426697
}
```

These results were produced using notebook run mode `quick`.
