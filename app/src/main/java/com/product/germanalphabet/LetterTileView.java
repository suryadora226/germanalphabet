package com.product.germanalphabet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * LetterTileView
 * ---------------
 * A single alphabet tile. States:
 *
 *   IDLE   → white background, dark letter, tappable
 *   TAPPED → colored background, white letter, locked (no re-tap)
 *
 * On tap:
 *   1. Scale pop  (scale 1 → 1.4 → 1  via OvershootInterpolator)
 *   2. Burst ring (expanding + fading circle drawn on Canvas)
 *   3. Background flood-fill from white to tileColor
 *   4. Letter color cross-fades from dark to white
 *   5. Small ✓ badge fades in at bottom-right
 */
public class LetterTileView extends View {

    // ── State ────────────────────────────────────────────────────────────────
    private boolean isTapped = false;

    // ── Data ─────────────────────────────────────────────────────────────────
    private LetterData data;

    // ── Paints ───────────────────────────────────────────────────────────────
    private final Paint bgPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint letterPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint burstPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Animation state ──────────────────────────────────────────────────────
    private float burstRadius  = 0f;
    private float burstAlpha   = 0f;
    private float checkAlpha   = 0f;
    private float bgColorFraction = 0f;   // 0=white → 1=tileColor
    private int   currentBgColor  = Color.WHITE;
    private int   currentLetterColor;

    private static final int IDLE_LETTER_COLOR  = Color.parseColor("#444441");
    private static final int TAPPED_LETTER_COLOR = Color.WHITE;

    // ── Geometry ─────────────────────────────────────────────────────────────
    private final RectF bgRect = new RectF();
    private float cornerRadius;

    // ── Listener ─────────────────────────────────────────────────────────────
    public interface OnTapListener {
        void onTap(LetterTileView view, LetterData data);
    }
    private OnTapListener listener;

    // ── Constructor ──────────────────────────────────────────────────────────

    public LetterTileView(Context context, LetterData data) {
        super(context);
        this.data = data;
        currentLetterColor = IDLE_LETTER_COLOR;
        setupPaints();
        setClickable(true);
        setOnClickListener(v -> handleTap());
    }

    private void setupPaints() {
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.WHITE);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        borderPaint.setColor(Color.parseColor("#D3D1C7"));

        letterPaint.setStyle(Paint.Style.FILL);
        letterPaint.setTextAlign(Paint.Align.CENTER);
        letterPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        letterPaint.setColor(IDLE_LETTER_COLOR);

        checkPaint.setStyle(Paint.Style.FILL);
        checkPaint.setTextAlign(Paint.Align.RIGHT);
        checkPaint.setColor(Color.WHITE);
        checkPaint.setAlpha(0);

