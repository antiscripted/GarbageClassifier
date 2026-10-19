---
license: agpl-3.0
language:
- en
metrics:
- precision
base_model:
- Ultralytics/YOLOv8
pipeline_tag: object-detection
tags:
- trash-detection
- YOLO
- structured-pruning
- pruned
---

Here are a series of pruned and quantized YOLOv8 models I generated for my master's thesis.

Here are my main results:

![image/png](https://cdn-uploads.huggingface.co/production/uploads/651bd5778010d2458e1d8436/zP8vCTy0AUu0NaWuRhPP1.png)

As you can see, accuracy quickly drops as the pruning ratio increases (the pruning ratio is shown by the black label next to each point on the graph).
Quantization and input image resolution reduction offer much better trade-offs in terms of speedup and accuracy! Structured pruning turned out to be too aggressive,
whereas quantization and resolution reduction proved to be much more effective! Keep in mind that YOLO already has a very efficient architecture.
Structured pruning tends to be more effective on over-parameterized models,
such as AlexNet or VGG-Net. Also, detection models are much more sensitive to pruning than plain classification ones.

Here are some real prediction examples:

![image/png](https://cdn-uploads.huggingface.co/production/uploads/651bd5778010d2458e1d8436/MOG2qxEbIkkWctTKloYrm.png)

Check out the code repo with all the code for model optimization: https://github.com/Alejandro-Casanova/YOLOv8-Pruned

And the repo with the demo Android App: https://github.com/Alejandro-Casanova/Android-Trash-Detection-with-YOLO

