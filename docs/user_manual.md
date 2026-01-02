# 📘 Veilar User Manual

Welcome to Veilar. This guide is the definitive reference for using the Veilar UI system. It is designed to be simple enough for beginners but detailed enough for power users.

---

## 1. 📦 Installation (Step-by-Step)

Veilar is a **local module**. You do not download it from the internet via Maven; you copy the source code directly into your project.

### Step A: Copy the Source
-  **Download** the Veilar repository/zip.
-  **Copy** the folder named `veilar`.
-  **Paste** it into your project's **root directory**.
    *(This is the folder that contains `app`, `gradle`, and `build.gradle` files).*

### Step B: Tell Gradle it Exists
Open `settings.gradle` (or `settings.gradle.kts`) in your project root. Add the line corresponding to your language:

**For Groovy (`settings.gradle`):**
```groovy
include ':veilar'

```

**For Kotlin (`settings.gradle.kts`):**

```kotlin
include(":veilar")

```

### Step C: Link the Dependency

Open `app/build.gradle` (or `app/build.gradle.kts`). Scroll to the `dependencies { ... }` block and add:

**For Groovy:**

```groovy
dependencies {
    implementation project(':veilar')
}

```

**For Kotlin:**

```kotlin
dependencies {
    implementation(project(":veilar"))
}

```

### Step D: Activate the Compiler (The Invisible Hook)

This is the most important step. Still in `app/build.gradle`, add this line at the very top or very bottom of the file:

```groovy
apply from: '../veilar/veilar.gradle'

```

**Final Step:** Go to the top menu bar in Android Studio and click **File -> Sync Project with Gradle Files**.

---

## 2. 🎨 Attributes Guide (The `as9:` namespace)

All Veilar commands start with `as9:`. You use them inside your XML layout files (like `activity_main.xml`) just like standard Android attributes.

### A. Shapes (`as9:shape`)

Defines the physical geometry (outline) of the view.

| Command | Syntax | Description |
| --- | --- | --- |
| **Squircle** | `squircle` | A "super-ellipse." Much smoother than a standard rounded rectangle. |
| **Pill** | `pill` | Fully rounded sides (perfect semicircles). Ideal for buttons. |
| **Cut Corner** | `cut` | Slices the corners off diagonally at 45 degrees (customizable). |
| **Polygon** | `gon:N` | Creates a regular polygon with **N** sides. <br>

  eg: ` as9:shape="gon:6"` renders the view into a hexagon

**Default:** If N is missing or invalid, it defaults to **5** (Pentagon). |

 **Circle**: `circle` forces the view to be a perfect circle.
 **Oval**  `oval` similar to circle but respects the view's width/height ratio. 

**Defaults:**

* **Radius:** If you use `squircle` or `cut` but do not set a radius, Veilar defaults to **16dp**.
eg: `as9:shape="squircle:24` renders a `squircle` with a 24dp radius while `as9:shape="squircle` renders one with a default radius of 16dp (*similar for other such shapes that require a radius*)
* **Rectangles:** If you use a gradient but *don't* specify a shape, Veilar defaults to a standard rectangle with **8dp** corners.

### B. Colors (`as9:shade` vs `as9:bgshade`)

Veilar uses a powerful "Color Algebra" engine. You can mix and modify colors directly in XML strings.

**Supported Colors:**
`red`, `blue`, `green`, `yellow`, `orange`, `purple`, `teal`, `magenta`, `amber`, `lime`, `violet`, `crimson`, `black`, `white`, `gray`, `brown`, `cyan`.

**Syntax:** `MODIFIER_MODIFIER_COLOR + COLOR`

* **Mixing:** `red+blue` (Averages the RGB values of Red and Blue).
* **Lighten:** `light_red` (Adds brightness).
* **Darken:** `dark_red` (Removes brightness).
* **Chaining:** `light_light_blue` (Brightens Blue twice).

**Behavior Difference:**

1. **`as9:shade` (Smart Context):**
* If used on a **TextView**: Changes the **Text Color**.
* If used on a **Button/Layout**: Changes the **Background Tint**.


2. **`as9:bgshade` (Force Background):**
* Always changes the **Background**, even on a TextView.



**Default:** If a color name is invalid or empty, it defaults to **Black** (`#000000`).

