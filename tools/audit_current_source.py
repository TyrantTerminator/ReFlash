#!/usr/bin/env python3
"""Conservative inventory gate: FAIL if current JAR top-level classes lack matching .java files.

This is a necessary check, NOT sufficient evidence of GPL corresponding source:
matching filenames alone cannot prove source matches the distributed binary.
"""
import argparse
from pathlib import Path
from zipfile import ZipFile


def source_class_names(roots):
    names = set()
    for root in roots:
        root = Path(root)
        if not root.is_dir():
            raise SystemExit('Missing source directory: ' + str(root))
        for p in root.rglob('*.java'):
            try:
                text = p.read_text(encoding='utf-8')
            except UnicodeError:
                continue
            import re
            pkg = re.search(r'(?m)^\s*package\s+([\w.]+)\s*;', text)
            # Use the filename as the Java primary type, avoiding test source files.
            if pkg:
                names.add(pkg.group(1).replace('.', '/') + '/' + p.stem)
    return names


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('jar', type=Path)
    parser.add_argument('source_roots', type=Path, nargs='+')
    args = parser.parse_args()
    sources = source_class_names(args.source_roots)
    with ZipFile(args.jar) as jar:
        classes = sorted(set(n[:-6] for n in jar.namelist()
                             if n.endswith('.class') and '$' not in n))
    missing = [n for n in classes if n not in sources]
    print(f'Primary classes in JAR: {len(classes)}')
    print(f'Classes with similarly named Java file: {len(classes)-len(missing)}')
    print(f'Classes without a Java source file: {len(missing)}')
    for name in missing:
        print('MISSING:', name + '.java')
    if missing:
        raise SystemExit('NOT READY: source-file coverage is incomplete (further license and clean-build checks required)')
    print('COVERAGE ONLY: all type names present. Still require source correctness, third-party notices, clean independent build and verification.')


if __name__ == '__main__':
    main()
