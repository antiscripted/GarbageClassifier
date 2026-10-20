---
language: en
license: apache-2.0
library_name: keras
tags:
- image-classification
- waste-classification
- tensorflow
- keras
- efficientnet
datasets:
- alistairking/recyclable-and-household-waste-classification
metrics:
- accuracy
---

# Waste Classification V2 (EfficientNet)

30-class waste image classifier built with EfficientNet transfer learning.

## Model Details
- **Architecture:** EfficientNetB0 + custom classification head
- **Input size:** 224 x 224 x 3
- **Framework:** TensorFlow / Keras
- **Author:** Darshan764

## Performance
- **Validation Accuracy:** 87.3%
- **Validation set:** 3,000 images (20% held-out)
- **Number of classes:** 30

## Classes
aerosol_cans, aluminum_food_cans, aluminum_soda_cans, cardboard_boxes,
cardboard_packaging, clothing, coffee_grounds, disposable_plastic_cutlery,
eggshells, food_waste, glass_beverage_bottles, glass_cosmetic_containers,
glass_food_jars, magazines, newspaper, office_paper, paper_cups,
plastic_cup_lids, plastic_detergent_bottles, plastic_food_containers,
plastic_shopping_bags, plastic_soda_bottles, plastic_straws,
plastic_trash_bags, plastic_water_bottles, shoes, steel_food_cans,
styrofoam_cups, styrofoam_food_containers, tea_bags

## Dataset
[Recyclable and Household Waste Classification](https://www.kaggle.com/datasets/alistairking/recyclable-and-household-waste-classification)
- 15,000 images total (250 per class)
- 80/20 train/validation split (seed=42)

## Training
- Optimizer: Adam
- Loss: sparse_categorical_crossentropy
- Batch size: 32
- Callbacks: EarlyStopping, ReduceLROnPlateau, ModelCheckpoint

## How to Use

```python
import tensorflow as tf
import numpy as np
from tensorflow.keras.utils import load_img, img_to_array

model = tf.keras.models.load_model("hf://Darshan764/waste-classification-v2")

img = load_img("waste.jpg", target_size=(224, 224))
arr = np.expand_dims(img_to_array(img), axis=0)

pred = model.predict(arr, verbose=0)
idx = int(np.argmax(pred[0]))
print(f"Class index: {idx}")
print(f"Confidence: {pred[0][idx]*100:.2f}%")
```

## Limitations
- Trained only on the Kaggle dataset above; may not generalize to very different lighting/backgrounds.
- Not intended for production waste-sorting systems without further validation.

---
language: en
license: apache-2.0
library_name: keras
tags:
- image-classification
- waste-classification
- tensorflow
- keras
- efficientnet
datasets:
- alistairking/recyclable-and-household-waste-classification
metrics:
- accuracy
---

# Waste Classification V2 (EfficientNet)

30-class waste image classifier built with EfficientNet transfer learning.

## Model Details
- **Architecture:** EfficientNetB0 + custom classification head
- **Input size:** 224 x 224 x 3
- **Framework:** TensorFlow / Keras
- **Author:** Darshan764

## Performance
- **Validation Accuracy:** 87.3%
- **Validation set:** 3,000 images (20% held-out)
- **Number of classes:** 30

## Classes
aerosol_cans, aluminum_food_cans, aluminum_soda_cans, cardboard_boxes,
cardboard_packaging, clothing, coffee_grounds, disposable_plastic_cutlery,
eggshells, food_waste, glass_beverage_bottles, glass_cosmetic_containers,
glass_food_jars, magazines, newspaper, office_paper, paper_cups,
plastic_cup_lids, plastic_detergent_bottles, plastic_food_containers,
plastic_shopping_bags, plastic_soda_bottles, plastic_straws,
plastic_trash_bags, plastic_water_bottles, shoes, steel_food_cans,
styrofoam_cups, styrofoam_food_containers, tea_bags

## Dataset
[Recyclable and Household Waste Classification](https://www.kaggle.com/datasets/alistairking/recyclable-and-household-waste-classification)
- 15,000 images total (250 per class)
- 80/20 train/validation split (seed=42)

## Training
- Optimizer: Adam
- Loss: sparse_categorical_crossentropy
- Batch size: 32
- Callbacks: EarlyStopping, ReduceLROnPlateau, ModelCheckpoint

## How to Use

```python
import tensorflow as tf
import numpy as np
from tensorflow.keras.utils import load_img, img_to_array

model = tf.keras.models.load_model("hf://Darshan764/waste-classification-v2")

img = load_img("waste.jpg", target_size=(224, 224))
arr = np.expand_dims(img_to_array(img), axis=0)

pred = model.predict(arr, verbose=0)
idx = int(np.argmax(pred[0]))
print(f"Class index: {idx}")
print(f"Confidence: {pred[0][idx]*100:.2f}%")
```

## Limitations
- Trained only on the Kaggle dataset above; may not generalize to very different lighting/backgrounds.
- Not intended for production waste-sorting systems without further validation.
## 🎮 Live Demo

Try the model in your browser (upload any waste image):

[**Waste Classification Demo**](https://waste-classification-demo-fwv65knoqoz7dgfwdsauhn.streamlit.app)

**Sample verification** — uploaded a real-world plastic water bottle image:

| Rank | Class | Confidence |
|------|-------|-----------|
| 1 | plastic_water_bottles | 92.7% |
| 2 | plastic_soda_bottles | 6.9% |
| 3 | plastic_trash_bags | 0.2% |