### C. Gradients (`as9:gradient` vs `as9:bggradient`)

Veilar compiles complex gradients into optimized GPU shaders without requiring separate XML drawable files.

**Syntax:** `COLORS | TYPE | ANGLE | TILE`

* **Colors:** Semicolon-separated list (e.g., `red;blue`). Can include percentages (e.g., `red:10%`).
* **Type:** `linear` (default), `radial`, or `sweep`.
* **Angle:** Integer value in degrees (e.g., `90` for top-to-bottom).
* **Tile:** `clamp` (default), `repeat`, or `mirror`.

**Behavior Difference:**

1. **`as9:gradient` (Text Context):**
* Applies the gradient to the **TEXT itself** (e.g., Gold text effect).
* *Note: Ignored on Layouts.*


2. **`as9:bggradient` (Background Context):**
* Applies the gradient to the **BACKGROUND** (e.g., a gradient button or container).



**Smart Features:**

* **Auto-Normalization:** `red;blue;green` automatically calculates stops at 0%, 50%, and 100%.
* **Over-Saturation:** If stops exceed 100% (e.g., `50% + 80%`), Veilar automatically rescales them to fit.
* **Defaults:** Missing type defaults to `linear`. Missing angle defaults to `0` (Left-to-Right).

**Example:** `red;blue | linear | 90` *(Red to Blue, Top to Bottom)*
### D. Interactions (`as9:interaction`)

A comma-separated list of effects triggered by user input.

| Effect | Trigger | Behavior |
| --- | --- | --- |
| `shrink` | **Tap** (Touch Down) | Scales view down to roughly 95%. |
| `pop` | **Tap** (Touch Down) | Scales view up to roughly 105%. |
| `dim`** | **Tap** (Touch Down) | Darkens the color by 20%.|
| `glow`** | **Tap** (Touch Down) | Brightens the color by 30%. |
| `vibe` | **Hold** (Long Press) | Triggers system haptic feedback (Keyboard Tap vibration). |

⚠️ **Requires `as9:shade**` defined to calculate the dark/bright version. 

**Note on Timing:**

* **Build-Time:** `dim` and `glow` colors are calculated during compilation.
* **Runtime:** `shrink`, `pop`, and `vibe` animations happen live on the device.

### E. Sizing & Cloning

* **Percent Sizing:** `as9:width="50%"`
* Sets width/height to a percentage of the parent.
* **Requirement:** Parent must be `ConstraintLayout` or `LinearLayout`. If used inside a `FrameLayout`, Veilar will warn you.


* **Cloning:** `as9:width="id:header"`
* Copies the width value from another view named `header`.
* **Recursive:** If View A copies View B, and View B copies View C, Veilar resolves the entire chain.



### F. Asset Injection (`as9:src`)

* **Syntax:** `as9:src="C:\Path\To\Image.png"`
* **What it does:** Copies the file from your computer into the app's `drawable` folder automatically and sets it as the background.
* **Safety:** If the file doesn't exist, Veilar prints a warning and the view will have no background.

---

## 3. ⚠️ Troubleshooting (The Warning System)

Veilar is transparent. If something is wrong, it prints **Yellow Warnings** in your Android Studio "Build" output tab.

| Warning Message | Cause | Solution |
| --- | --- | --- |
| `Color input is empty. Defaulting to Black.` | You wrote `as9:shade=""` or provided an empty string. | Add a valid color name or hex code. |
| `Interaction requires 'as9:shade' to calculate brightness.` | You used `dim` or `glow` but Veilar doesn't know the starting color. | Add `as9:shade="red"` (or your desired color) so the compiler knows what to darken/lighten. |
| `Percent-based width... requires ConstraintLayout.` | You used `50%` sizing inside a `FrameLayout` or `RelativeLayout`. | Wrap your view in a `ConstraintLayout` or `LinearLayout`. |
| `Resolution depth limit (10) exceeded.` | Infinite Loop (View A copies View B, View B copies View A). | Check your `id:` references and remove the loop. |
| `Invalid polygon side count.` | You wrote `gon:x` where x is not a number. | Use a valid integer, e.g., `gon:6`. |


#### [⚙️Check out the architecture document] (Veilar/docs/architecture.md) ####
