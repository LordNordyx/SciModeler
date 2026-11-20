# SciModeler

A desktop editor for scientific models and conceptual diagrams for theses, papers and presentations. It runs on **Windows** and **macOS**.

Type your content once (a title, a central concept, and items with description points and icons), then switch between **24 model layouts** with one click, or build your own in the **Freeform Builder**. Every model uses the same design language, so figures placed next to each other in a thesis look like one family.

| Category | Models |
|---|---|
| **My models** | Freeform Builder · your saved models |
| **Circular** | Wheel · Cycle · Hub & spoke · Concentric · Petals · Venn |
| **Flow & process** | Process · Milestones · Card flow · Zigzag · Arc list |
| **Hierarchy & funnels** | Tree · Topic map · Pyramid · Funnel · Cone · Layers · Matrix |
| **Relations & frameworks** | Branches · Fan-out |
| **Research designs** | Framework · Variables · Mediation · Path model (H1, H2 …) |

### Freeform Builder
Build any 2D model from scratch, with the same styling, themes and export as the built-in models:
- **Component library:** shapes, text, connectors, icon nodes, cards and decorations. Click a component to add it, or drag it onto the canvas.
- **Direct manipulation:** move, resize (rotated shapes too), rotate, duplicate and delete elements. Use arrow buttons or arrow keys with an adjustable step.
- **Smart snapping:** grid snapping, alignment guides to other shapes' edges and centres, and automatic positioning of new elements. Hold Alt while dragging to place freely.
- **Connectors:** draw them with the Connect tool. Ends attach to shapes and follow them when moved, with straight, elbow or curved routes, arrowheads at either end, and dashed lines.
- **Editing tools:** multi-selection (Shift-click or drag a box), grouping, layers with lock and hide, align and distribute, numeric position, size and rotation, and undo/redo.
- **Materials:** Theme, Gradient, Solid, Soft, Outline and Glass. Colours come from the theme palette and follow theme changes.
- **Icons and images:** built-in icons and your own images, which keep their proportions.
- **Save and reuse:** save any model to **My models** in the library, then open, duplicate, rename or delete it later.

### Styling
- **Themes are complete presets:** applying one restyles the whole model at once (palette, gradients, shadows, card tints, connectors and background). Undo returns to the previous look.
- **Accurate colour pickers:** every item's swatch shows exactly the colour the model draws. Colours picked from the theme row follow later theme changes; custom colours stay fixed.
- **Other options:** fonts, text size, rotation, shadows and gradients, and path labels.

### Also
- **Icons:** 43 built-in line icons, or any PNG/JPG/GIF/BMP as a custom icon. Images keep their proportions, the picker previews the result live, and "Restore default" switches back.
- **Modular content:** add, remove, duplicate, reorder and nest items. Roles (independent, dependent, mediator, moderator, context) apply in research designs.
- **Export:** PNG sized for your document (width in cm plus 150/300/600 dpi, embedded DPI), SVG vector, or copy to clipboard.
- **Persistence:** editable `.scimodel` files with undo/redo, and automatic restore of your last session, custom icons and settings.
- **Interface:** frosted-glass panels (or solid; this follows the system's transparency setting), animated states, and reduced-motion support.

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

`./gradlew test` renders every model with every template and edge case. It also checks that every colour swatch matches what each model draws, and tests the Freeform editing operations (snapping, resizing rotated shapes, grouping, connectors).

Two offscreen UI flows drive the real interface and save screenshots to `build/screenshots/`:
- **`UiFlowTest`:** select a model, assign a custom icon, restart, and restore the default.
- **`BuilderFlowTest`:** theme colours and presets, the new models, and the Freeform Builder (add, drag with snapping, delete/undo, group, connect, save to My models, restart).
