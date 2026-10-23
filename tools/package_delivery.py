import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

ROOT = Path(__file__).resolve().parents[1]


def checksum(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--inspection', type=Path, required=True)
    parser.add_argument('--apk-output', type=Path, required=True)
    parser.add_argument('--source-output', type=Path, required=True)
    parser.add_argument('--apk', type=Path, default=ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
    args = parser.parse_args()
    output_apk = args.apk_output.resolve()
    archive_path = args.source_output.resolve()
    for output in [output_apk, archive_path]:
        assert not output.exists(), f'Refusing to overwrite {output}'
        output.parent.mkdir(parents=True, exist_ok=True)
    apk = args.apk.resolve()
    assert apk.is_file(), 'Build the debug APK before packaging'
    inspected = json.loads(args.inspection.read_text())
    assert checksum(apk) == inspected['apk_sha256'], 'APK differs from the verified artifact'
    shutil.copy2(apk, output_apk)
    files = subprocess.check_output(
        ['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT
    ).decode().split('\0')
    files = sorted({name for name in files if name and not name.startswith(
        ('preservation/', 'delivery/', 'apks/', 'source/', 'backup/'))})
    inventory = [{'path': name, 'bytes': (ROOT / name).stat().st_size, 'sha256': checksum(ROOT / name)}
                 for name in files]
    manifest = {'apk_sha256': checksum(output_apk), 'files': inventory}
    with ZipFile(archive_path, 'w', ZIP_DEFLATED) as archive:
        for name in files:
            archive.write(ROOT / name, 'GarbageClassifier/' + name)
        archive.writestr('GarbageClassifier/SOURCE_MANIFEST.json', json.dumps(manifest, indent=2) + '\n')
    with ZipFile(archive_path) as archive:
        assert archive.testzip() is None
        for item in inventory:
            data = archive.read('GarbageClassifier/' + item['path'])
            assert hashlib.sha256(data).hexdigest() == item['sha256'], item['path']
    for path in [output_apk, archive_path]:
        digest = f'{checksum(path)}  {path.name}\n'
        (path.parent / 'SHA256SUMS.txt').write_text(digest)
        print(digest, end='')
    print(f'Packaged {len(files)} source/evidence files and verified every archived file')


if __name__ == '__main__':
    main()
