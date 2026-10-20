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
    delivery = ROOT / 'delivery'
    delivery.mkdir(exist_ok=True)
    apk = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
    assert apk.is_file(), 'Build the debug APK before packaging'
    inspected = json.loads((ROOT / 'docs/validation/final/apk-inspection.json').read_text())
    assert checksum(apk) == inspected['apk_sha256'], 'APK differs from the verified artifact'
    output_apk = delivery / 'GarbageClassifier-debug.apk'
    shutil.copy2(apk, output_apk)
    files = subprocess.check_output(
        ['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT
    ).decode().split('\0')
    files = sorted({name for name in files if name and not name.startswith(('preservation/', 'delivery/'))})
    inventory = [{'path': name, 'bytes': (ROOT / name).stat().st_size, 'sha256': checksum(ROOT / name)}
                 for name in files]
    manifest = {'apk_sha256': checksum(output_apk), 'files': inventory}
    archive_path = delivery / 'GarbageClassifier-source.zip'
    with ZipFile(archive_path, 'w', ZIP_DEFLATED) as archive:
        for name in files:
            archive.write(ROOT / name, 'GarbageClassifier/' + name)
        archive.writestr('GarbageClassifier/SOURCE_MANIFEST.json', json.dumps(manifest, indent=2) + '\n')
    with ZipFile(archive_path) as archive:
        assert archive.testzip() is None
        for item in inventory:
            data = archive.read('GarbageClassifier/' + item['path'])
            assert hashlib.sha256(data).hexdigest() == item['sha256'], item['path']
    (delivery / 'SHA256SUMS.txt').write_text(
        ''.join(f'{checksum(path)}  {path.name}\n' for path in [output_apk, archive_path])
    )
    print(f'Packaged {len(files)} source/evidence files and verified every archived file')
    print((delivery / 'SHA256SUMS.txt').read_text())


if __name__ == '__main__':
    main()
