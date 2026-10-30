#!/usr/bin/env python3
"""Heuristic inventory for review; does not certify visual translation or dynamic catalog text."""
from pathlib import Path
import json, re, sys
root = Path(__file__).resolve().parents[1]
rows = []
for path in sorted((root / 'app/src/main/java/com/sarani/carobd/ui').rglob('*.kt')):
    for number, line in enumerate(path.read_text().splitlines(), 1):
        if line.lstrip().startswith('//'): continue
        if re.search(r'[\u0600-\u06ff]', line):
            rows.append({'path':str(path.relative_to(root)), 'line':number, 'kind':'COMPATIBILITY_HELPER_OR_DYNAMIC_DATA_REVIEW', 'text':line.strip()})
        elif re.search(r'(?:Text\(|text\s*=|title\s*=|contentDescription\s*=).*"', line):
            rows.append({'path':str(path.relative_to(root)), 'line':number, 'kind':'IDENTIFIER_FORMAT_OR_LITERAL_REVIEW', 'text':line.strip()})
print(json.dumps({'scope':'UI Kotlin; heuristic only; dynamic catalog/technical evidence requires separate review', 'entries':rows}, ensure_ascii=False, indent=2))
