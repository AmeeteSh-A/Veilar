# test
# Veilar
### A Two-Phase Android UI Transpiler
![Language](https://img.shields.io/badge/Language-Java_%7C_Groovy-orange) ![Platform](https://img.shields.io/badge/Platform-Android-green) ![License](https://img.shields.io/badge/License-MIT-blue) ![Status](https://img.shields.io/badge/Status-Experimental-red)

Veilar is a **two-phase UI system for Android** that compiles declarative UI intent at **build time** and executes rendering, geometry, gradients, and interactions at **runtime**.

It replaces verbose, multi-file Android UI workflows with a compact DSL written directly in XML, while still producing **100% native Android UI**.

> **"Write Intent. Compile Native."**
> Veilar replaces the verbose, multi-file "drawable hell" of Android development with a single, expressive tag- without sacrificing native performance.

---

## What Problem Does Veilar Solve?

In native Android, even a simple interactive component (custom shape + pressed state + interaction) often requires:
- multiple drawable XML files
- selectors
- duplicated attributes
- manual state wiring

Veilar collapses this into **a single declarative tag**, without sacrificing performance or control.

---

### ⚡ The Efficiency Gap

| Metric | Native Android Workflow | Veilar Two-Phase Pipeline |
| :--- | :--- | :--- |
| **File Overhead** | 4+ Files (XML Shape, Selector, Color, Layout) | **1 File** (Layout only) |
| **Logic Location** | Scattered across `res/drawable` & `res/color` | **Unified** in View attributes |
| **Code Volume** | ~50-60 lines of XML boilerplate | **4-6 lines** of DSL intent |
| **State Wiring** | Manual `android:state_pressed` mapping | **Automatic** via build-time injection |
| **Result** | High maintenance / Fragile references | **100% Native Output** / Zero boilerplate |

## The Core Idea (Simple Explanation)

Veilar works in **two phases**:

### 1. Build Time — Transpilation
- Reads layout XML
- Processes `as9:*` DSL attributes
- Generates native Android resources
- Rewrites views to Veilar runtime components

### 2. Runtime — Execution
- Renders gradients using GPU shaders
- Generates shapes via geometry, not XML
- Handles interactions (press, glow, haptics)
- Updates visuals without reallocating drawables

> You write **intent**.  
> Veilar compiles **structure**.  
> The runtime executes **behavior and rendering**.

---
~~~ mermaid
flowchart TD
    subgraph Phase 1: Build-Time [Transpilation]
        A[Source XML + as9: DSL] --> B[VeilarEngine]
        B --> C{Static Analysis}
        C --> D[Generate XML Selectors]
        C --> E[Inject Binary Assets]
        C --> F[Rewrite Tags to VeilarViews]
    end

    subgraph Phase 2: Runtime [Execution]
        F --> G[VeilarButton / Layout / TV]
        G --> H[GPU Shader Rendering]
        G --> I[Unified Interaction Loop]
        H --> J[High-Fidelity UI]
        I --> J
    end

    D -.-> G
    E -.-> G
~~~
## Native Android vs Veilar

| Native Android | Veilar |
|---------------|--------|
| Multiple XML files | Single layout tag |
| Static drawables | Runtime geometry |
| Manual selectors | Auto-generated |
| No interaction abstraction | Unified interaction engine |
| Verbose XML | Compact DSL |

---

## The DSL (as9:)

Veilar introduces a small, composable DSL via XML attributes.
~~~xml
com.example.veilar.VeilarButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    as9:shape="squircle:16dp"
    as9:shade="crimson+light_green"
    as9:interaction="shrink,vibe"  />
~~~

### Another Example (conceptual)
-*-*-*-*-*-*-*--------------------*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-**--*-*-*-*


(Examples are placeholders — see documentation below.)

---
## Supported Features

### 🏗️ Build-Time (The Compiler)

Features executed by `veilar.gradle` during the build process.

- **Color Algebra & Modifiers**  
  Veilar resolves complex color math before the app even runs. You can blend colors additively (`blue+teal`) or modify brightness (`_dark`) directly in XML.  
  ~~~xml
  as9:shade="blue+yellow"
  as9:bgshade="dark_orange"
  ~~~

- **Gradient Normalization**  
  The compiler parses gradient strings, calculates missing stop positions (e.g., distributing 3 colors evenly), and bundles them into a normalized format for the runtime engine.

- **Context-Aware Sizing**  
  Uses a single `%` syntax that compiles into different attributes depending on the parent container (`ConstraintLayout` vs `LinearLayout`).  
  ~~~xml
  as9:width="30%"
  ~~~

- **Recursive ID Referencing**  
  Allows views to inherit property values from other views by referencing their IDs. The compiler resolves these chains recursively.  
  ~~~xml
  as9:bgshade="id:btn_1"
  ~~~

- **Asset Injection**  
  Copies raw file paths into the project's drawable resources automatically via `as9:src`, eliminating manual file management.
  ~~~xml
  as9:src=""C:\Users\Lenovo\Desktop\ClassiFyUI\itemcontainer.png"
  ~~~

---

### 🚀 Runtime (The Engine)

Features executed by `VeilarButton`, `VeilarLayout`, etc., on the device.

- **Gradient Rendering**  
  The runtime engine interprets the normalized data to construct GPU shaders (Linear, Radial, Sweep) on the fly, allowing for dynamic updates without static drawables.

- **Procedural Geometry (Shapes)**  
  Draws complex shapes mathematically on the GPU.  
  **Supported:** Circle, Oval, Pill, Squircle, Cut Corners, Dynamic Polygons

- **Interaction State Machine**  
  A unified physics engine that handles touch feedback without selector XMLs.  
  - **Motion:** `shrink` (scale down), `pop` (scale up)  
  - **Visual:** `dim` (darken), `glow` (lighten)

- **Haptic Integration**  
  The `vibe` attribute triggers the system's haptic feedback engine automatically on interaction events.


---

## Architecture Overview

### Build-Time (via `buildSrc`)
- DOM-based XML parsing
- ID-based attribute reuse (`id:otherView`)
- DSL → bundle compilation
- Native resource generation
- Tag rewriting to Veilar views
- Build report emission

### Runtime (custom views)
- `VeilarButton`
- `VeilarLayout`
- `VeilarTextView`

These classes consume compiled bundles and execute rendering and interaction logic directly.

---

## Transparency & Safety

Every build emits:

assets/veilar_report.json


This maps generated assets to their source declarations.

- No reflection  
- No hidden runtime parsing  
- No silent behavior  

---

## Documentation

Veilar has **two levels of documentation**:

- **User Manual (Conceptual / Usage)**  
  👉 `[ Manual documentation link will be added here ]`

- **Technical Architecture (Build + Runtime internals)**  
  👉 `[ Technical documentation link will be added here ]`

This separation keeps usage simple while allowing deep technical inspection.

---

## Limitations (Known & Intentional)

- Not incremental-build optimized
- String-based DSL (no schema validation)
- No IDE autocomplete for DSL
- Some runtime code duplication
- Shader recreation during interaction

These are conscious tradeoffs for clarity and learning value.

---

## Project Structure

buildSrc/\
├── VeilarEngine.java\
├── ColorResolver.java\
└── GradientResolver.java

app/\
├── VeilarButton.java\
├── VeilarLayout.java\
├── VeilarTextView.java\
└── attrs.xml


---

## What Veilar Is Not

- ❌ Not a replacement for Jetpack Compose
- ❌ Not a design system
- ❌ Not a runtime UI framework
- ❌ Not production-ready tooling

Veilar is a **systems-level exploration** of Android UI compilation and execution.

---

## Author

Built by **Ameetesh**  
B.Tech Undergraduate (2nd–3rd semester)  
Focused on Android internals, UI systems, and build tooling.

---

## License

MIT
