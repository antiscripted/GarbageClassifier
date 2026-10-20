# Indian waste-segregation policy used by this app

The first version supports a deliberately limited material-based wet/dry decision. It does not claim to implement every municipal collection rule. Local authority instructions take precedence, including any current sanitary, special-care, e-waste or hazardous-waste streams. Bin colors vary by locality; the app uses names rather than inferring a bin type from its color.

| Verified detector label | App recommendation | Assumption and limitation |
| --- | --- | --- |
| Food waste | Wet | Ordinary biodegradable kitchen/food waste, separated from its packaging. The detector cannot establish chemical or medical contamination. |
| Glass | Dry | Ordinary separated household glass. It cannot identify dangerous contents or guarantee that glass is unbroken. |
| Metal | Dry | Ordinary separated metal packaging/material. Batteries, electronics, pressurized containers and contaminated metal must not be inferred from appearance alone. |
| Plastic | Dry | Ordinary emptied plastic material. Medical plastic, chemical containers and mixed or food-filled packaging need a different decision. |
| Paper | Dry | Ordinary separated paper/cardboard. Wet, soiled, laminated and composite material can require different local handling. |
| Not Recyclable | No recommendation | This label is too broad to choose a safe bin. |
| Textile, Wooden, Leather, Ceramic | No recommendation | Collection routes depend on condition, treatment, reuse and local rules. The label alone is insufficient. |
| Unknown or uncertain | No recommendation | “Object not clear”; no default wet/dry guess. |

Food and other biodegradable kitchen waste belong in the wet stream; separated ordinary dry materials belong in the dry stream under Indian source-segregation practice. **Dry does not mean recyclable**, and the model label “Not Recyclable” does not mean wet. The implementation maps explicit class IDs, not English keyword guesses.

The app cannot reliably identify sanitary waste, medicines, sharps, batteries, e-waste, chemical residues, contaminated packaging or other special-care items. No sanitary/special-care classifier or recommendation is enabled. An object from those groups can still be mistaken for a common material by the model, so the app must not be used to decide their disposal. Follow the local authority's separate collection instructions instead.

## What bin inspection establishes

The user explicitly selects Wet or Dry. For each confirmed supported material, the app compares its recommended bin with that selection. A mismatch is “Wrong bin”, a match “Correct bin”, and an unconfirmed/unsupported detection “Object not clear”. No verdict is made about unseen contents, overall bin cleanliness, contamination or completeness. The uncertainty state has no wet/dry fallback.

## Sources and review status

The policy follows the longstanding Indian practice of segregating biodegradable and non-biodegradable household waste at source and keeping hazardous/sanitary material separate. Official references for the teacher/local authority to check are the [CPCB waste rules directory](https://cpcb.nic.in/rules-2/) and [Ministry of Environment, Forest and Climate Change](https://moef.gov.in/). The Solid Waste Management Rules, 2016 are historical background; this document does not assert that their wording is the latest law on the competition date.

Live retrieval of the official sites was blocked by this cloud environment's network proxy on 19 October 2026. Therefore current national amendments and municipality-specific collection instructions have not been independently verified here. The teacher should check the rules currently in force locally, including any 2026 sanitary/special-care requirements, before the competition. This verification limitation does not change the code's explicit five-material mapping above.
