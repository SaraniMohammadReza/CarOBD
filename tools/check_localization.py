#!/usr/bin/env python3
"""Host resource contract: coverage, duplicate keys and positional formatter compatibility."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
def load(folder):
    result = {}
    for path in (ROOT / 'app/src/main/res' / folder).glob('*.xml'):
        for node in ET.parse(path).getroot().findall('string'):
            key = node.attrib['name']
            assert key not in result, f'Duplicate {folder}/{key}'
            result[key] = ''.join(node.itertext())
    for path in (ROOT / 'app/src/main/res' / folder).glob('*.xml'):
        for node in ET.parse(path).getroot().findall('plurals'):
            for item in node.findall('item'):
                key = node.attrib['name'] + '[' + item.attrib['quantity'] + ']'
                assert key not in result
                result[key] = ''.join(item.itertext())
    for path in (ROOT / 'app/src/main/res' / folder).glob('*.xml'):
        for node in ET.parse(path).getroot().findall('string-array'):
            for i, item in enumerate(node.findall('item')):
                key = node.attrib['name'] + '[' + str(i) + ']'
                assert key not in result
                result[key] = ''.join(item.itertext())
    return result
def placeholders(text):
    tokens = re.findall(r'%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z%])', text)
    result = []; implicit = 0
    for position, kind in tokens:
        if kind == '%': continue
        implicit += 1
        result.append((int(position) if position else implicit, kind))
    return sorted(result)
def main():
    en, fa = load('values'), load('values-fa')
    assert en.keys() == fa.keys(), f'Missing keys: {en.keys() ^ fa.keys()}'
    for key in en:
        assert placeholders(en[key]) == placeholders(fa[key]), f'Format mismatch: {key}: {placeholders(en[key])} vs {placeholders(fa[key])}'
    assert placeholders('%2$s %1$d') == [(1,'d'),(2,'s')]
    assert placeholders('%1$s') != placeholders('%1$d')
    print(f'PASS: {len(en)} EN/FA resources, unique keys and matching format arguments; visual RTL not tested')
if __name__ == '__main__': main()
