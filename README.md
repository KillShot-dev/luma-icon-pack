# Luma — Android Icon Pack (Phase 3, PROTOTYPE)

> **PROTOTYPE — not release-ready, not published.** Mapping verification is
> partial (see below) and no launcher has been tested yet (Phase 4).
> Do not publish, list, or ship from this project. Nothing here has been
> uploaded to Google Play and no money has been spent.

## Project layout

```
app/src/main/            dashboard code + manifest + launcher icon + metaPhasic logo
app/src/<flavor>/res/    per-flavor drawables + appfilter.xml + mask assets + app name
  drawable-mdpi|hdpi|xhdpi|xxhdpi|xxxhdpi   315 icons at 48/72/96/144/192 px
  xml/appfilter.xml      252 entries, identical in all flavors (198 component-verified + 36 community-reported drawables; 81 unmapped)
tools/generate_project.py  generator (single source of truth for this project —
                         re-run it to regenerate drawables/appfilter/dashboard catalog;
                         do not hand-edit generated appfilter.xml or IconCatalog.java)
```

Regenerate with:

```bash
cd ~/workspace/goals/luma-android-icon-pack/android-project
python3 tools/generate_project.py
```

## Identity and SDK levels

- Working `applicationId` (base): `com.metaphasicgroup.luma`. It can change
  at release. Flavor APKs use suffixes (see § The 4-set delivery decision):
  `com.metaphasicgroup.luma.regular`, `.dark`, `.gradient`, `.glyph`.
- `compileSdk 35`, `targetSdk 35`, `minSdk 26` (Android 8.0).
  - Why target 35: current AGP 8.5.2 / Gradle 8.7 toolchain, and Play's
    target-API expectations for a new listing. No API-35-only feature is
    used, so lowering target later is cheap.
  - Why min 26: covers every launcher on the target list in practice
    (all of them require Android 8+ for current versions) while avoiding
    pre-26 legacy-icon and vector-compat work. If Phase 4 finds a target
    launcher needing older support, minSdk is a one-line change — but
    re-verify the icon PNG pipeline, which is density-based and unaffected.
- Java 17, Android Gradle Plugin 8.5.2, Gradle 8.7, AndroidX AppCompat
  1.7.0 + RecyclerView 1.3.2. No Kotlin, no dashboard library.

## Build

Prerequisites (this VM's toolchain lives in `toolchain/` and is gitignored;
on another machine, install JDK 17 + Android SDK with `platforms;android-35`
and `build-tools;35.0.0` and point `local.properties` at it):

```bash
./gradlew assembleDebug          # builds all 4 flavor APKs (debug)
./gradlew assembleRegularDebug   # one flavor, e.g. Regular
./gradlew assembleRelease        # unsigned release (no signing config yet)
```

APKs land in `app/build/outputs/apk/<flavor>/debug/`. Release signing is
deliberately not configured: no keystore exists and none may be created
or uploaded without the release decision.

### Build evidence (first successful build, 2026-10-26, PROTOTYPE)

Command (from `android-project/`, after the toolchain fixes below):

```bash
export JAVA_HOME=$(ls -d $PWD/toolchain/jdk-17*/ | head -1)
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
./gradlew assembleDebug --no-daemon -Dorg.gradle.java.home="$JAVA_HOME"
```

Gradle output tail, verbatim (clean rebuild from the regenerated tree,
after `tools/generate_project.py` was re-run to prove the generator
reproduces the project):

```
> Task :app:createRegularDebugApkListingFileRedirect
> Task :app:assembleRegularDebug
> Task :app:assembleDebug

BUILD SUCCESSFUL in 1m 31s
137 actionable tasks: 133 executed, 4 up-to-date
```

(The first successful build, 2026-10-26 02:57, reported `BUILD
SUCCESSFUL in 2m 47s / 136 actionable tasks: 117 executed, 19
up-to-date`; the clean rebuild above is the current evidence.)

APKs produced (debug, unsigned, version `1` / `0.1.0-prototype`):

| Flavor | APK | Size (bytes) | Package |
|---|---|---|---|
| Regular | `app/build/outputs/apk/regular/debug/app-regular-debug.apk` | 9,569,070 | `com.metaphasicgroup.luma.regular` |
| Dark | `app/build/outputs/apk/dark/debug/app-dark-debug.apk` | 10,124,002 | `com.metaphasicgroup.luma.dark` |
| Gradient | `app/build/outputs/apk/gradient/debug/app-gradient-debug.apk` | 12,455,066 | `com.metaphasicgroup.luma.gradient` |
| Glyph | `app/build/outputs/apk/glyph/debug/app-glyph-debug.apk` | 6,912,588 | `com.metaphasicgroup.luma.glyph` |

APK contents verified with `aapt2 dump badging` / `aapt2 dump xmltree
--file res/xml/appfilter.xml` / `unzip -l`: every APK carries
`appfilter.xml` with exactly 252 `item` entries (matching the source
`appfilter.xml`); Regular/Dark/Gradient carry `iconback` + `iconmask` +
`iconupon` + `scale`, while Glyph carries `iconback` + `scale` only —
no `iconmask`, no `iconupon` (see § Mask / backplate behaviour). PNG
entries per APK: 1,794 (tiled flavors) / 1,790 (Glyph; the 4 fewer are
exactly `iconmask` + `iconupon` at mdpi + xxxhdpi). Application labels
are "Luma Regular" / "Luma Dark" / "Luma Gradient" / "Luma Glyph".

