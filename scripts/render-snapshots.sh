#!/usr/bin/env bash
# Renders the main screens to PNG without a device, using Paparazzi in a
# throwaway copy of the repo, so the app's own build stays free of it.
# Usage: scripts/render-snapshots.sh [output-dir]   (needs the Android SDK set up as in CLAUDE.md)
set -euo pipefail
repo="$(cd "$(dirname "$0")/.." && pwd)"
out="${1:-$repo/build/snapshots}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
tar -C "$repo" --exclude=./app/build --exclude=./.gradle --exclude=./.git --exclude=./build -cf - . | tar -xf - -C "$work"
cd "$work"
sed -i 's/    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false/&\n    id("app.cash.paparazzi") version "2.0.0-alpha02" apply false/' build.gradle.kts
sed -i '0,/id("org.jetbrains.kotlin.plugin.compose")$/s//id("org.jetbrains.kotlin.plugin.compose")\n    id("app.cash.paparazzi")/' app/build.gradle.kts
# The screens are private to MainActivity.kt; the copy opens them to the snapshot test.
sed -i 's/^private fun \(Masthead\|BottomNavigation\|FeedScreen\|SavedScreen\|AboutScreen\)/internal fun \1/; s/^private enum class NavTab/internal enum class NavTab/' \
  app/src/main/java/com/caceras/aibrief/MainActivity.kt
mkdir -p app/src/test/java/com/caceras/aibrief/snap
for f in "$repo"/scripts/snapshots/*.kt.txt; do cp "$f" "app/src/test/java/com/caceras/aibrief/snap/$(basename "$f" .txt)"; done
./gradlew -q recordPaparazziRelease --tests '*Snaps*'
mkdir -p "$out"
cp app/src/test/snapshots/images/*.png "$out/"
echo "Snapshots written to $out"
