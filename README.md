# SciModeler

A desktop editor for scientific models and conceptual diagrams for theses, papers and presentations. It runs on **Windows** and **macOS**.

Type your content once (a title, a central concept, and items with description points), then switch between layouts with one click:

| Layout | Good for |
|---|---|
| **Wheel** | Factors around a central concept (e.g. an interprofessional collaboration model) |
| **Cycle** | Repeating processes (PDCA, the nursing process) |
| **Hub & spoke** | Determinants linked to a core concept |
| **Concentric** | Nested levels (socio-ecological model) |
| **Process** | Sequential steps (research design) |
| **Pyramid** | Hierarchies |
| **Venn** | 2–4 overlapping concepts |
| **Matrix** | 2×2 or grid categorisation |
| **Layers** | Stacked levels with descriptions |

Features:
- 8 colour themes, including a greyscale print theme; you can also set a custom colour per item
- Your choice of font and text size, rotation for the circular layouts, and toggles for shadows and gradients
- Export to a **PNG** sized for your document (width in cm plus 150/300/600 dpi, with DPI embedded so Word inserts it at the right size)
- Export to an **SVG** vector file, or copy the image to the clipboard
- Save and reopen editable `.scimodel` files, with full undo/redo

## Download

Every push to `main` builds the installers on GitHub Actions:
**Actions → latest "Build installers" run → Artifacts** (`SciModeler-Windows` / `SciModeler-macOS`).

To publish a proper release, push a tag such as `v1.0.0`. The installers will then be attached to a GitHub Release.

### First launch on macOS
The app isn't signed with an Apple Developer certificate, so macOS blocks it the first time.
After dragging SciModeler into Applications, run this once in Terminal:

```
xattr -cr /Applications/SciModeler.app
```

Alternatively, try to open it, then go to **System Settings → Privacy & Security → Open Anyway**.

### Windows
The `.msi` installs for the current user (no admin rights needed). If SmartScreen shows a warning, click **More info → Run anyway**.

## Build it yourself

You need JDK 17 or newer.

```
./gradlew run                                 # start the app
./gradlew test                                # run tests
./gradlew packageDistributionForCurrentOS     # build .msi (Windows) or .dmg (macOS)
```

Installers end up in `build/compose/binaries/main/`.

## Project structure

```
src/main/kotlin/scimodeler/
  core/      model, layouts, rendering (PNG via Java2D, SVG writer), file format, templates
  ui/        Compose Multiplatform desktop interface
  Main.kt    window, menus and keyboard shortcuts
```

All layouts render into one scene of shapes and text. The on-screen preview, the PNG and the SVG are drawn from that same scene, so what you see is what you export.
