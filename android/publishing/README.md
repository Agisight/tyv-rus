# Google Play publication assets

Prepared for Android 1.0 (versionCode 1), package `ru.tyv.sostuk`, on 2026-10-15.

Open `index.html` for the local visual review. Follow `GOOGLE-PLAY-CHECKLIST.ru.md` for the Console fields and outstanding publication steps. The complete local upload kit, including the signed AAB, is in `../release-artifacts/google-play-1.0/`; the ZIP is alongside it. Generated release bundles and ZIPs are ignored by Git.

- `phone-screenshots/`: seven upload-ready 1080×1920 RGB PNGs, in upload order. The short headline occupies less than 20% of the image; the actual captured app screen remains intact below it.
- `raw-phone-screenshots/`: original unframed captures at 1080×1920. The rendering layouts link to these originals.
- `tablet-screenshots/`: four 1920×1080 RGB PNGs of the actual large-screen layout, without extra headings. These use an emulator viewport with a 720dp minimum dimension, not a photograph of a physical tablet.
- `screenshot-layouts/`: self-contained local HTML/CSS compositions. Render in Chromium at 1080×1920, device scale factor 1, with local file access enabled; export as RGB PNG.
- `graphics/`: SVG sources and upload-ready 512×512 RGBA icon / 1024×500 RGB feature graphic. SVGs render at their declared pixel dimensions. The store icon is a flat vector interpretation of the existing flags; the installed launcher icon is unchanged.
- `listing/ru-RU/`: plain UTF-8 fields for the Russian store listing and release notes.
- `asset-captions.json`: concise accessibility descriptions.
- `privacy.html`: public-page source, ready to host. No live privacy-policy URL is configured or claimed here.
- `validation.json`: asset dimensions, PNG modes, text lengths and release checksum.

Screenshots were captured from the unpublished 2.1.5 debug UI (identical to the 1.0 release UI) on the API 36 emulator. A temporary, separate instrumentation helper only navigated the existing view model; no screenshot-only UI was added to the app. The helper was removed and emulator display settings restored afterwards. The upload AAB is the signed release variant of the same application sources.

Image sizes and text limits follow [Google Play preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en). The package identity and signature were verified again during kit preparation; the existing app regression run passed 34 instrumentation tests. The public release was rebuilt as 1.0 (versionCode 1); only version metadata changed. Existing screenshots still accurately represent its interface.
