#!/usr/bin/env python3
"""Compare each archive entry against a provided reference JAR."""
import argparse
from zipfile import ZipFile
p=argparse.ArgumentParser();p.add_argument('rebuilt');p.add_argument('reference');a=p.parse_args()
with ZipFile(a.rebuilt) as b,ZipFile(a.reference) as r:
    bn=set(b.namelist());rn=set(r.namelist())
    missing=sorted(rn-bn);extra=sorted(bn-rn)
    mismatched=sorted(n for n in (bn & rn) if b.read(n)!=r.read(n))
    if missing or extra or mismatched:
        raise SystemExit(f'FAIL: missing={missing}, extra={extra}, bytes_differ={mismatched}')
    print(f'PASS: all {len(bn)} entries match byte-for-byte (ZIP container metadata may differ)')