Build fixes that had to land before this build succeeded (all in the
project or its toolchain notes, so a re-run is reproducible):
- `settings.gradle`: `gradlePluginPortal()` removed from
  `pluginManagement` — from this sandbox, Java HTTPS to
  `plugins.gradle.org` hangs (read timeout) while Google Maven works,
  and the AGP plugin marker is published on Google Maven.
- `app/build.gradle`: `buildToolsVersion "35.0.0"` pinned (AGP 8.5.2
  otherwise defaults to 34.0.0, which is not installed here) and the
  standard SDK license hashes written to
  `toolchain/sdk/licenses/android-sdk-license` (the `sdkmanager
  --licenses` route fails in this sandbox for the proxy reason above).
- `app/build.gradle`: legacy `kotlin-stdlib-jdk7` / `kotlin-stdlib-jdk8`
  1.6.21 excluded — an older transitive edge pulled them alongside
  kotlin-stdlib 1.8.22 (via AppCompat), causing a duplicate-class
  failure at `:app:checkDarkDebugDuplicateClasses`. stdlib 1.8.22
  already contains those classes, so the exclusion is the standard fix.
- Stale Gradle daemon: a daemon started before the Hatch CA import kept
  failing TLS with its cached SSL context; killing it by PID and letting
  a fresh daemon start fixed plugin resolution. Never kill daemons by a
  pattern that matches your own shell command line (an earlier
  `pkill -f "GradleDaemon"` killed the agent's own shell).

Environment note (this VM, 2026-10-25): the Gradle daemon fails with
"Could not receive a message from the daemon (Unexpected type tag 109)"
unless IPv4 is forced, because `localhost` resolves to `::1` first while
the daemon listens on IPv4. The fix is in `gradle.properties`
(`-Djava.net.preferIPv4Stack=true` in `org.gradle.jvmargs`) and, for the
Gradle client JVM itself, `JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"`.
The heap is also capped at 512 MB in `gradle.properties` because this VM
has little free RAM; on a developer machine, raising `-Xmx` is fine.
Also note: `apt-get` install of a JDK was blocked pending user
confirmation in this sandbox, so the toolchain here is a Temurin 17
tarball + SDK packages downloaded directly (curl via the no-auth egress
proxy) into `toolchain/`; `sdkmanager` itself fails in this sandbox
because its Java HTTP stack chokes on the authenticated proxy, which is
why SDK packages were fetched with curl instead. Two further sandbox
fixes were needed for Gradle dependency downloads, both recorded here
so a future run is not a mystery: (1) the no-auth egress proxy is in
the user-level `~/.gradle/gradle.properties` (`systemProp.http(s)
.proxyHost/Port`, no credentials — the proxy host/port only);
(2) the sandbox egress proxy MITMs TLS with "Hatch Sandbox Egress CA",
so that CA (extracted from `/run/hatch/egress-tls/ca-bundle.pem`) was
imported into the toolchain JDK's `cacerts` with `keytool` — without
it, Java HTTPS fails with `PKIX path building failed` even though
curl works. None of that affects the project itself — on a normal
machine, install the SDK the usual way and none of these workarounds
are needed.

## Drawable naming rule

Icon-index names are lowercase hyphenated (`google-play-games`). Android
resource names must be lowercase with underscores, so the rule is a pure
mechanical substitution: **hyphen → underscore** (`google_play_games`).
All 315 names already match `[a-z][a-z0-9-]*`, so no renames, no prefixes,
and no collisions were needed. The same drawable name is used in every
flavor; the flavor source set supplies that flavor's PNGs. The mapping
between index name, drawable name, package(s) and status is in
`../hidden_files/phase3-mapping-table.csv` (generated, not hand-maintained).

## Mappings (appfilter.xml) — what is in, what is deliberately out

- **In: 252 `<item>` entries, 239 mapped packages, 234 mapped drawables,**
  identical in all flavors. **198 drawables are component-verified**
  (203 packages): each activity was found in the Lawnicons `develop`
  `appfilter.xml` or Arcticons `main` `appfilter.xml` (community
  appfilter databases) under an app name matching Luma's app, and is a
  launcher-named activity (Main/Home/Launcher/Splash/MapsActivity/
  CameraActivity/PeopleActivity/ConversationList/Dialtacts/Calculator/
  GalleryActivity/ClockPackage/Settings/Dashboard/MDActivity/StartActivity/
  App, etc.). **36 drawables are community-reported**: plausible
  first-listed picks from the same databases, included in `appfilter.xml`
  but labeled community-reported in the mapping table — never verified,
  pending a device-side check. Archive-reactivate aliases, alternate-icon
  aliases (`Icon0`, `*Alias`, `launcher.*` icon variants), sub-activities
  (Compose, Search, CallLog, Settings sub-screens) and cross-app junk
  entries were excluded. Sources: Lawnicons `develop` `appfilter.xml`
  commit 2504a58 (2026-10-23); Arcticons `main` `appfilter.xml` commit
  eb38671 (Sep 19, 2026); both accessed 2026-10-25. Per-batch notes are
  in the mapping table CSV.
- **Out (NOT in appfilter.xml): 81 drawables.**
  - 73 packages were found in NEITHER community database (Lawnicons
    `develop` + Arcticons `main`, checked 2026-10-25): including TikTok
    Pro (`com.ss.android.ugc.tiktok.pro`, absent) and Settings Home
    (`com.eet.settings.launcher`, absent — a third-party launcher, NOT
    system Settings, never substituted). Their package IDs are verified
    (chart/system sources), their icons exist in all 4 sets, but their
    launcher activities are unknown to this build.
  - 8 packages are HELD OUT despite a database entry, because the entry
    is not credible as a main activity: Zoom (`us.zoom.videomeetings`,
    only entry `com.zipow.videobox.LauncherActivity`, foreign namespace
    — Phase 3 exclusion upheld), two Bible apps with alias-only
    `IconDefault` entries (`com.offline.bible`,
    `com.bible.holy.bible.for.women`), and five oddity-only entries
    (BlankActivity, OnboardingActivity, UserGuideActivity, LoginActivity,
    Loading). Guessing or trusting these would silently ship wrong
    mappings, so all 81 await a device-side PackageManager query
    (`getLaunchIntentForPackage` / `dumpsys`) on a device with the app
    installed.
- **Excluded entirely (not package-only, not mapped):** the 4
  not-shown chart ranks (28/111/119/133 — no icons exist, never created)
  and the 6 verified=false Samsung system IDs from
  `chart/system-package-ids.csv` (superseded by verified corrected IDs;
  see the tracker). Collision warnings honoured: rank 9 Settings Home is
  kept separate from system Settings; DoorDash consumer
  (`com.dd.doordash`) is separate from Dasher (`com.doordash.driverapp`,
  its own icon, now component-verified via Lawnicons); Emoji apps 177 vs 180 are
  distinct icons and were not merged.
- **Consequence for testing:** a Phase 4 tester will see Luma icons for
  the 234 mapped drawables (198 component-verified + 36
  community-reported). The 81 unmapped installed apps fall back to the
  mask/backplate behaviour below (tiled flavors) or to their original
  icon (Glyph flavor). That is the honest prototype state, not a
  coverage claim.

## Mask / backplate behavior (implemented)

| Flavor | iconback | iconmask | iconupon | scale | Unmapped apps |
|---|---|---|---|---|---|
| Regular / Dark / Gradient | squircle tile in a flavor-neutral gradient (Regular light grey, Dark slate, Gradient slate) | squircle mask (white shape on black) | top gloss, squircle-clipped, fading by ~45% height (design-system gloss language) | 1.0 | Original icon composited onto the squircle backplate through the mask, with gloss — coherent with the set, never a broken icon. |
| Glyph | fully transparent PNG (223 bytes) | **none — no element, no file** | **none** | 1.0 | Left as their original icons. A mask/backplate would crop glyph-only marks into a tile and destroy the set's whole point, so Glyph deliberately does not theme unmapped apps. |

Mapped icons are drawn directly from their drawable in all flavors; the
mask/backplate/upon trio is the standard mechanism launchers use for
*unmapped* apps. Caveat for Phase 4: some launchers also apply their own
shape/size settings to all icons (e.g. Nova's icon-shape options). Glyph
must be tested per launcher with those settings at default, and the
dashboard's About text says so. If a launcher is found to force-crop
Glyph marks, that launcher is a Glyph limitation, not something to fix
by adding a tile.

## The 4-set delivery decision — (b) build flavors, one APK per set

**Verified technical fact:** launchers read an icon pack's resources
statically from the installed APK. The standard mechanism is parsing
`res/xml/appfilter.xml` from the pack's resources and loading the named
drawables (Apex theme tutorial: appfilter moved from `assets/` to
`res/xml/` precisely so launchers could read it from paid/encrypted
APKs; launcher implementations such as Taskbar's `IconPack` and
BridgeLauncher's `IconPackCache` parse that XML from the installed
package's resources). An installed APK's compiled resources cannot be
rewritten by its own dashboard at runtime, so **no in-dashboard picker
can change the set a launcher applies**. This build therefore does NOT
ship a picker that only changes the dashboard preview — the Sets tab
says so in plain language and instead routes the user to install/select
a different flavor APK.

**Chosen approach: (b) Gradle build flavors producing one APK per set.**
One codebase, one `appfilter.xml`, one drawable name set; each flavor
source set supplies its own 315 PNGs, its own mask assets and its own
app name. All four APKs can be installed side by side (distinct
`applicationIdSuffix`), and the user switches sets by selecting a
different Luma pack in the launcher's icon-pack setting — exactly how
the market already does light/dark variants (e.g. Hera / Hera Dark and
Ombre / ELEV8 are separate apps).

**Trade-offs, stated plainly:**

- *Purchase implication — this approach breaks "one $2.49 purchase
  unlocks all 4 sets" if published naively.* Four separate paid Play
  listings would cost a user up to 4 × $2.49. There is no Play mechanism
  that lets one paid purchase DRM-unlock four separate icon-pack APKs;
  a free companion APK's drawables are readable by any launcher whether
  or not its dashboard gates anything, so a "free variants, paid base"
  scheme would be honor-system only, not a real unlock. The honest
  release options are: (1) four listings (breaks the one-purchase
  ideal — say so in the listing copy, or price accordingly);
  (2) one listing, one set (breaks the four-set promise); or
  (3) one paid listing plus companion listings whose copy is explicit
  that they are separate purchases / free extras. **The coordinator and
  the user must choose before release; this prototype chooses neither
  and publishes nothing.**
- *Why not (a), a single app with a runtime variant mechanism?* No such
  mechanism was verified for standard launchers. Dynamic-feature modules
  were considered and rejected as unverified: a launcher reading the
  base package's `appfilter` resource has no defined way to see a
  feature module's replacement `appfilter`, so claiming it would be
  exactly the fake-picker problem in a new costume.
- *Why not (c), separate listings as the build model?* The flavor build
  already produces the same four APKs that separate listings would need,
  while keeping one codebase and one mapping table. Listing strategy is
  a release decision, not a build decision, and the flavor model keeps
  both options open.
- *Switching cost for the user:* installing another flavor and
  re-selecting it in the launcher (seconds, no data loss — dashboard
  state is trivial). The Sets tab remembers which pack the Apply tab
  targets, including installed siblings.

## Luma's own launcher icon

Original art generated in `tools/generate_project.py` (`write_app_icon`):
a deep-slate squircle tile with a white four-point spark (light) mark and
a small icy-blue centre dot. It is not a brand glyph, not the metaPhasic
mark, and not distorted from any logo. Adaptive icon (`mipmap-anydpi-v26`)
uses a `#141821` background and the spark tile as foreground; legacy
mipmap PNGs are generated at all densities.

## Dashboard (original code, no dashboard library)

`app/src/main/java/com/metaphasicgroup/luma/` — `MainActivity`,
`IconAdapter`, `LauncherApply`, and generated `IconCatalog`. No
CandyBar, IconShowcase or other dashboard code is used, so no OSS
attribution is owed for the dashboard itself. (CandyBar's
`LauncherHelper` was used only as a *research source* for intent
strings, cited below — no code was copied from it.)

- **Header/branding:** the supplied metaPhasic Group logo JPG (dark
  background, teal geometric mark + wordmark) is copied unmodified into
  `drawable-nodpi/metaphasic_logo.jpg` and shown `fitCenter` on a dark
  header so it is never cropped or distorted. It is used for dashboard/
  about branding only, never as an app icon glyph.
- **Icons tab:** searchable grid of all 315 icons in the installed flavor.
- **Apply tab:** per-launcher buttons (installed launchers first by
  state label), each with a manual-path fallback dialog.
- **Sets tab:** honest switching explanation + sibling-flavor buttons
  (see § Delivery decision).
- **Request tab:** app name / package / notes form producing request
  text the user copies or shares themselves. Luma sends nothing on its
  own and no recipient address is invented — the share sheet lets the
  user choose where it goes.
- **About tab:** metaPhasic branding, non-affiliation disclaimer,
  Samsung Theme Park note, Pixel limitation note, Glyph mask note.

## Apply intents + sources

Only intents actually sourced in Phase 3 are coded as direct routes.
Everything else is a settings-launch or a documented manual path, and
the dashboard always shows the manual fallback alongside any direct
attempt. All routes are **untested against Luma** until Phase 4.

| Launcher | Package | Coded route | Source |
|---|---|---|---|
| Nova Launcher | `com.teslacoilsw.launcher` | Intent action `com.teslacoilsw.launcher.APPLY_ICON_THEME`, addressed to the Nova package. No extra payload is coded: no extra name was verified in Phase 3 sources, and inventing one would be guessing. If Nova's prompt does not pre-select Luma, the manual path applies. | CandyBar `LauncherHelper` summary (DeepWiki, "Icon Pack Application System"); Nova manifest listing `IconThemePrompt` handling `APPLY_ICON_THEME` |
| Action Launcher | `com.actionlauncher.playstore` | Launch intent + `apply_icon_pack` extra (value = Luma pack package) | CandyBar summary: "launch intent with `apply_icon_pack` extra" |
| ADW Launcher 2 | `org.adw.launcher` | Broadcast `org.adw.launcher.SET_THEME` | CandyBar summary/troubleshooting table (broadcast `SET_THEME`); package from Smart Launcher's compatible-launcher list |
| Apex Launcher | `com.anddoes.launcher` | Broadcast `com.anddoes.launcher.SET_THEME` | CandyBar summary |
| Lawnchair | `ch.deletescape.lawnchair.plah` | Manual only. (CandyBar mentions an `APPLY_ICONS` intent, but its full action string was not verified, so it is NOT coded.) | Manual path from Lawnicons: Home Settings → General → Icon Style; package from Smart Launcher list |
| Niagara Launcher | `bitpit.launcher` | Manual only | Package from Smart Launcher list; exact settings path to confirm in Phase 4 |
| Smart Launcher | `ginlemon.flowerfree` | Manual only | Package from Smart Launcher list; exact settings path to confirm in Phase 4 |
| Hyperion Launcher | `projekt.launcher` | Open launcher settings (launch intent) + manual path | CandyBar classifies Hyperion as "Settings Launch"; package from Smart Launcher list |
| Poco Launcher | `com.mi.android.globallauncher` | Open launcher settings (launch intent) + manual path | CandyBar classifies Poco as "Settings activity launch"; package from Smart Launcher list |
| Microsoft Launcher | `com.microsoft.launcher` | Manual only | Package from Smart Launcher list + AppBrain; manual path (Settings → Personalization → Change icon pack) from a public apply guide |
| Samsung One UI Home | (Good Lock / Theme Park, Galaxy Store) | Manual only, in dashboard + About | Phase 3 launcher-compatibility plan (documented-but-untested): Good Lock → Theme Park → Icon → Create New → Iconpack → select installed pack → save/apply. Requires One UI 4+; Good Lock availability varies by region. |

Stock Pixel launcher: no apply path exists (platform limitation, not a
Luma defect) — stated in the dashboard, not coded as a route.

## Known gaps (for Phase 4)

1. **Mapping coverage:** 234/315 drawables are mapped (198
   component-verified + 36 community-reported, 252 appfilter entries).
   81 drawables remain unmapped (8 held out + 73 found in neither
   community database) and will not theme until their activities are
   verified device-side. The mapping table CSV is the work list; do not
   hand-add guessed components to `appfilter.xml`.
2. **Launcher testing:** nothing has been tested on any launcher or
   device. In particular: Nova's extra-less apply intent, ADW/Apex
   broadcast behaviour, Lawnchair/Niagara/Smart/Hyperion/Poco/Microsoft
   exact settings paths, Glyph under each launcher's shape settings,
   and the Samsung Theme Park route all need empirical passes. The
   Phase 4 starting point is the four debug APKs in § Build evidence
   (install a flavor APK, then select that Luma pack in the launcher's
   icon-pack setting, or use the dashboard Apply tab).
3. **Variant switching on Play:** unresolved purchase/listing decision
   (see § Delivery decision). Do not promise "one purchase, all sets"
   in any copy until it is chosen and is true.
4. **Release signing:** no keystore, no signing config, no Play listing.
5. **Icon request delivery:** the dashboard produces request text but
   has no destination address by design (none was supplied). The
   coordinator must supply a real support/request address before
   release, or keep the user-shared flow.
6. **Artwork:** all icons remain PROTOTYPE per the artwork tracker
   (48 px watch items, Glyph dark-wallpaper visibility) — no redesign
   was done in Phase 3.
7. **Dynamic calendars / clocks:** not implemented (no `<calendar>` or
   clock handling). The `calendar` drawable maps statically like any
   other icon in this build.

## Phase 5 rebuild (2026-10-26, PROTOTYPE — still not release-ready)

Two pre-release fixes, then a full regeneration + rebuild of all 4
flavors with the § Build command above (`BUILD SUCCESSFUL in 1m 4s`,
136 actionable tasks: 37 executed, 99 up-to-date). Nothing was
published, no keystore was created, and no mapping was changed.

- **Glyph X colour:** `design/svg/glyph/x.svg` stroke changed
  `#0F172A` → `#FFFFFF` (white). Shape untouched: same two diagonal
  paths, same stroke-width 40, same transform, no fill change, no
  outline, no tile. The design generator's source of truth for this
  glyph (`GLYPH_COLORS["x"]` in `design/generate_icons.py`) was changed
  to `("#FFFFFF", "#FFFFFF")` at the same time, so re-running the
  generator reproduces the white X instead of clobbering it back to
  slate; that entry affects only the Glyph set's X (tiled sets take
  their glyph colour from `tile_colors`). Full design regeneration
  (`generate_icons.py --png`) changed exactly 7 files — the X SVG, its
  5 density PNGs and the flat 192 px PNG — verified by md5 diff
  against a pre-run snapshot; no other glyph in any set changed. The
  project generator then copied the new PNGs into the Glyph flavor
  drawables (byte-identical to `design/png/glyph/`).
- **Lawnchair packages:** fixed at the generator layer in
  `tools/generate_project.py` (both `LauncherApply.java` and
  `AndroidManifest.xml` are generated outputs of that script, so
  editing the generated files directly would be clobbered). The
  Lawnchair entry now lists `ch.deletescape.lawnchair.plah` (legacy,
  kept) + `app.lawnchair` + `app.lawnchair.play`, in both the
  dashboard's package list (`LauncherApply.java` line 42) and the
  manifest `<queries>`. Verified in the compiled Glyph APK manifest
  via `aapt2 dump xmltree` (all three packages present).
- **Re-verified on the new APKs:** `aapt2 dump xmltree` appfilter
  counts are still exactly 66 `item` entries in every flavor
  (mappings unchanged); Glyph still carries `iconback` + `scale` only
  — no `iconmask`, no `iconupon` (Regular control APK still has its
  4 mask/upon entries); every opaque pixel of the Glyph X PNG at all
  5 densities is pure white `(255, 255, 255)` with fully transparent
  corners, sampled from the drawables extracted from the Glyph APK
  (and identically from `design/png/glyph/`).

New APKs (debug, unsigned, version `1` / `0.1.0-prototype`):

| Flavor | APK | Size (bytes) | Package |
|---|---|---|---|
| Regular | `app/build/outputs/apk/regular/debug/app-regular-debug.apk` | 9,582,710 | `com.metaphasicgroup.luma.regular` |
| Dark | `app/build/outputs/apk/dark/debug/app-dark-debug.apk` | 10,137,625 | `com.metaphasicgroup.luma.dark` |
| Gradient | `app/build/outputs/apk/gradient/debug/app-gradient-debug.apk` | 12,468,708 | `com.metaphasicgroup.luma.gradient` |
| Glyph | `app/build/outputs/apk/glyph/debug/app-glyph-debug.apk` | 6,932,186 | `com.metaphasicgroup.luma.glyph` |

(Sizes are slightly up on Phase 3 from the longer manifest queries
and the re-rasterised X PNGs.)

## Phase 6 integration (2026-10-26, PROTOTYPE — still not release-ready)

Wallpapers, widgets, and the dynamic calendar are now wired into the
app. Everything below is generated by `tools/generate_project.py`
(the single source of truth — re-running it reproduces Phase 6);
no generated file was hand-edited. Nothing was published, no
keystore was created, and the mapping was not changed.

### Features

- **Wallpapers (10):** `design/wallpapers/wallpaper_01.png` …
  `wallpaper_10.png` (1080×1920 each; 3 light / 3 dark / 4 mid-tone)
  are copied into `app/src/main/res/drawable-nodpi/` (shared across
  flavors). The dashboard gains a **Wallpapers** tab (preview grid +
  "Set wallpaper" per wallpaper) using `WallpaperManager.setResource`
  with `FLAG_SYSTEM | FLAG_LOCK` (home + lock screen; minSdk 26
  supports both). `SET_WALLPAPER` (a normal permission, no runtime
  request) is declared in the generated manifest.
- **Widgets (2):** Clock (~4×2 cells, 250×110dp; `TextClock` time +
  date; tap → `AlarmClock.ACTION_SHOW_ALARMS`, no permission needed)
  and Search (~4×1, 250×40dp; tappable bar, no `EditText` —
  RemoteViews cannot host one; tap → `ACTION_WEB_SEARCH` with no
  query extra). Providers + `res/xml` provider-info + RemoteViews-safe
  layouts + bar background + previews are copied from
  `android-project/phase6-staging/` into the main source set, and
  both receivers are declared in the manifest with
  `android:exported="true"` + `APPWIDGET_UPDATE` + `android.appwidget.provider`
  meta-data. Add widgets from the launcher's widget picker.
- **Dynamic calendar (Samsung Calendar only):** `calendar_1` …
  `calendar_31` (no zero padding) at all 5 densities per flavor,
  rendered by the staging script `phase6-staging/tools/generate_calendar_days.py`
  from each flavor's own `design/png/<set>/<density>/calendar.png`
  (white bold numbers on tiled sets; `#059669` on tile-free Glyph,
  no tile added). Each flavor's `appfilter.xml` gains two
  `<calendar component="ComponentInfo{com.samsung.android.calendar/...}"
  prefix="calendar_" />` elements (trailing underscore in the prefix;
  the launcher appends the day integer → `calendar_25`).

### Mechanism notes (binding)

- **Dynamic calendar is a launcher convention, not an AOSP API.**
  No platform contract defines `<calendar>` in `appfilter.xml`;
  launchers that support the convention resolve the day drawable,
  others ignore the element. Listing copy therefore says only
  "where the launcher supports dynamic calendar icons" — no
  launcher is named as supported until tested.
- **Fallback rule (must not be broken):** the two existing
  `<item drawable="calendar">` entries for Samsung Calendar are
  KEPT alongside the `<calendar>` elements. A launcher without
  dynamic-calendar support ignores the unknown element and themes
  the calendar with the static icon; replacing the `<item>` entries
  would leave it unthemed.
- **Scope:** Samsung Calendar only — the only calendar components
  verified in Luma's mapping (`com.android.calendar.AllInOneActivity`,
  `com.samsung.android.app.calendar.activity.MainActivity`). Google /
  AOSP / vendor calendars are OUT until their activities are
  verified; no activity string was guessed. Full research record:
  `phase6-staging/CALENDAR_SPEC.md`.
- **Widgets + dynamic calendar + wallpapers are all UNTESTED on any
  device/launcher** (see Known gaps). The Theme Park route remains
  documented-not-tested; Theme Park applies icons statically, so
  dynamic-calendar behaviour must not be promised for it.

### Build evidence (Phase 6 rebuild, 2026-10-26)

Command (from `android-project/`, same toolchain pattern as § Build):

```bash
export JAVA_HOME=$(ls -d $PWD/toolchain/jdk-17*/ | head -1)
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
./gradlew assembleDebug --no-daemon -Dorg.gradle.java.home="$JAVA_HOME"
```

Gradle output tail, verbatim:

```
> Task :app:assembleRegularDebug
> Task :app:assembleDebug

BUILD SUCCESSFUL in 1m 44s
136 actionable tasks: 52 executed, 84 up-to-date
```

Generator output (same run): 315 icons, 252 appfilter entries per
flavor, 6300 PNGs copied, 620 calendar day drawables, 10 wallpapers,
10 widget files wired.

New APKs (debug, unsigned, version `1` / `0.1.0-prototype`):

| Flavor | APK | Size (bytes) | Package |
|---|---|---|---|
| Regular | `app/build/outputs/apk/regular/debug/app-regular-debug.apk` | 11,325,023 | `com.metaphasicgroup.luma.regular` |
| Dark | `app/build/outputs/apk/dark/debug/app-dark-debug.apk` | 11,901,119 | `com.metaphasicgroup.luma.dark` |
| Gradient | `app/build/outputs/apk/gradient/debug/app-gradient-debug.apk` | 14,505,959 | `com.metaphasicgroup.luma.gradient` |
| Glyph | `app/build/outputs/apk/glyph/debug/app-glyph-debug.apk` | 8,346,845 | `com.metaphasicgroup.luma.glyph` |

Re-verified on the new APKs (`aapt2 dump xmltree/badging`,
`unzip -l`, pixel checks on Glyph APK drawables): appfilter is
**252 `item` entries + 2 `calendar` elements per flavor** (the
"66 items" figure in the Phase 4/5 notes is the pre-merge count and
does not describe this build); static calendar `<item>` entries
kept; **155 calendar day files per APK** (31 × 5 densities, no
zero-padded names); **10 wallpapers per APK**; both widget receivers
present with `exported="true"` in every manifest, plus
`SET_WALLPAPER`; Glyph X still pure white at all 5 densities
(0 non-white opaque pixels); Lawnchair `app.lawnchair` +
`app.lawnchair.play` present with legacy `plah` retained; labels
and packages unchanged. Mapping unchanged: 234/315 drawables
mapped (198 component-verified + 36 community-reported), 81
unmapped pending a device query.

## Phase 7 integration (2026-10-26, PROTOTYPE — still not release-ready)

Three new flavors — Clear, Prism, Midnight — join the four existing
ones (seven flavors total, seven separate listings at $2.49 each; a
buyer wanting all seven pays 7 × $2.49 = $17.43). Everything below is
generated by `tools/generate_project.py` (extended, still the single
source of truth — re-running it with `--flavors clear prism midnight`
reproduces the new flavors; re-running it with no `--flavors` flag
regenerates all seven). No generated file was hand-edited. Nothing
was published, no keystore was created, and the mapping was not
changed. Titles are the user-confirmed em-dash forms (Oct 25, 2026;
see `../files/phase7-titles-config.md`): "Luma — Clear",
"Luma — Prism", "Luma — Midnight".

### New flavors

| Flavor | Package | App label | Treatment | Mask behavior |
|---|---|---|---|---|
| Clear | `com.metaphasicgroup.luma.clear` | Luma Clear | Gradient glyphs, no tile — NEW treatment | Transparent iconback, NO iconmask, NO iconupon, scale 1.0 (Glyph-like). Unmapped apps stay as their original icons. |
| Prism | `com.metaphasicgroup.luma.prism` | Luma Prism | Gradient tiles + white glyphs — shares treatment with Gradient | iconback + iconmask + iconupon (Gradient gradient), scale 1.0. Unmapped apps composite onto the backplate. |
| Midnight | `com.metaphasicgroup.luma.midnight` | Luma Midnight | Dark slate tiles + light glyphs — shares treatment with Dark | iconback + iconmask + iconupon (Dark gradient), scale 1.0. Unmapped apps composite onto the backplate. |

Prism and Midnight make NO new-treatment claim: their listings sell
the user's own motif/palette execution (see
`../files/play-listing-prism.md` and
`../files/play-listing-midnight.md`, whose honest-differentiation
disclosures were kept through the title normalization). Clear's
Settings icon is now an 8-tooth gear in the Clear gradient (user-approved
swap, Oct 25, 2026 — the sheet's dial read as a target at 48 px; the
gear was verified readable at 48 px and rebuilt into the Clear APK).
Midnight's Settings-vs-target ring rhyme was ACCEPTED as-is (both
glyphs faithful to their sources; neither was altered).

