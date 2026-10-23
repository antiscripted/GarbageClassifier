import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]


def run(*command):
    return subprocess.check_output([str(part) for part in command], text=True, stderr=subprocess.STDOUT)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--original-apk', type=Path, default=ROOT / 'delivery/GarbageClassifier-debug.apk')
    parser.add_argument('--new-model', action='store_true')
    args = parser.parse_args()
    build_tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0'
    contract = json.loads((ROOT / 'docs/model/contract.json').read_text())
    permissions = run(build_tools / 'aapt', 'dump', 'permissions', args.apk).splitlines()
    permissions = [line for line in permissions if line.startswith('uses-permission:')]
    assert permissions == [
        "uses-permission: name='android.permission.CAMERA'",
        "uses-permission: name='org.garbageclassifier.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'"
    ], permissions
    badging = run(build_tools / 'aapt', 'dump', 'badging', args.apk)
    assert "sdkVersion:'24'" in badging and "targetSdkVersion:'35'" in badging
    signature = run(build_tools / 'apksigner', 'verify', '--verbose', '--print-certs', args.apk)
    assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature
    certificate = next(line for line in signature.splitlines() if 'certificate SHA-256 digest:' in line)
    same_certificate = None
    if args.original_apk.is_file():
        original_signature = run(build_tools / 'apksigner', 'verify', '--print-certs', args.original_apk)
        same_certificate = certificate in original_signature
        assert same_certificate
    run(build_tools / 'zipalign', '-c', '-P', '16', '4', args.apk)
    libraries = []
    candidate_sha = None
    with ZipFile(args.apk) as archive, tempfile.TemporaryDirectory() as temporary:
        model = archive.read('assets/trash_detector.tflite')
        model_sha = hashlib.sha256(model).hexdigest()
        assert model_sha == contract['sha256'] and len(model) == contract['bytes']
        assert archive.testzip() is None
        if args.new_model:
            overhead = args.apk.stat().st_size - sum(item.compress_size for item in archive.infolist())
            assert overhead < 1024 * 1024, 'Repackage the APK to remove retained ZIP space'
            candidate = archive.read('assets/material_classifier.onnx')
            candidate_sha = hashlib.sha256(candidate).hexdigest()
            assert candidate_sha == '63802ff39c00c52663846e4bf7c0dfe511e4e4242398a24240bce733cdbdf366'
            assert len(candidate) == 56733021
            assert archive.getinfo('assets/material_classifier.onnx').compress_type == 0
            with ZipFile(ROOT / 'tools/android-runtime/onnxruntime-android-1.22.0.aar') as runtime:
                for abi in ['arm64-v8a', 'armeabi-v7a', 'x86_64']:
                    for library in ['libonnxruntime.so', 'libonnxruntime4j_jni.so']:
                        assert archive.read(f'lib/{abi}/{library}') == runtime.read(f'jni/{abi}/{library}')
            dex = b''.join(archive.read(name) for name in archive.namelist() if name.endswith('.dex'))
            assert b'Lorg/garbageclassifier/camera/CenterScanner;' in dex
            assert b'Lorg/garbageclassifier/camera/MaterialClassifier;' in dex
            assert b'Lai/onnxruntime/OrtSession;' in dex
        else:
            assert not any(name.endswith('.onnx') or 'onnxruntime' in name for name in archive.namelist())
        assert not any(name.lower().endswith(('.jpg', '.jpeg')) for name in archive.namelist())
        for name in sorted(archive.namelist()):
            if name.startswith('lib/') and name.endswith('.so'):
                path = Path(temporary) / Path(name).name
                path.write_bytes(archive.read(name))
                segments = [line.split() for line in run('readelf', '-lW', path).splitlines()
                            if line.strip().startswith('LOAD ')]
                alignment = [segment[-1] for segment in segments]
                required_alignment = 4096 if args.new_model and '/armeabi-v7a/' in name else 16384
                assert alignment and all(int(value, 16) >= required_alignment for value in alignment)
                libraries.append(dict(path=name, load_segment_alignment=alignment))
        assert len(libraries) == (15 if args.new_model else 9)
        assert {item['path'].split('/')[1] for item in libraries} == {
            'arm64-v8a', 'armeabi-v7a', 'x86_64'}
        for name in ['NOTICE.txt', 'MODEL-AND-APP-AGPL-3.0.txt',
                     'ANDROIDX-LITERT-APACHE-2.0.txt', 'LITERT-NOTICES.txt']:
            location = 'newModel' if args.new_model and name == 'NOTICE.txt' else 'main'
            assert archive.read('assets/licenses/' + name) == (
                ROOT / f'app/src/{location}/assets/licenses' / name).read_bytes()
        catalog = json.loads(archive.read('assets/licenses/index.json'))
        accessible = {name for entry in catalog for name in entry['files']}
        packaged = {Path(name).name for name in archive.namelist()
                    if name.startswith('assets/licenses/') and name.endswith('.txt')}
        assert accessible == packaged
        for name in accessible | {'index.json'}:
            source = ROOT / 'app/src/newModel/assets/licenses' / name
            if not args.new_model or not source.exists():
                source = ROOT / 'app/src/main/assets/licenses' / name
            assert archive.read('assets/licenses/' + name) == source.read_bytes()
    report = dict(apk_bytes=args.apk.stat().st_size,
                  apk_sha256=hashlib.sha256(args.apk.read_bytes()).hexdigest(),
                  model_bytes=len(model), model_sha256=model_sha, permissions=permissions,
                  native_libraries=libraries, minimum_sdk=24, target_sdk=35,
                  test_photos_in_main_apk=False, signature_v2=True,
                  same_signing_certificate_as_original=same_certificate, zipalign_16kb=True,
                  bundled_license_texts_verified=True)
    report['license_catalog_entries'] = len(catalog)
    report['all_license_texts_accessible'] = True
    report['candidate_weights_or_runtime_in_scanner_apk'] = args.new_model
    report['candidate_model_sha256'] = candidate_sha
    report['production_inference_verified'] = False if args.new_model else None
    args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
