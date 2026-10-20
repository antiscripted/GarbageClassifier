import argparse
import concurrent.futures
import hashlib
import json
from pathlib import Path
import urllib.parse
import urllib.request
from zipfile import ZipFile


REALWASTE_REVISION = "c626504f80c98ffe0d437e8ebf65a642e8d9fdba"
TACO_REVISION = "c23700b465af288488bd7b4544c4166b9a970edf"
MATERIALS = {
    "Cardboard": "Paper", "Food Organics": "Food waste", "Glass": "Glass",
    "Metal": "Metal", "Paper": "Paper", "Plastic": "Plastic",
}
TACO_MATERIALS = {
    0: "Metal", 4: "Plastic", 5: "Plastic", 6: "Glass", 7: "Plastic",
    8: "Metal", 9: "Glass", 10: "Metal", 12: "Metal", 13: "Paper",
    14: "Paper", 15: "Paper", 17: "Paper", 19: "Paper", 21: "Plastic",
    23: "Glass", 24: "Plastic", 25: "Food waste", 26: "Glass",
    27: "Plastic", 28: "Metal", 29: "Plastic", 30: "Paper", 32: "Paper",
    33: "Paper", 34: "Paper", 36: "Plastic", 37: "Plastic", 38: "Plastic",
    39: "Plastic", 40: "Plastic", 41: "Plastic", 43: "Plastic",
    44: "Plastic", 45: "Plastic", 47: "Plastic", 49: "Plastic",
    50: "Metal", 52: "Metal", 55: "Plastic",
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def get_json(url, destination):
    if not destination.exists():
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(urllib.request.urlopen(url, timeout=45).read())
    return json.loads(destination.read_text())


def ranked(rows, key):
    return sorted(rows, key=lambda row: digest(("20261020/" + str(key(row))).encode()))


def download(root, row):
    path = root / row["path"]
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        data = urllib.request.urlopen(row["url"], timeout=45).read()
        if row.get("sha256"):
            assert digest(data) == row["sha256"], row["id"]
        path.write_bytes(data)
    actual = digest(path.read_bytes())
    if row.get("sha256"):
        assert actual == row["sha256"], row["id"]
    row["sha256"] = actual
    return row


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--trashnet", type=Path, required=True)
    args = parser.parse_args()
    root = args.output
    root.mkdir(parents=True, exist_ok=True)
    manifest = []
    assert digest(args.trashnet.read_bytes()) == "0bf472790f8b20e5c950d5b5012a9d38af0d3392efd65f8ce171334fc16b07c2"
    with ZipFile(args.trashnet) as archive:
        for category in ["glass", "plastic", "metal", "paper", "cardboard", "trash"]:
            for index in range(11, 51):
                member = f"dataset-resized/{category}/{category}{index}.jpg"
                path = root / "trashnet" / member
                path.parent.mkdir(parents=True, exist_ok=True)
                data = archive.read(member)
                path.write_bytes(data)
                expected = "Paper" if category == "cardboard" else category.capitalize()
                manifest.append(dict(id=f"trashnet/{member}", path=str(path.relative_to(root)),
                                     dataset="trashnet", category=category, sha256=digest(data),
                                     expected=None if category == "trash" else expected))
    api = get_json(f"https://huggingface.co/api/datasets/shahzaibvohra/realwaste/revision/{REALWASTE_REVISION}?blobs=true",
                   root / "realwaste-metadata.json")
    rows = []
    categories = sorted({s["rfilename"].rsplit("_", 1)[0] for s in api["siblings"]
                         if s["rfilename"].endswith(".jpg")})
    for category in categories:
        available = [s for s in api["siblings"] if s["rfilename"].startswith(category + "_")]
        for spec in ranked(available, lambda s: s["rfilename"])[:40]:
            filename = spec["rfilename"]
            rows.append(dict(id="realwaste/" + filename, path="realwaste/" + filename,
                             dataset="realwaste", category=category, expected=MATERIALS.get(category),
                             url=f"https://huggingface.co/datasets/shahzaibvohra/realwaste/resolve/{REALWASTE_REVISION}/{urllib.parse.quote(filename)}",
                             sha256=spec["lfs"]["sha256"]))
    taco = get_json(f"https://huggingface.co/datasets/RandyHuynh5815/TACO-Waste-Recognition/resolve/{TACO_REVISION}/annotations.json",
                    root / "taco-annotations.json")
    selected = set()
    for category in [1, 4, 5, 6, 10, 12, 17, 25, 33, 53, 59]:
        candidates = {a["image_id"] for a in taco["annotations"] if a["category_id"] == category}
        selected.update(ranked(candidates, lambda item: item)[:12])
    names = {c["id"]: c["name"] for c in taco["categories"]}
    for image in taco["images"]:
        if image["id"] not in selected:
            continue
        targets = []
        for annotation in taco["annotations"]:
            if annotation["image_id"] != image["id"]:
                continue
            x, y, width, height = annotation["bbox"]
            category = annotation["category_id"]
            targets.append(dict(id=annotation["id"], category=names[category],
                                expected=TACO_MATERIALS.get(category),
                                box=[x, y, x + width, y + height]))
        filename = image["file_name"]
        rows.append(dict(id="taco/" + filename, path="taco/" + filename, dataset="taco",
                         category="scene", expected=None, targets=targets,
                         annotated_size=[image["width"], image["height"]],
                         original_url=image["flickr_url"],
                         url=f"https://huggingface.co/datasets/RandyHuynh5815/TACO-Waste-Recognition/resolve/{TACO_REVISION}/{filename}"))
    errors = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        futures = {pool.submit(download, root, row): row for row in rows}
        for number, future in enumerate(concurrent.futures.as_completed(futures), 1):
            try:
                manifest.append(future.result())
            except Exception as error:
                errors.append(dict(id=futures[future]["id"], error=str(error)))
            if number % 50 == 0:
                print(f"Downloaded/verified {number}/{len(rows)}; errors {len(errors)}", flush=True)
    manifest.sort(key=lambda row: row["id"])
    (root / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    (root / "download-errors.json").write_text(json.dumps(errors, indent=2) + "\n")
    print({dataset: sum(row["dataset"] == dataset for row in manifest)
           for dataset in ["trashnet", "realwaste", "taco"]}, flush=True)
    if errors:
        raise SystemExit(f"{len(errors)} images failed; inspect download-errors.json")


if __name__ == "__main__":
    main()
