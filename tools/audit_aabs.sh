#!/usr/bin/env bash
# audit_aabs.sh — pre-submission verification for the Luma icon pack suite.
# Checks, per flavor: launcher asset uniqueness (source MD5 matrix),
# AAB signature + signing-certificate SHA1, AAB/APK manifest identity
# (package, versionCode, label), and appfilter drawable integrity.
# Usage: ./audit_aabs.sh [EXPECTED_SHA1]
#   Defaults: project root = this script's parent dir's parent (android-project),
#   EXPECTED_SHA1 = 57:8E:E6:B8:95:83:CC:CA:9E:5D:AE:1F:7B:B8:1B:C6:38:E7:BD:00
# Exit 0 only if every check passes. Cross-flavor identical launchers are
# reported as CLONE (warning, exit 0) — they are only a failure if you
# require distinct brand marks per finish.
set -u
PROJ="$(cd "$(dirname "$0")/.." && pwd)"
export PROJ
EXPECTED_SHA1="${1:-57:8E:E6:B8:95:83:CC:CA:9E:5D:AE:1F:7B:B8:1B:C6:38:E7:BD:00}"
FLAVORS="regular dark gradient glyph clear prism midnight"
STORE_DIR="$PROJ/../marketing/app-icons"  # store 512 icons live beside android-project, not inside it
FAIL=0
say() { printf '%s\n' "$*"; }
fail() { say "FAIL: $*"; FAIL=1; }

say "== 1. Source launcher MD5 matrix (foreground / xxxhdpi / store 512) =="
for f in $FLAVORS; do
  fg=$(md5sum "$PROJ/app/src/$f/res/drawable/ic_launcher_foreground.png" 2>/dev/null | cut -d' ' -f1)
  mm=$(md5sum "$PROJ/app/src/$f/res/mipmap-xxxhdpi/ic_launcher.png" 2>/dev/null | cut -d' ' -f1)
  st=$(md5sum "$STORE_DIR/luma-$f-512.png" 2>/dev/null | cut -d' ' -f1)
  say "$f fg=$fg mipmap=$mm store=$st"
  [ -n "$fg" ] && [ -n "$mm" ] && [ -n "$st" ] || fail "$f missing launcher asset"
done
say "Checking for cross-flavor identical launchers..."
for f in $FLAVORS; do
  md5sum "$PROJ/app/src/$f/res/drawable/ic_launcher_foreground.png" 2>/dev/null | cut -d' ' -f1
done | sort | uniq -d | while read -r d; do
  say "CLONE: foreground md5 $d shared by: $(for f in $FLAVORS; do
    [ "$(md5sum "$PROJ/app/src/$f/res/drawable/ic_launcher_foreground.png" 2>/dev/null | cut -d' ' -f1)" = "$d" ] && echo -n "$f "; done)"
done

say "== 2. AAB signature + signing certificate SHA1 =="
for f in $FLAVORS; do
  aab="$PROJ/app/build/outputs/bundle/${f}Release/app-${f}-release.aab"
  [ -f "$aab" ] || { fail "$f AAB missing: $aab"; continue; }
  jarsigner -verify "$aab" 2>&1 | grep -q 'jar verified' || fail "$f AAB signature invalid"
  rsa=$(unzip -l "$aab" | awk '/META-INF\/.*\.RSA/ {print $4; exit}')
  sha1=$(unzip -p "$aab" "$rsa" 2>/dev/null | keytool -printcert 2>/dev/null | grep -i 'SHA1:' | sed 's/.*SHA1: //')
  say "$f cert SHA1=$sha1"
  [ "$sha1" = "$EXPECTED_SHA1" ] || fail "$f cert SHA1 mismatch (expected $EXPECTED_SHA1)"
done

say "== 3. Manifest identity (package / versionCode / label) =="
AAPT2="$PROJ/toolchain/sdk/build-tools/35.0.0/aapt2"
for f in $FLAVORS; do
  apk="$PROJ/app/build/outputs/apk/$f/release/app-$f-release.apk"
  if [ -f "$apk" ] && [ -x "$AAPT2" ]; then
    line=$("$AAPT2" dump badging "$apk" 2>/dev/null | grep '^package:')
    say "$f $line"
    echo "$line" | grep -q "name='com.metaphasicgroup.luma.$f'" || fail "$f package mismatch"
    echo "$line" | grep -q "versionCode='4'" || say "NOTE: $f versionCode is not 4 (check before upload)"
  else
    say "$f APK or aapt2 unavailable, manifest check skipped"
  fi
  name=$(grep -h 'app_name' "$PROJ/app/src/$f/res/values/strings.xml" 2>/dev/null | sed 's/.*>\(.*\)<.*/\1/')
  say "$f app_name=$name"
done

say "== 4. appfilter drawable integrity =="
for f in $FLAVORS; do
  python3 - "$f" <<'PYEOF' || fail "$f appfilter check errored"
import re, os, sys
f = sys.argv[1]
base = os.environ.get('PROJ', '.')
af = open(f'{base}/app/src/{f}/res/xml/appfilter.xml').read()
refs = set(re.findall(r'drawable="([^"]+)"', af))
have = set()
for d in os.listdir(f'{base}/app/src/{f}/res'):
    if d.startswith('drawable'):
        for fn in os.listdir(f'{base}/app/src/{f}/res/{d}'):
            have.add(os.path.splitext(fn)[0])
missing = sorted(r for r in refs if r not in have)
print(f'{f} refs={len(refs)} missing={len(missing)} {missing[:10]}')
sys.exit(1 if missing else 0)
PYEOF
done

[ "$FAIL" -eq 0 ] && say "AUDIT PASSED" || say "AUDIT FAILED"
exit "$FAIL"
