# Veilar
### A Two-Phase Android UI Transpiler
![Language](https://img.shields.io/badge/Language-Java_%7C_Groovy-orange) ![Platform](https://img.shields.io/badge/Platform-Android-green) ![License](https://img.shields.io/badge/License-MIT-blue) ![Status](https://img.shields.io/badge/Status-Experimental-red)

---
### 🔗 Quick Links
- [⚡ The Efficiency Gap](#-the-efficiency-gap) - [⚙️ Architecture](#%EF%B8%8F-architecture-the-invisible-hook) - [📦 Installation Steps](#-installation)
- [📐 Usage Example](#-usage-example) - [✨Features](#supported-features) -[📄Documentation](#documentation) - [📂 Project Structure](#-project-structure)
---

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

<p align="right">(<a href="#veilar">back to top</a>)</p>

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

<p align="right">(<a href="#veilar">back to top</a>)</p>

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

<p align="right">(<a href="#veilar">back to top</a>)</p>

---

## The DSL (as9:)

Veilar introduces a small, composable DSL via XML attributes.
## 📐 Usage Example

### Input (What You Write)

```xml
<Button
    android:id="@+id/my_button"
    android:layout_width="match_parent"
    android:layout_height="60dp"
    as9:shape="squircle:16dp"
    as9:bggradient="crimson+light_amber:30% | linear | 45"
    as9:interaction="shrink, glow, vibe"
    as9:width="50%" />

<Button
    android:id="@+id/your_button"
    android:layout_width="match_parent"
    android:layout_height="60dp"
    as9:shape="gon:8"
    as9:src="C:\Users\Lenovo\Desktop\ClassiFy\classifyUI\itemcontainer.png"
    as9:interaction="shrink, pop"
    as9:width="id:my_button" />
```

### Output (What Veilar Compiles To)

#### 🏗️ Build-Time (Compiler)

- Generates `vsel_my_button.xml` (Color State List) for interaction states.
- Resolves color algebra expressions (e.g., `crimson+light_amber`) and **converts all colors to final hex values**.
- Parses gradient definitions, resolves gradient type and parameters, and **normalizes color stop positions**.
- Resolves `as9:width="50%"` and injects  
  `layout_constraintWidth_percent="0.5"` (or the appropriate attribute based on the parent layout).
- Resolves `as9:width="id:my_button"` by recursively inheriting the referenced view’s computed value.
- Detects raw asset paths, copies files into `res/drawable`, and rewrites references automatically.
- Precomputes color variants required for interaction effects such as `glow` and `dim`.

#### 🚀 Runtime (Engine)

- **Renders normalized gradients on the GPU** (Linear, Radial, Sweep) using runtime shader construction.
- Renders procedural shapes (e.g., **Squircle**, **Polygon**) directly on the GPU.
- Executes interaction animations (`shrink`, `pop`) via a unified physics engine.
- Triggers system haptic feedback automatically when `vibe` is specified.

---

**Result:** Clean XML in, fully resolved colors and gradients at build-time, GPU-driven rendering at runtime — no manual selectors, no asset plumbing, no unnecessary runtime overhead.


(Examples are placeholders — see documentation below.)

<p align="right">(<a href="#veilar">back to top</a>)</p>

---

## ✨Supported Features

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

<p align="right">(<a href="#veilar">back to top</a>)</p>

---

## ⚙️ Architecture: The "Invisible Hook"

Veilar's genius lies in how it attaches to your project. It does not require a custom IDE plugin or a forked Android SDK.

### 1. The Build-Time Hook
When you add `apply from: 'veilar.gradle'`, Veilar inserts itself directly into the Gradle execution graph **before** the standard Android resource merger runs.
1.  **Intercept:** It scans your layout XMLs for `as9:` attributes.
2.  **Compile:** It calculates the geometry and gradients in Java/Groovy.
3.  **Inject:** It generates standard Android XML resources (drawables, selectors) and places them into the build stream.
4.  **Rewrite:** It swaps your raw tags (e.g., `<Button>`) with Veilar's runtime counterparts.

**Result:** The final APK contains optimized, native bytecode. The heavy lifting is done before the app even launches.

### 2. The Runtime Engine
Once the app launches, `VeilarButton` and `VeilarLayout` take over. instead of parsing heavy XML files, they read the lightweight, pre-compiled "bundles" injected during the build.
* **Zero Reflection:** Properties are applied via standard setter methods.
* **GPU Rendering:** Shapes are drawn using `Canvas` and `Shader` primitives, ensuring 60fps animations even for complex polygons.---

## 🛡️Transparency & Safety

Every build emits:

`assets/veilar_report.json`


This maps generated assets to their source declarations.

- **No Reflection:** Attributes are applied via standard setters, keeping it fast and safe.
- **Build-Time Resolution:** Heavy DSL logic (recursion, color math) is compiled, not interpreted at runtime.
- **Explicit Execution:** The runtime views only consume optimized, pre-compiled data bundles. 

<p align="right">(<a href="#veilar">back to top</a>)</p>

---

## 📄Documentation

Veilar has **two levels of documentation**:

- **[📘 User Manual (Conceptual / Usage)](Veilar/docs/user_manual.md)**
  *For developers integrating Veilar into their apps.*

- **[⚙️ Technical Architecture (Build + Runtime internals)](Veilar/docs/architecture.md)**
  *For contributors and engineers.*

This separation keeps usage simple while allowing deep technical inspection.

<p align="right">(<a href="#veilar">back to top</a>)</p>

---

## 📦 Installation

Veilar is designed as a drop-in module.

- Copy the veilar directory to your project root.
```text
your-project/
├─ app/
├─ veilar/
└─ settings.gradle and other packages
```

- Include the module in `settings.gradle`:
    
    include ':veilar'

- Add Dependency in `app/build.gradle`:

    dependencies {
    implementation project(':veilar')
    }

- Activate Compiler in `app/build.gradle` (Top or Bottom):

    apply from: '../veilar/veilar.gradle'

*(This hooks Veilar into your build process to perform the XML translation.)*

<p align="right">(<a href="#veilar">back to top</a>)</p>

---

## ⚠️Technical Trade-offs (Known & Intentional)

- Not incremental-build optimized
- String-based DSL (no schema validation)
- No IDE autocomplete for DSL
- Some runtime code duplication
- Shader recreation during interaction

These are conscious tradeoffs for clarity and learning value.

---

## 📂 Project Structure

Veilar is contained entirely within a single module, making it easy to drop into any project.

```text
Project Root
├── app/                      // Your App (Consumer)
│   └── src/main/assets/      // 📄 veilar_report.json (Generated Debug Map)
│
└── veilar/                   // The Veilar Library (Source)
    ├── veilar.gradle         // 🏗️ THE COMPILER (Groovy Script)
    │                         // Contains: VeilarEngine, ColorResolver, GradientResolver
    │
    └── src/main/
        ├── java/com/veilar/  // 🚀 THE RUNTIME (Java Views)
        │   ├── VeilarButton.java
        │   ├── VeilarLayout.java
        │   └── VeilarTextView.java
        │
        └── res/values/
            └── attrs.xml     // 🎨 THE DSL (Attribute Definitions)
```

<p align="right">(<a href="#veilar">back to top</a>)</p>

---

## 🚧What Veilar Is Not

- ❌ Not a replacement for Jetpack Compose
- ❌ Not a design system
- ❌ Not a runtime UI framework
- ❌ Not production-ready tooling

Veilar is a **systems-level exploration** of Android UI compilation and execution.

---
## 💡 Why I Built This
I wanted to challenge the assumption that Android UI development *has* to be verbose. By treating XML layouts as source code to be compiled rather than just static markup, I proved that we can have the **expressiveness of a modern framework** (like Flutter) with the **performance and stability of the native Android View System**.

---

## 👨‍💻Author

Built by **Ameetesh**  
B.Tech Undergraduate (South Asian University)  
Focused on Android internals, UI systems, and build tooling.

---

## License

MIT License. Free to use, modify, and learn from.

<p align="right">(<a href="#veilar">back to top</a>)</p>
