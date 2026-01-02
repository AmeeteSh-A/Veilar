package com.veilar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.Arrays;

public class VeilarLayout extends ConstraintLayout {

    private float manualRadius = 0f;
    private int shapeId = 0;
    private int shapeParam = 0;
    private String interactionBundle = "";
    private String cachedBgGradient = null;
    private String cachedBgShade = null;

    public VeilarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VeilarAttributes);
        cachedBgGradient = a.getString(R.styleable.VeilarAttributes_bggradient);
        cachedBgShade = a.getString(R.styleable.VeilarAttributes_bgshade);
        String radiusStr = a.getString(R.styleable.VeilarAttributes_radius);
        String shapeBundle = a.getString(R.styleable.VeilarAttributes_shapeBundle);
        interactionBundle = a.getString(R.styleable.VeilarAttributes_interactionBundle);
        if (interactionBundle == null) interactionBundle = "";
        a.recycle();

        setClickable(true);
        setLongClickable(true);

        float density = getResources().getDisplayMetrics().density;

        if (shapeBundle != null) {
            String[] parts = shapeBundle.split(":");
            shapeId = Integer.parseInt(parts[0]);
            shapeParam = Integer.parseInt(parts[1]);
        }

        if (radiusStr != null) {
            manualRadius = Float.parseFloat(radiusStr.replaceAll("[^\\d.]", "")) * density;
        } else if (cachedBgGradient != null) {
            manualRadius = 8f * density;
        }

        post(() -> {
            if (cachedBgGradient != null && !cachedBgGradient.isEmpty()) {
                applyGradient(cachedBgGradient, 1.0f);
            } else if (cachedBgShade != null && !cachedBgShade.isEmpty()) {
                String dummyGradient = "linear:0:0|" + cachedBgShade + ":0;" + cachedBgShade + ":1|0|clamp";
                applyGradient(dummyGradient, 1.0f);
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (interactionBundle.contains("shrink")) {
            if (action == MotionEvent.ACTION_DOWN) {
                this.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start();
                handleInteractionShift(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                this.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                handleInteractionShift(false);
            }
        } else {
            if (action == MotionEvent.ACTION_DOWN) handleInteractionShift(true);
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) handleInteractionShift(false);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void handleInteractionShift(boolean isPressed) {
        float factor = 1.0f;
        if (isPressed) {
            if (interactionBundle.contains("dim")) factor = 0.85f;
            else if (interactionBundle.contains("glow")) factor = 1.15f;
        }

        Drawable bg = getBackground();
        if (bg instanceof ShapeDrawable) {
            if (cachedBgGradient != null) {
                updateShaderOnly(cachedBgGradient, factor);
            } else if (cachedBgShade != null) {
                String dummyGradient = "linear:0:0|" + cachedBgShade + ":0;" + cachedBgShade + ":1|0|clamp";
                updateShaderOnly(dummyGradient, factor);
            }
        } else if (bg != null) {
            if (isPressed && factor != 1.0f) {
                int tint = (factor < 1.0f) ? Color.argb(40, 0, 0, 0) : Color.argb(40, 255, 255, 255);
                bg.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP);
            } else {
                bg.clearColorFilter();
            }
        }
    }

    private void updateShaderOnly(String bundle, float factor) {
        try {
            Drawable bg = getBackground();
            if (!(bg instanceof ShapeDrawable)) return;
            ShapeDrawable drawable = (ShapeDrawable) bg;

            String[] segments = bundle.split("\\|");
            String[] shaderParams = segments[0].split(":");
            String type = shaderParams[0];

            String[] colorEntries = segments[1].split(";");
            int[] colors = new int[colorEntries.length];
            float[] stops = new float[colorEntries.length];
            for (int i = 0; i < colorEntries.length; i++) {
                String[] pair = colorEntries[i].split(":");
                int baseColor = Color.parseColor(pair[0]);
                colors[i] = factor == 1.0f ? baseColor : shiftBrightness(baseColor, factor);
                stops[i] = Float.parseFloat(pair[1]);
            }

            float w = getWidth(), h = getHeight();
            Shader shader;

            if (type.equals("radial")) {
                shader = new RadialGradient(w / 2, h / 2, Math.max(w, h) / 2, colors, stops, Shader.TileMode.CLAMP);
            } else if (type.equals("sweep")) {
                shader = new SweepGradient(w / 2, h / 2, colors, stops);
            } else {
                int angle = (segments.length > 2) ? Integer.parseInt(segments[2]) : 0;
                double rad = Math.toRadians(angle);
                shader = new LinearGradient(0, 0, (float) (Math.cos(rad) * w), (float) (Math.sin(rad) * h), colors, stops, Shader.TileMode.CLAMP);
            }

            drawable.getPaint().setShader(shader);
            invalidate();
        } catch (Exception e) {}
    }

    @Override
    public boolean performLongClick() {
        boolean handled = super.performLongClick();
        if (interactionBundle.contains("vibe")) {
            this.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        if (interactionBundle.contains("pop")) {
            this.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100)
                    .withEndAction(() -> this.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                    .start();
        }
        return handled;
    }

    private void applyGradient(String bundle, float brightnessFactor) {
        try {
            String[] segments = bundle.split("\\|");
            if (segments.length < 2) return;

            String[] colorEntries = segments[1].split(";");
            int[] colors = new int[colorEntries.length];
            float[] stops = new float[colorEntries.length];
            for (int i = 0; i < colorEntries.length; i++) {
                String[] pair = colorEntries[i].split(":");
                int baseColor = Color.parseColor(pair[0]);
                colors[i] = brightnessFactor == 1.0f ? baseColor : shiftBrightness(baseColor, brightnessFactor);
                stops[i] = Float.parseFloat(pair[1]);
            }

            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;

            Shader shader;
            String type = segments[0].split(":")[0];
            if (type.equals("radial")) {
                shader = new RadialGradient(w / 2, h / 2, Math.max(w, h) / 2, colors, stops, Shader.TileMode.CLAMP);
            } else if (type.equals("sweep")) {
                shader = new SweepGradient(w / 2, h / 2, colors, stops);
            } else {
                int angle = (segments.length > 2) ? Integer.parseInt(segments[2]) : 0;
                double rad = Math.toRadians(angle);
                shader = new LinearGradient(0, 0, (float) (Math.cos(rad) * w), (float) (Math.sin(rad) * h), colors, stops, Shader.TileMode.CLAMP);
            }

            Shape vShape = getVeilarShape(w, h);
            ShapeDrawable drawable = new ShapeDrawable(vShape);
            drawable.getPaint().setShader(shader);
            this.setBackground(drawable);

            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (shapeId == 1) outline.setOval(0, 0, (int)w, (int)h);
                    else outline.setRoundRect(0, 0, (int)w, (int)h, manualRadius);
                }
            });
        } catch (Exception e) {}
    }

    private int shiftBrightness(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2]));
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    private Shape getVeilarShape(float w, float h) {
        switch (shapeId) {
            case 1: return new android.graphics.drawable.shapes.OvalShape();
            case 2: return createCutCornerShape(w, h, manualRadius);
            case 3:
                float pr = Math.min(w, h) / 2f;
                float[] radii = new float[8];
                Arrays.fill(radii, pr);
                return new RoundRectShape(radii, null, null);
            case 4: return createSquircleShape(w, h, manualRadius);
            case 5: return createPolygonShape(w, h, shapeParam);
            default:
                float[] r = new float[8];
                Arrays.fill(r, manualRadius);
                return new RoundRectShape(r, null, null);
        }
    }

    private Shape createCutCornerShape(float w, float h, float r) {
        Path path = new Path();
        path.moveTo(r, 0); path.lineTo(w - r, 0); path.lineTo(w, r);
        path.lineTo(w, h - r); path.lineTo(w - r, h); path.lineTo(r, h);
        path.lineTo(0, h - r); path.lineTo(0, r); path.close();
        return new PathShape(path, w, h);
    }

    private Shape createPolygonShape(float w, float h, int sides) {
        Path path = new Path();
        float cx = w / 2f, cy = h / 2f, rx = w / 2f, ry = h / 2f;
        for (int i = 0; i < sides; i++) {
            double angle = 2.0 * Math.PI * i / sides - Math.PI / 2.0;
            float x = (float) (cx + rx * Math.cos(angle)), y = (float) (cy + ry * Math.sin(angle));
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        return new PathShape(path, w, h);
    }

    private Shape createSquircleShape(float w, float h, float r) {
        Path path = new Path();
        float maxR = Math.min(w, h) / 2f, safeR = Math.min(r, maxR);
        float ratio = 0.5522847498f, c = safeR * ratio;
        path.moveTo(safeR, 0); path.lineTo(w - safeR, 0);
        path.cubicTo(w - c, 0, w, c, w, safeR); path.lineTo(w, h - safeR);
        path.cubicTo(w, h - c, w - c, h, w - safeR, h); path.lineTo(safeR, h);
        path.cubicTo(c, h, 0, h - c, 0, h - safeR); path.lineTo(0, safeR);
        path.cubicTo(0, c, c, 0, safeR, 0); path.close();
        return new PathShape(path, w, h);
    }
}