        burstPaint.setStyle(Paint.Style.STROKE);
        burstPaint.setStrokeWidth(dpToPx(3f));
    }

    // ── Tap handler ──────────────────────────────────────────────────────────

    private void handleTap() {
        if (isTapped) return;   // locked after first tap
        isTapped = true;

        if (listener != null) listener.onTap(this, data);

        runTapAnimation();
    }

    private void runTapAnimation() {
        // 1. Pop scale
        AnimatorSet popSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.35f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.35f, 1f);
        scaleX.setDuration(320);
        scaleY.setDuration(320);
        scaleX.setInterpolator(new OvershootInterpolator(3f));
        scaleY.setInterpolator(new OvershootInterpolator(3f));
        popSet.playTogether(scaleX, scaleY);
        popSet.start();

        // 2. Burst ring
        ValueAnimator burst = ValueAnimator.ofFloat(0f, 1f);
        burst.setDuration(420);
        burst.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            burstRadius = getWidth() * 0.6f * f;
            burstAlpha  = 1f - f;
            burstPaint.setColor(data.tileColor);
            burstPaint.setAlpha((int)(burstAlpha * 200));
            invalidate();
        });
        burst.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                burstRadius = 0; burstAlpha = 0; invalidate();
            }
        });
        burst.start();

        // 3. Background color flood
        ValueAnimator bgAnim = ValueAnimator.ofFloat(0f, 1f);
        bgAnim.setDuration(350);
        bgAnim.addUpdateListener(anim -> {
            bgColorFraction = (float) anim.getAnimatedValue();
            currentBgColor  = blendColors(Color.WHITE, data.tileColor, bgColorFraction);
            currentLetterColor = blendColors(IDLE_LETTER_COLOR, TAPPED_LETTER_COLOR, bgColorFraction);
            invalidate();
        });
        bgAnim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                bgPaint.setColor(data.tileColor);
                borderPaint.setColor(data.tileColor);
            }
        });
        bgAnim.start();

        // 4. Check mark fade in (after pop)
        ValueAnimator checkAnim = ValueAnimator.ofFloat(0f, 1f);
        checkAnim.setStartDelay(280);
        checkAnim.setDuration(200);
        checkAnim.addUpdateListener(anim -> {
            checkAlpha = (float) anim.getAnimatedValue();
            invalidate();
        });
        checkAnim.start();
    }

    // ── Drawing ──────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        cornerRadius = Math.min(w, h) * 0.18f;
        bgRect.set(dpToPx(1), dpToPx(1), w - dpToPx(1), h - dpToPx(1));

        // Scale text to fit tile
        float targetSize = w * 0.48f;
        letterPaint.setTextSize(targetSize);

        float checkSize = w * 0.20f;
        checkPaint.setTextSize(checkSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;

        // Background
        bgPaint.setColor(currentBgColor);
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint);

        // Border (hidden when tapped)
        if (!isTapped || bgColorFraction < 1f) {
            borderPaint.setColor(
                    blendColors(Color.parseColor("#D3D1C7"), data.tileColor, bgColorFraction)
            );
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint);
        }

        // Burst ring
        if (burstRadius > 0) {
            canvas.drawCircle(cx, cy, burstRadius, burstPaint);
        }

        // Letter
        letterPaint.setColor(currentLetterColor);
        Paint.FontMetrics fm = letterPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(data.letter, cx, textY, letterPaint);

        // Check mark
        if (checkAlpha > 0) {
            checkPaint.setAlpha((int)(checkAlpha * 200));
            canvas.drawText("✓", w - dpToPx(6), h - dpToPx(6), checkPaint);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public void setOnTapListener(OnTapListener l) { this.listener = l; }

    public boolean isTapped() { return isTapped; }

    /**
     * Restore tapped visual state silently (no animation, no sound).
     * Called on app restart to show previously tapped letters.
     */
    public void simulateTapSilent() {
        isTapped = true;
        bgColorFraction    = 1f;
        currentBgColor     = data.tileColor;
        currentLetterColor = TAPPED_LETTER_COLOR;
        checkAlpha         = 1f;
        bgPaint.setColor(data.tileColor);
        borderPaint.setColor(data.tileColor);
        invalidate();
    }

    /** Reset to idle state (used by "Start over" button) */
    public void reset() {
        isTapped = false;
        burstRadius = 0; burstAlpha = 0; checkAlpha = 0; bgColorFraction = 0;
        currentBgColor     = Color.WHITE;
        currentLetterColor = IDLE_LETTER_COLOR;
        bgPaint.setColor(Color.WHITE);
        borderPaint.setColor(Color.parseColor("#D3D1C7"));
        setScaleX(1f); setScaleY(1f);
        invalidate();
    }

    private static int blendColors(int from, int to, float fraction) {
        float r = Color.red(from)   + fraction * (Color.red(to)   - Color.red(from));
        float g = Color.green(from) + fraction * (Color.green(to) - Color.green(from));
        float b = Color.blue(from)  + fraction * (Color.blue(to)  - Color.blue(from));
        return Color.rgb((int)r, (int)g, (int)b);
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
}