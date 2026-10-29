#!/usr/bin/env python3
"""Offline safety checks using installed kotlinc and cached JUnit/coroutines.

This is a supplemental JVM check, not the Android Gradle release gate.
Requires Kotlin 2.x and cached coroutines core/test 1.10.2, JUnit 4.13.2,
and Hamcrest 1.3. Does not download dependencies or modify project sources.
Pass test file stem fragments to select a subset. Outputs go to a temporary dir.
"""
from pathlib import Path
import subprocess,sys,os,tempfile
work=Path(tempfile.mkdtemp(prefix="carobd-safety-"))
r=Path(__file__).resolve().parents[1]
c=Path.home()/'.gradle/caches/modules-2/files-2.1'
def jar(g,a,v):return next(p for p in (c/g/a/v).glob('*/*.jar') if not any(x in p.name for x in ('sources','javadoc')))
cp=':'.join(map(str,[jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.10.2'),jar('org.jetbrains.kotlinx','kotlinx-coroutines-test-jvm','1.10.2'),jar('junit','junit','4.13.2'),jar('org.hamcrest','hamcrest-core','1.3')]))
sources=[str(p) for m in ('model','protocol','adapter','telemetry','vehicle') for p in (r/'core'/m/'src/main').rglob('*.kt')]
tests=[p for m in ('model','protocol','adapter','telemetry','vehicle') for p in (r/'core'/m/'src/test').rglob('*.kt')]
if len(sys.argv)>1:tests=[p for p in tests if any(q in p.stem for q in sys.argv[1:])]
sources += [str(p) for p in (r/'app/src/main/java/com/sarani/carobd/data/session').glob('*.kt')]
extra = list((r/'app/src/test/kotlin/com/sarani/carobd/data/session').glob('*.kt'))
if len(sys.argv)>1: extra = [p for p in extra if any(q in p.stem for q in sys.argv[1:])]
tests += extra
sources += list(map(str,tests))
(work/'sources.txt').write_text('\n'.join(sources))
result=subprocess.run(['kotlinc','@'+str(work/'sources.txt'),'-classpath',cp,'-d',str(work/'tests.jar')])
if result.returncode:sys.exit(result.returncode)
classes=[]
for p in tests:
 import re
 text=p.read_text();package=re.search(r'package ([\w.]+)',text).group(1)
 classes += [package+'.'+name for name in re.findall(r'^class (\w+)',text,re.M) if name.endswith('Test')]
sys.exit(subprocess.run(['kotlin','-classpath',str(work/'tests.jar')+':'+cp,'org.junit.runner.JUnitCore',*classes]).returncode)