### Parity (extras)

- **Wallpapers (10 per new flavor):** from
  `design/wallpapers-phase7/<flavor>/` (1080 × 1920 each; Clear and
  Midnight documented 3 light / 3 dark / 4 mid-tone), wired as
  per-flavor `drawable-nodpi` overrides in the flavor source set.
  Flavor resources override the shared main set, so a new flavor's
  Wallpapers tab shows its own set. The existing four flavors keep
  the shared Phase 6 set — Phase 7 wallpapers were NOT injected into
  them (verified by hash: each new APK's `wallpaper_01.png` matches
  its Phase 7 source, not the Phase 6 shared file).
- **Widgets (2):** shared Clock + Search providers carry over — no
  per-flavor widget art was written, so the new flavors use the
  shared widget appearance. Both receivers are declared in the new
  manifests (`exported="true"` + `APPWIDGET_UPDATE`).
- **Dynamic calendar (Samsung Calendar only):** same mechanism as
  Phase 6 — 2 `<calendar prefix="calendar_">` elements per flavor,
  static `<item>` fallback kept, 155 day drawables per flavor
  (31 × 5 densities, no zero padding) rendered from each flavor's own
  `calendar.png`. Day-number colors: Clear `#7C3AED` (its calendar's
  deep stop, verified in `design/png/clear/192px/calendar.png`);
  Prism/Midnight white (tiled-set convention). **Prism finding:**
  Prism's calendar page body is pure white, so its white day number
  is nearly invisible (only ~313 changed pixels on day 25 at
  xxxhdpi, vs ~1,600–1,700 for Gradient/Dark/Midnight/Clear). This
  is flagged as an open decision in
  `../files/release-instructions-phase7.md` (deep-stop number is the
  Prism-consistent alternative); it was NOT silently redesigned.
  Midnight's white number on its pale page is visible (1,700 changed
  pixels) but lower-contrast — a watch item, not a fail.
