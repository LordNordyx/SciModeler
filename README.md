# SciModeler

A desktop editor for scientific models and conceptual diagrams for theses, papers and presentations. It runs on **Windows** and **macOS**.

Type your content once (a title, a central concept, and items with description points and icons), then switch between **20 model layouts** with one click. Every layout uses the same design language, so figures placed next to each other in a thesis look like one family.

| Category | Models |
|---|---|
| **Circular** | Wheel · Cycle · Hub & spoke · Concentric · Petals · Venn |
| **Flow & process** | Process · Milestones · Card flow · Zigzag · Arc list |
| **Hierarchy & funnels** | Pyramid · Funnel · Cone · Layers · Matrix |
| **Relations & frameworks** | Branches · Fan-out · Framework · Variables |

Features:
- **Icons:** a built-in set of 43 line icons, or any local image (PNG/JPG/GIF/BMP) as a custom icon. Images keep their proportions, the picker previews exactly how the node will look, and "Restore default" returns to the automatic icon.
- **Modular content:** add, remove, duplicate and reorder items freely. In framework models, items can be assigned to groups (input/output/context), and group captions are editable.
- **Styles:** 8 colour themes, including a greyscale print theme, plus custom colours per item, font and text size, rotation, shadows and gradients.
- **Export:** PNG sized for your document (width in cm plus 150/300/600 dpi, embedded DPI), SVG vector, or copy to clipboard.
- **Persistence:** editable `.scimodel` files with full undo/redo. The open model, its custom icons and your settings are restored automatically when you restart.
- **Interface:** frosted-glass floating panels, the Inter typeface, and animated hover, press, focus and selection states. It respects the system's reduced-motion setting, which can be overridden under Settings → Motion.

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
  core/            data model, model registry, layouts, icons, rendering (PNG + SVG), files, persistence
  ui/theme/        design tokens: colours, typography, spacing, radii, motion
  ui/components/   reusable parts: glass panels, buttons, inputs, preview cards, icon picker
  ui/              screens: top bar, model gallery, canvas, inspector, overlays
  Main.kt          window, menus and keyboard shortcuts
```

All layouts render into one scene of shapes, text and images. The on-screen preview, the PNG and the SVG are drawn from that same scene, so what you see is what you export.

### Adding a model

1. Write a layout function in `core/` (e.g. `fun Ctx.timeline(): Size`) using the shared helpers: `iconNode`, `textBlock`, `arrow`, `callout`, `withCallouts`.
2. Add **one entry** to `Models.all` in `core/Models.kt`:
   ```kotlin
   ModelSpec("timeline", "Timeline", "Events along a line", Category.FLOW, usesIcons = true) { timeline() }
   ```
3. Optional: put a thumbnail at `src/main/resources/thumbnails/timeline.png`. Without one, the gallery shows a live preview of the user's own content.

### Tests

`./gradlew test` renders every model with every template and edge case. It also runs an offscreen UI flow (select a model, assign a custom icon, restart, restore the default) and saves screenshots to `build/screenshots/`.
