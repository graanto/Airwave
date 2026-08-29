import os
import shutil

manifest_path = "apps/tv-native/android/app/src/main/AndroidManifest.xml"
if not os.path.exists(manifest_path):
    print(f"Manifest not found at {manifest_path}")
    exit(1)

with open(manifest_path, "r", encoding="utf-8") as f:
    content = f.read()

# Copy banner images
banner_src = "apps/tv-native/assets/icons/android-tv-banner.png"
if os.path.exists(banner_src):
    for res_dir in ["drawable", "drawable-mdpi", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi"]:
        target_dir = os.path.join("apps/tv-native/android/app/src/main/res", res_dir)
        os.makedirs(target_dir, exist_ok=True)
        shutil.copy(banner_src, os.path.join(target_dir, "tv_banner.png"))
        print(f"Copied banner to {target_dir}")

# Ensure LEANBACK_LAUNCHER
if "android.intent.category.LEANBACK_LAUNCHER" not in content:
    content = content.replace(
        '<category android:name="android.intent.category.LAUNCHER"/>',
        '<category android:name="android.intent.category.LAUNCHER"/>\n        <category android:name="android.intent.category.LEANBACK_LAUNCHER"/>'
    )
    content = content.replace(
        '<category android:name="android.intent.category.LAUNCHER" />',
        '<category android:name="android.intent.category.LAUNCHER" />\n        <category android:name="android.intent.category.LEANBACK_LAUNCHER" />'
    )

# Ensure uses-feature leanback & touchscreen
if "android.software.leanback" not in content:
    feature_tags = '  <uses-feature android:name="android.software.leanback" android:required="false" />\n  <uses-feature android:name="android.hardware.touchscreen" android:required="false" />\n'
    if "<application" in content:
        content = content.replace("<application", feature_tags + "  <application", 1)

# Ensure android:banner is on application & activity
if 'android:banner="@drawable/tv_banner"' not in content:
    content = content.replace("<application ", '<application android:banner="@drawable/tv_banner" ', 1)
    content = content.replace('<activity android:name=".MainActivity"', '<activity android:name=".MainActivity" android:banner="@drawable/tv_banner"', 1)

with open(manifest_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Successfully patched AndroidManifest.xml and added TV banners.")