- **Dashboard:** the Sets tab now lists all seven flavors and the
  manifest `<queries>` include all seven packages, so any flavor's
  dashboard can target an installed sibling. The Wallpapers tab note
  is flavor-aware: Prism's note states no tone split (its split is
  not documented); all other flavors keep the Phase 6 wording.

### Protection of the existing four flavors

The Phase 7 run used `tools/generate_project.py --flavors clear prism
midnight`, so only the three new flavor source dirs were written.
Before regenerating, the four existing flavor dirs were snapshotted
(6,948 files, sha256 manifest); after the run the manifest diff was
EMPTY — the existing four flavor dirs are byte-identical. The
mapping table CSV hash is also unchanged
(`ae86d62e4b92209aaef405dd4a90c7b92d20b0624b9c5833d43d878fc93e8ec2`).
Shared main code (dashboard, manifest, gradle flavor list) was
updated by design — it is one codebase for all flavors — but no
existing flavor's artwork, appfilter, listing, or APK was modified.
The existing four APKs were NOT rebuilt: their build-output sizes
still match the Phase 6 evidence exactly (Regular 11,325,023; Dark
11,901,119; Gradient 14,505,959; Glyph 8,346,845).

### Build evidence (Phase 7, 2026-10-26)

Command (from `android-project/`, same toolchain pattern as § Build):

