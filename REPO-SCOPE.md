# Repository scope — Luma icon pack app

This repository is the **canonical code master** for the Luma Android app shell
(metaPhasic Group): Java sources, manifests, layouts, per-flavor appfilter.xml
mappings, and Gradle build files for all 7 flavors.

NOT here, by design:
- Icon PNG assets (12k+ files) — design master is the goal workspace
  `design/png/` tree; the full shippable tree (assets included) is the
  canonical zip in Google Drive, per the MASTER INDEX.
- Signing keystores — never in git, never in Drive. Held separately.
- Build outputs and the local toolchain.

Update path: edit the canonical VM tree, then snapshot-push; diffs between
snapshots are the review surface (fleet diff-first rule). Current shipped
state: v6 (versionCode 6, versionName 1.0.2, targetSdk 36).
