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
mkdir -p build/snapshot-media
# Needs a Python with Pillow; the system one in the cloud container is broken,
# so point SNAPSHOT_PYTHON at a venv there.
"${SNAPSHOT_PYTHON:-python3}" - "$repo/content/media" build/snapshot-media <<'PY'
import sys, pathlib
from PIL import Image
src, out = map(pathlib.Path, sys.argv[1:])
for f in src.glob("*.webp"):
    Image.open(f).convert("RGB").resize((800, 500)).save(out / (f.stem + ".png"))
PY
sed -i 's/    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false/&\n    id("app.cash.paparazzi") version "2.0.0-alpha02" apply false/' build.gradle.kts
sed -i '0,/id("org.jetbrains.kotlin.plugin.compose")$/s//id("org.jetbrains.kotlin.plugin.compose")\n    id("app.cash.paparazzi")/' app/build.gradle.kts
# The screens are private to MainActivity.kt; the copy opens them to the snapshot test.
sed -i 's/^private fun \(Masthead\|BottomNavigation\|FeedScreen\|ExploreScreen\|AboutScreen\)/internal fun \1/; s/^private enum class NavTab/internal enum class NavTab/' \
  app/src/main/java/com/caceras/aibrief/MainActivity.kt
# Pictures load from the network in the app; here each published cover is read
# from content/media on the first frame, so snapshots show the real artwork.
sed -i 's/^    val bitmap = image?.bitmap$/    val bitmap = image?.bitmap ?: snapshotBitmap(imageUrl)/' \
  app/src/main/java/com/caceras/aibrief/ui/components/ArticleImage.kt
cat > app/src/main/java/com/caceras/aibrief/ui/components/SnapshotImages.kt <<KT
package com.caceras.aibrief.ui.components

import android.graphics.Bitmap
import java.io.File

private val decoded = HashMap<String, Bitmap?>()

/** Reads the PNG with ImageIO (by reflection: unit tests compile against android.jar). */
internal fun snapshotBitmap(url: String): Bitmap? = decoded.getOrPut(url) {
    val file = File("$work/build/snapshot-media", url.substringAfterLast('/').substringBeforeLast('.') + ".png")
    if (!file.exists()) return@getOrPut null
    val image = Class.forName("javax.imageio.ImageIO").getMethod("read", File::class.java).invoke(null, file)
    val cls = image.javaClass
    val w = cls.getMethod("getWidth").invoke(image) as Int
    val h = cls.getMethod("getHeight").invoke(image) as Int
    val pixels = IntArray(w * h)
    cls.getMethod("getRGB", Int::class.java, Int::class.java, Int::class.java, Int::class.java, IntArray::class.java,
        Int::class.java, Int::class.java).invoke(image, 0, 0, w, h, pixels, 0, w)
    Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}
KT
mkdir -p app/src/test/java/com/caceras/aibrief/snap
for f in "$repo"/scripts/snapshots/*.kt.txt; do cp "$f" "app/src/test/java/com/caceras/aibrief/snap/$(basename "$f" .txt)"; done
./gradlew -q recordPaparazziRelease --tests '*Snaps*'
mkdir -p "$out"
cp app/src/test/snapshots/images/*.png "$out/"
echo "Snapshots written to $out"
