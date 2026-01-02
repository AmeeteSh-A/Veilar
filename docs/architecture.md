# ⚙️ Veilar Architecture & Internals

> **"Write Intent. Compile Native."**

Veilar is not just a runtime library; it is a **Build-Time Transpiler** that sits between your source XML and the Android Asset Packaging Tool (AAPT). This document details the engineering decisions behind the system.

---

## 1. The Build-Time Hook (`veilar.gradle`)

Standard Android libraries use Annotation Processing (KAPT/KSP) to generate code. Veilar takes a more aggressive approach: **Gradle Lifecycle Injection**.

### The Injection Strategy
Veilar does not ask the user to change their source sets. Instead, it hooks into the build graph dynamically.

1.  **The Hook Point:**
    We use `afterEvaluate` in the Gradle lifecycle. This ensures that all Android source sets (debug, release, custom flavors) are fully defined before we modify them.

2.  **Priority Override:**
    Veilar injects its generated resources into the `debug` and `release` source sets, *not* the `main` source set.
    ```groovy
    // veilar.gradle
    android.sourceSets.matching { it.name == 'debug' || it.name == 'release' }.all { 
        sourceSet.res.srcDirs += "${project.buildDir}/generated/veilar/res"
    }
    ```
    **Engineering Decision:** Android's resource merger prioritizes Build Type resources over Main resources. By injecting into `debug`, Veilar's generated XMLs silently shadow the original files without triggering "Duplicate Resource" conflicts.

3.  **Task Dependency:**
    The `runVeilarCompiler` task is registered as a dependency of `preBuild`. This guarantees that Veilar finishes its compilation *before* the Android Resource Merger (AAPT2) begins its work.

---

## 2. The Compiler Engine (`VeilarEngine`)

The compiler is written in Groovy and runs inside the Gradle daemon. It operates in three distinct phases.

### Phase A: Static Analysis (Snapshotting)
Before processing a single tag, the engine scans every layout file to build a **Global Attribute Snapshot**.
* **Structure:** `Map<ViewID, Map<Attribute, Value>>`
* **Purpose:** This enables **Recursive Resolution**.
    * If `View A` references `View B` for its width...
    * ...and `View B` references `View C`...
    * The engine can look up C -> B -> A instantly.
    * **Safety:** A recursion depth limit (10) prevents infinite loops (StackOverflow protection).

### Phase B: The Math Engines
Veilar treats UI attributes as mathematical expressions rather than static strings.

#### 1. Color Algebra (`ColorResolver`)
* **Input:** `blue + light_red`
* **Algorithm:**
    1.  Resolve base colors to Hex (e.g., `#0000FF`, `#FF0000`).
    2.  Apply modifiers: `light` adds +30 brightness per channel; `dark` subtracts 30.
    3.  **Blend:** Calculate the arithmetic mean of the R, G, and B channels independently.
    4.  **Output:** A single optimized Hex string (e.g., `#800080`).

#### 2. Gradient Normalization (`GradientResolver`)
Android's native `GradientDrawable` fails if color stops are missing or unordered. Veilar's parser is robust.
* **Auto-Stops:** If stops are missing (e.g., `red;blue;green`), Veilar calculates the delta `(1.0 / (count - 1))` and assigns stops at `0.0`, `0.5`, `1.0`.
* **Bundling:** The result is packed into a compact, pipe-delimited string format (`type:params|colors|stops|angle`) to minimize runtime parsing overhead.

### Phase C: Transpilation (The Rewrite)
The engine reads the source XML DOM and writes a transformed version to the build directory.

* **Tag Swapping:**
    * `<Button>` → `<com.veilar.VeilarButton>`
    * `<ConstraintLayout>` → `<com.veilar.VeilarLayout>`
* **Asset Injection:**
    * If `as9:src` points to a local file path, the engine **copies the file bytes** directly into the generated `res/drawable` folder.
    * It then rewrites the attribute to refer to the new `@drawable/vsrc_...` resource ID.

---

## 3. The Runtime Engine (`VeilarView`)

The runtime views (`VeilarButton`, `VeilarLayout`, `VeilarTextView`) are lightweight wrappers designed for performance.

### Zero-Allocation Shader Factory
Standard Android UI often creates new `GradientDrawable` objects on the heap for every state change. Veilar avoids this.
* **Gradients:** Are constructed as native `Shader` objects (Linear, Radial, Sweep).
* **Application:**
    * **Text:** Applied directly to the `TextView.getPaint()` object.
    * **Backgrounds:** Applied to a cached `ShapeDrawable`.
* **Geometry:** Shapes (Squircles, Polygons) are drawn using `Path` primitives calculated once on size change, ensuring 60fps performance during scrolling.

### The Unified Interaction State Machine
Veilar replaces XML StateListSelectors with a physics-based interaction loop.

| State | Logic |
| :--- | :--- |
| **Tap (Down)** | Triggers `scaleX/Y` animation to **0.95** (shrink) or **1.05** (pop). <br> Swaps shader colors to "Darker" or "Lighter" variants pre-calculated by the compiler. |
| **Tap (Up)** | Springs back to **1.0**. |
| **Hold** | Triggers `HapticFeedbackConstants.KEYBOARD_TAP`. |

### Performance Profile
* **Memory:** Lower than native XML. A single `VeilarButton` replaces 3-4 separate XML drawable files (background, pressed state, ripple mask).
* **CPU:** Init time is slightly higher (parsing the bundle string) but frame render time is identical to native (as it uses standard Canvas API).

---

## 4. Project Structure

```text
veilar/
├── veilar.gradle          # The Compiler (Groovy Script)
│                          # Contains: VeilarEngine, ColorResolver, GradientResolver
│
└── src/main/
    ├── res/values/attrs.xml    # The DSL Definition
    │
    └── java/com/veilar/        # The Runtime Components
        ├── VeilarButton.java   # Handles 'shrink' and 'bggradient'
        ├── VeilarLayout.java   # Handles 'pop', 'vibe', containers
        └── VeilarTextView.java # Handles text gradients
