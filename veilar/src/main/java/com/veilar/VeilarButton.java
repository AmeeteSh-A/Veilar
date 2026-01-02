package com.veilar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient; 
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import androidx.appcompat.widget.AppCompatButton;
import java.util.Arrays;

public class VeilarButton extends AppCompatButton {

    private float manualRadius = 0f;
    private int shapeId = 0;
    private int shapeParam = 0;
    private String interactionBundle = "";
    private String cachedBgGradient = null;
    private String cachedBgShade = null;

    public VeilarButton(Context androidContext, AttributeSet attrs) {
        super(androidContext, attrs); 
        init(androidContext, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        
        setBackground(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(0);
            setStateListAnimator(null);
        }
        setPadding(0, 0, 0, 0);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VeilarAttributes);
        String textGradient = a.getString(R.styleable.VeilarAttributes_gradient);
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
        }

        post(() -> {
            if (textGradient != null && !textGradient.isEmpty()) applyGradient(textGradient, true, 1.0f);

            if (cachedBgGradient != null && !cachedBgGradient.isEmpty()) {
                applyGradient(cachedBgGradient, false, 1.0f);
            } else if (cachedBgShade != null && !cachedBgShade.isEmpty()) {
                String dummyGradient = "linear|" + cachedBgShade + ":0;" + cachedBgShade + ":1|0|clamp";
                applyGradient(dummyGradient, false, 1.0f);
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            if (cachedBgGradient != null) applyGradient(cachedBgGradient, false, 1.0f);
            else if (cachedBgShade != null) {
                String dummyGradient = "linear|" + cachedBgShade + ":0;" + cachedBgShade + ":1|0|clamp";
                applyGradient(dummyGradient, false, 1.0f);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (interactionBundle.contains("shrink")) {
            if (action == MotionEvent.ACTION_DOWN) {
                this.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                handleInteractionShift(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                this.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                handleInteractionShift(false);
            }
        } else {
            if (action == MotionEvent.ACTION_DOWN) handleInteractionShift(true);
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) handleInteractionShift(false);
        }
        return super.onTouchEvent(event);
    }

    private void handleInteractionShift(boolean isPressed) {
        float factor = 1.0f;
        if (isPressed) {
            if (interactionBundle.contains("dim")) factor = 0.8f;
            else if (interactionBundle.contains("glow")) factor = 1.2f;
        }

        Drawable bg = getBackground();
        if (bg instanceof RippleDrawable) {
            Drawable content = ((RippleDrawable) bg).getDrawable(0);
            if (content instanceof ShapeDrawable) {
                if (cachedBgGradient != null) updateShaderOnly((ShapeDrawable) content, cachedBgGradient, factor);
                else if (cachedBgShade != null) {
                    String dummyGradient = "linear|" + cachedBgShade + ":0;" + cachedBgShade + ":1|0|clamp";
                    updateShaderOnly((ShapeDrawable) content, dummyGradient, factor);
                }
            } else if (isPressed && factor != 1.0f) {
                int tint = (factor < 1.0f) ? Color.argb(60, 0, 0, 0) : Color.argb(60, 255, 255, 255);
                bg.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP);
            } else {
                bg.clearColorFilter();
            }
        }
    }

    private void updateShaderOnly(ShapeDrawable drawable, String bundle, float factor) {
        try {
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

    private void applyGradient(String bundle, boolean isText, float brightnessFactor) {
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
            if (isText) {
                getPaint().setShader(shader);
                invalidate();
            } else {
                Shape vShape = getExactShape(w, h);
                ShapeDrawable content = new ShapeDrawable(vShape);
                content.getPaint().setShader(shader);
                content.setBounds(0, 0, (int)w, (int)h);

                ShapeDrawable mask = new ShapeDrawable(vShape);
                mask.setBounds(0, 0, (int)w, (int)h);

                RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40FFFFFF")), content, mask);
                this.setBackground(ripple);

                boolean isSimple = (shapeId == 1 || shapeId == 3 || shapeId == 0);
                if (isSimple) {
                    setClipToOutline(true);
                    setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            if (shapeId == 1) outline.setOval(0, 0, (int)w, (int)h);
                            else outline.setRoundRect(0, 0, (int)w, (int)h, manualRadius);
                        }
                    });
                } else {
                    setClipToOutline(false);
                    setOutlineProvider(null);
                }
            }
        } catch (Exception e) {}
    }

    private int shiftBrightness(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2]));
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    private Shape getExactShape(float w, float h) {
        if (shapeId == 1) return new android.graphics.drawable.shapes.OvalShape();
        if (shapeId == 3) {
            float pr = Math.min(w, h) / 2f;
            float[] r = new float[]{pr, pr, pr, pr, pr, pr, pr, pr};
            return new RoundRectShape(r, null, null);
        }
        if (shapeId == 0) {
            float[] r = new float[]{manualRadius, manualRadius, manualRadius, manualRadius, manualRadius, manualRadius, manualRadius, manualRadius};
            return new RoundRectShape(r, null, null);
        }

        final Path path;
        if (shapeId == 2) path = getCutPath(w, h, manualRadius);
        else if (shapeId == 4) path = getSquirclePath(w, h, manualRadius);
        else if (shapeId == 5) path = getPolyPath(w, h, shapeParam);
        else path = new Path();

        return new Shape() {
            @Override
            public void draw(Canvas canvas, Paint paint) {
                canvas.drawPath(path, paint);
            }
        };
    }

    private Path getCutPath(float w, float h, float r) {
        Path path = new Path();
        path.moveTo(r, 0); path.lineTo(w - r, 0); path.lineTo(w, r);
        path.lineTo(w, h - r); path.lineTo(w - r, h); path.lineTo(r, h);
        path.lineTo(0, h - r); path.lineTo(0, r); path.close();
        return path;
    }

    private Path getPolyPath(float w, float h, int sides) {
        Path path = new Path();
        float cx = w / 2f, cy = h / 2f, rx = w / 2f, ry = h / 2f;
        for (int i = 0; i < sides; i++) {
            double angle = 2.0 * Math.PI * i / sides - Math.PI / 2.0;
            float x = (float) (cx + rx * Math.cos(angle));
            float y = (float) (cy + ry * Math.sin(angle));
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        return path;
    }

    private Path getSquirclePath(float w, float h, float r) {
        Path path = new Path();
        float ratio = 0.5522847498f, c = r * ratio;
        path.moveTo(r, 0);
        path.lineTo(w - r, 0);
        path.cubicTo(w - c, 0, w, c, w, r);
        path.lineTo(w, h - r);
        path.cubicTo(w, h - c, w - c, h, w - r, h);
        path.lineTo(r, h);
        path.cubicTo(c, h, 0, h - c, 0, h - r);
        path.lineTo(0, r);
        path.cubicTo(0, c, c, 0, r, 0);
        path.close();
        return path;
    }
}