```bash
export JAVA_HOME=$(ls -d $PWD/toolchain/jdk-17*/ | head -1)
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
./gradlew assembleClearDebug assemblePrismDebug assembleMidnightDebug --no-daemon -Dorg.gradle.java.home="$JAVA_HOME"
```

Gradle output tail, verbatim:

```
> Task :app:assemblePrismDebug
> Task :app:assembleMidnightDebug

BUILD SUCCESSFUL in 1m 52s
102 actionable tasks: 102 executed
```

Generator output (same run): 315 icons, 252 appfilter entries per
flavor, 4,725 PNGs copied (315 × 5 × 3 flavors), 465 calendar-day
drawables (31 × 5 × 3), 30 per-flavor wallpapers (10 × 3), mapping
counts 198 component-verified + 36 community-reported drawables.

New APKs (debug, unsigned, version `1` / `0.1.0-prototype`):

| Flavor | APK | Size (bytes) | Package |
|---|---|---|---|
| Clear | `app/build/outputs/apk/clear/debug/app-clear-debug.apk` | 10,376,641 | `com.metaphasicgroup.luma.clear` |
| Prism | `app/build/outputs/apk/prism/debug/app-prism-debug.apk` | 16,337,891 | `com.metaphasicgroup.luma.prism` |
| Midnight | `app/build/outputs/apk/midnight/debug/app-midnight-debug.apk` | 12,445,591 | `com.metaphasicgroup.luma.midnight` |

Re-verified on the new APKs (`aapt2 dump badging`, `aapt2 dump
xmltree --file res/xml/appfilter.xml` / `--file AndroidManifest.xml`,
`unzip -l`, pixel checks): appfilter is **252 `item` entries + 2
`calendar` elements per flavor**; static calendar `<item>` entries
kept; **155 calendar-day files per APK** (31 × 5 densities, no
zero-padded names); **10 wallpapers per APK** (per-flavor sets, see
above); both widget receivers present with `exported="true"` +
`APPWIDGET_UPDATE` in every manifest, plus `SET_WALLPAPER`; Lawnchair
`app.lawnchair` + `app.lawnchair.play` present with legacy `plah`
retained; labels and packages as in the table above; Clear carries
iconback + scale only — no iconmask, no iconupon — with a fully
transparent iconback, while Prism/Midnight carry
iconback+mask+upon. Mapping unchanged: 234/315 drawables mapped
(198 component-verified + 36 community-reported), 81 unmapped
pending a device query. Widgets, wallpapers, and dynamic calendar
are all UNTESTED on any device/launcher for the new flavors (see
Known gaps); the Theme Park route remains documented-not-tested.
