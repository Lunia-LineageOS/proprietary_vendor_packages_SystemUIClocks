# SystemUI Clock Styles

Source-built implementations of Google Pixel lock screen clock plugins for AOSP. These packages provide modular `ClockProviderPlugin` services that integrate directly with SystemUI's `ClockRegistry`.

---

## Architecture Overview

The codebase is structured around a shared core rendering engine (`SystemUIClocks-CoreLib`) and seven self-contained clock plugin applications (`android_app`).

```
clocks/
├── common/             SystemUIClocks-CoreLib (shared clock engine & controllers)
│   ├── src/com/android/systemui/clocks/
│   │   ├── ClockDesign.kt          Declarative layout, layer & style model
│   │   ├── BaseClockProvider.kt    ClockProviderPlugin implementation base
│   │   ├── CustomClockContext.kt   Clock context & TypeFactory SPI
│   │   ├── AssetLoader.kt          Asset, font, color & Lottie resolution
│   │   ├── AssetDrawable.kt        Theme & doze cross-fading drawable wrapper
│   │   ├── controller/             Clock, face & layer controllers; analog layout
│   │   └── view/                   Text rendering, canvas clipping & layout groups
│   ├── Android.bp
│   └── proguard_clocks.flags
│
├── bignum/             ANALOG_CLOCK_BIGNUM (BigNum Analog)
├── calligraphy/        DIGITAL_CLOCK_CALLIGRAPHY (Calligraphy Digital)
├── growth/             DIGITAL_CLOCK_GROWTH (Growth Digital)
├── inflate/            DIGITAL_CLOCK_INFLATE (Inflate Digital)
├── metro/              DIGITAL_CLOCK_METRO (Transit Digital)
├── numoverlap/         DIGITAL_CLOCK_NUMBEROVERLAP (Number Overlap Digital)
└── weather/            DIGITAL_CLOCK_WEATHER (Weather Digital)
```

Each clock plugin is built as a privileged `system_ext` application that exposes the `com.android.systemui.action.PLUGIN_CLOCK_PROVIDER` service action.

---

## Core Engine (`SystemUIClocks-CoreLib`)

The common library provides shared layout, controller, rendering, and asset-handling primitives:

- **Design System (`ClockDesign.kt`)**: Declarative specifications defining small and large clock faces (`ClockFace`), visual layers (`ClockLayer`), digital styles (`DigitalStyle`), analog configurations (`AnalogStyle`), and text formats (`TextFormat`).
- **Context & Factory SPI (`CustomClockContext.kt`)**: Wraps host context and resources, providing a `TypeFactory` extension point for custom view groups, text views, and layer controllers.
- **Controllers (`controller/`)**:
  - `CustomClockController`: Top-level controller managing face controllers, color palette propagation, AOD/doze transitions, and time ticks.
  - `CustomClockFaceController`: Manages individual face lifecycles, font-axis animations, and doze fraction transitions.
  - `AnalogClockFaceLayout`: Coordinate and rotation manager for analog dials, hour, minute, and second hands.
  - `DigitalHandLayerController`: Manages single and composed digital digit layers and font-axis interpolation.
- **View Hierarchy & Rendering (`view/`)**:
  - `DigitalFaceViewGroup`: Container view hosting digital clock layers.
  - `CustomClockViewGroup`: Base view group coordinating lock screen sizing, bounds, and layout passes.
  - `CustomDigitalTextView`: Specialized text view supporting variable font axes, stroke/fill styling, and canvas transforms.
- **Color & Asset Resolution (`AssetLoader.kt`, `AssetDrawable.kt`)**:
  - Resolves colors, variable fonts, drawables, and animation data across plugin and host contexts.
  - Dynamically extracts tonal shades using Monet color palettes and `ColorStateList.withLStar` adjustments.
- **Plugin Foundation (`BaseClockProvider.kt`)**:
  - Implements SystemUI's `ClockProviderPlugin` interface with API validation, clock picker thumbnail resolution, and controller instantiation.

---

## Clock Styles

| Clock Plugin | Clock ID | Type | Key Visual Mechanics | Assets |
|---|---|---|---|---|
| **BigNum** | `ANALOG_CLOCK_BIGNUM` | Analog | Bold graphic numerals with layered vector hands across normal, large, and AOD modes. | Vector drawables (`res/drawable/`) |
| **Calligraphy** | `DIGITAL_CLOCK_CALLIGRAPHY` | Digital | Slanted, high-contrast calligraphy strokes with variable stroke width. | Variable font (`Calligraphy-Regular.ttf`) |
| **Growth** | `DIGITAL_CLOCK_GROWTH` | Digital | Weight-morphing stencil digits that expand and contract across doze states. | Variable font (`Growth-Variable.ttf`) |
| **Inflate** | `DIGITAL_CLOCK_INFLATE` | Digital | Bubble-style hollow digits with outer stroke outlines and directional doze bounce. | Custom view group (`InflateClockViewGroup`), Font (`Inflate-Regular.ttf`) |
| **Metro** | `DIGITAL_CLOCK_METRO` | Digital | Transit flipboard numbers driven by Lottie vector animations. | Lottie animations (`clocks/lotties/*.json`), vector colon |
| **Number Overlap** | `DIGITAL_CLOCK_NUMBEROVERLAP` | Digital | Vertically stacked numbers with Porter-Duff (`DST_OUT`) masking for transparent cut-outs. | Custom view group (`NumberOverlapClockViewGroup`), Font (`NumOverlap-Variable.ttf`) |
| **Weather** | `DIGITAL_CLOCK_WEATHER` | Digital | Compact digital time display integrated with date, weather condition icons, and temperature. | Custom view group (`WeatherClockViewGroup`), Font (`GoogleSansFlex-Regular.ttf`) |

---

## Build Configuration

The modules are built using the Android Soong build system (`Android.bp`):

- **Target Partition**: `system_ext` (`privileged: true`, `system_ext_specific: true`).
- **Platform Permissions**: `platform_apis: true`, `certificate: "platform"`.
- **Dependencies**: Each clock statically links `SystemUIClocks-CoreLib` and dynamically links `SystemUIPluginLib`.
- **Optimization**: ProGuard/R8 optimization and resource shrinking are enabled across all clock packages using shared rules from `common/` alongside per-clock rules where required.
