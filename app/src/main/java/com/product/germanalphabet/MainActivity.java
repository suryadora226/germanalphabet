package com.product.germanalphabet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * MainActivity
 * -------------
 * The single screen of the German Alphabet Kids App.
 *
 * Layout (top → bottom):
 *   ┌─────────────────────────────────┐
 *   │  Header: title + subtitle       │
 *   │  Progress bar  (0/30)           │
 *   │  Showcase panel                 │
 *   │    big letter | emoji | word    │
 *   │  6-column letter grid (30 tiles)│
 *   │  "Hear again" + "Start over"    │
 *   └─────────────────────────────────┘
 *
 * State is saved in SharedPreferences so tapped letters
 * survive app restarts (kids can pick up where they left off).
 */
public class MainActivity extends AppCompatActivity
        implements LetterTileView.OnTapListener {

    // ── TTS check request code ───────────────────────────────────────────────
    private static final int TTS_CHECK_CODE = 101;

    // ── Color palette (12 vivid kid-friendly colors cycling across 30 tiles) ─
    private static final int[] TILE_COLORS = {
            Color.parseColor("#E24B4A"),   // red
            Color.parseColor("#D85A30"),   // orange-red
            Color.parseColor("#EF9F27"),   // amber
            Color.parseColor("#639922"),   // green
            Color.parseColor("#1D9E75"),   // teal
            Color.parseColor("#378ADD"),   // blue
            Color.parseColor("#534AB7"),   // purple
            Color.parseColor("#D4537E"),   // pink
            Color.parseColor("#0F6E56"),   // dark teal
            Color.parseColor("#185FA5"),   // dark blue
            Color.parseColor("#993C1D"),   // burnt orange
            Color.parseColor("#3B6D11"),   // dark green
    };

    // ── Views ────────────────────────────────────────────────────────────────
    private ProgressBar     progressBar;
    private TextView        progressLabel;
    private FrameLayout     showcasePanel;
    private TextView        showcaseLetter;
    private TextView        showcaseEmoji;
    private TextView        showcaseWord;
    private TextView        showcasePronunciation;
    private TextView        showcaseMeaning;
    private View            hearAgainBtn;
    private View            congratsPanel;
    private LetterTileView[] tiles;

    // ── State ────────────────────────────────────────────────────────────────
    private SpeechHelper    speech;
    private int             tappedCount = 0;
    private LetterData      lastTapped  = null;
    private SharedPreferences prefs;

    private static final String PREFS_NAME    = "german_alphabet_prefs";
    private static final String KEY_TAPPED    = "tapped_";   // + index

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Assign tile colors to letter data
        for (int i = 0; i < LetterData.ALL.length; i++) {
            LetterData.ALL[i].tileColor = TILE_COLORS[i % TILE_COLORS.length];
        }

        buildUI();
        restoreSavedState();
        checkTTSAvailability();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speech != null) speech.shutdown();
    }

    // ── TTS setup ─────────────────────────────────────────────────────────────

    private void checkTTSAvailability() {
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, TTS_CHECK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TTS_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                initSpeech();
            } else {
                // TTS data missing — trigger install (offline pack downloads once)
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
                initSpeech(); // init anyway; will work once installed
            }
        }
    }

    private void initSpeech() {
        speech = new SpeechHelper(this, germanAvailable -> runOnUiThread(() -> {
            if (!germanAvailable) {
                new AlertDialog.Builder(this)
                        .setTitle("German Voice Missing")
                        .setMessage("Please install the German language pack in Settings → Text-to-Speech for the best experience.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }));
    }

    // ── UI Construction ──────────────────────────────────────────────────────

    private void buildUI() {

        // Root scroll view
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#FFF9F0"));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(20));

        // Header
        root.addView(buildHeader());
        root.addView(space(10));

        // Progress
        root.addView(buildProgressSection());
        root.addView(space(12));

        // Congrats panel (hidden initially)
        congratsPanel = buildCongratsPanel();
        congratsPanel.setVisibility(View.GONE);
        root.addView(congratsPanel);

        // Showcase panel
        showcasePanel = buildShowcasePanel();
        root.addView(showcasePanel);
        root.addView(space(12));

        // Letter grid
        root.addView(buildGrid());
        root.addView(space(14));

        // Bottom buttons
        root.addView(buildButtons());

        scroll.addView(root);
        setContentView(scroll);
    }

    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("Das Deutsche Alphabet");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#3C3489"));
        title.setGravity(Gravity.CENTER);

        TextView sub = new TextView(this);
        sub.setText("Tippe auf einen Buchstaben!  ·  Tap a letter!");
        sub.setTextSize(13);
        sub.setTextColor(Color.parseColor("#888780"));
        sub.setGravity(Gravity.CENTER);

        h.addView(title);
        h.addView(space(2));
        h.addView(sub);
        return h;
    }

    private View buildProgressSection() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(30);
        progressBar.setProgress(0);
        progressBar.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(10)));
        progressBar.getProgressDrawable().setColorFilter(
                Color.parseColor("#1D9E75"),
                android.graphics.PorterDuff.Mode.SRC_IN);
        progressBar.getProgressDrawable().setColorFilter(
                Color.parseColor("#1D9E75"),
                android.graphics.PorterDuff.Mode.SRC_IN);

        progressLabel = new TextView(this);
        progressLabel.setText("0 / 30 letters learned");
        progressLabel.setTextSize(12);
        progressLabel.setTextColor(Color.parseColor("#0F6E56"));
        progressLabel.setGravity(Gravity.END);

        col.addView(progressBar);
        col.addView(space(3));
        col.addView(progressLabel);
        return col;
    }

    private View buildCongratsPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(Color.parseColor("#EAF3DE"));
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        setRoundedBackground(panel, Color.parseColor("#EAF3DE"), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        panel.setLayoutParams(lp);

        TextView stars = new TextView(this);
        stars.setText("★ ★ ★ ★ ★");
        stars.setTextSize(24);
        stars.setTextColor(Color.parseColor("#BA7517"));
        stars.setGravity(Gravity.CENTER);

        TextView heading = new TextView(this);
        heading.setText("Wunderbar! You learned all 30 letters!");
        heading.setTextSize(16);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(Color.parseColor("#27500A"));
        heading.setGravity(Gravity.CENTER);

        TextView sub = new TextView(this);
        sub.setText("Fantastic work! You are a German alphabet superstar!");
        sub.setTextSize(13);
        sub.setTextColor(Color.parseColor("#3B6D11"));
        sub.setGravity(Gravity.CENTER);

        panel.addView(stars);
        panel.addView(space(4));
        panel.addView(heading);
        panel.addView(space(2));
        panel.addView(sub);
        return panel;
    }

    private FrameLayout buildShowcasePanel() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.WHITE);
        setRoundedBackground(frame, Color.WHITE, dp(14));
        frame.setMinimumHeight(dp(140));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(150));
        frame.setLayoutParams(lp);

        // Placeholder text
        TextView hint = new TextView(this);
        hint.setText("★  Tap any letter below to start!");
        hint.setTextSize(14);
        hint.setTextColor(Color.parseColor("#B4B2A9"));
        hint.setGravity(Gravity.CENTER);
        hint.setId(View.generateViewId());
        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        hlp.gravity = Gravity.CENTER;
        frame.addView(hint, hlp);

        // Content row (hidden until first tap)
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(16), dp(10));
        row.setVisibility(View.GONE);
        row.setId(View.generateViewId());
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        rlp.gravity = Gravity.CENTER_VERTICAL;
        row.setLayoutParams(rlp);

        showcaseLetter = new TextView(this);
        showcaseLetter.setTextSize(72);
        showcaseLetter.setTypeface(null, Typeface.BOLD);
        showcaseLetter.setTextColor(Color.parseColor("#534AB7"));
        showcaseLetter.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(90), dp(100));
        row.addView(showcaseLetter, llp);

        showcaseEmoji = new TextView(this);
        showcaseEmoji.setTextSize(44);
        showcaseEmoji.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(dp(70), dp(80));
        row.addView(showcaseEmoji, elp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(8), 0, 0, 0);

        showcaseWord = new TextView(this);
        showcaseWord.setTextSize(20);
        showcaseWord.setTypeface(null, Typeface.BOLD);
        showcaseWord.setTextColor(Color.parseColor("#2C2C2A"));

        showcasePronunciation = new TextView(this);
        showcasePronunciation.setTextSize(13);
        showcasePronunciation.setTextColor(Color.parseColor("#888780"));
        showcasePronunciation.setTypeface(null, Typeface.ITALIC);

        showcaseMeaning = new TextView(this);
        showcaseMeaning.setTextSize(13);
        showcaseMeaning.setTextColor(Color.parseColor("#5F5E5A"));

        info.addView(showcaseWord);
        info.addView(space(2));
        info.addView(showcasePronunciation);
        info.addView(space(2));
        info.addView(showcaseMeaning);

        row.addView(info);
        frame.addView(row, rlp);

        // Store row reference for toggling
        frame.setTag(new View[]{hint, row});
        return frame;
    }

    private View buildGrid() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(6);
        tiles = new LetterTileView[LetterData.ALL.length];

        int tileSize = calculateTileSize();

        for (int i = 0; i < LetterData.ALL.length; i++) {
            LetterTileView tile = new LetterTileView(this, LetterData.ALL[i]);
            tile.setOnTapListener(this);
            tiles[i] = tile;

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = tileSize;
            lp.height = tileSize;
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            grid.addView(tile, lp);
        }
        return grid;
    }

    private View buildButtons() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        hearAgainBtn = buildBtn("▶  Hear again", Color.WHITE, Color.parseColor("#534AB7"), false);
        hearAgainBtn.setVisibility(View.GONE);
        hearAgainBtn.setOnClickListener(v -> { if (speech != null) speech.replay(); });

        View resetBtn = buildBtn("↺  Start over", Color.parseColor("#534AB7"), Color.WHITE, true);
        resetBtn.setOnClickListener(v -> resetAll());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(dp(4), 0, dp(4), 0);
        row.addView(hearAgainBtn, lp);
        row.addView(resetBtn, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        return row;
    }

    private View buildBtn(String text, int bg, int fg, boolean solid) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(13);
        btn.setTextColor(solid ? Color.WHITE : fg);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(12), dp(10), dp(12), dp(10));
        setRoundedBackground(btn,
                solid ? Color.parseColor("#534AB7") : Color.WHITE, dp(8));
        btn.setClickable(true);
        return btn;
    }

    // ── Tap callback ──────────────────────────────────────────────────────────

    @Override
    public void onTap(LetterTileView view, LetterData data) {
        lastTapped = data;
        tappedCount++;

        // Speak
        if (speech != null) speech.speak(data);

        // Update showcase
        updateShowcase(data);

        // Progress
        updateProgress();

        // Save state
        int idx = indexOf(data);
        prefs.edit().putBoolean(KEY_TAPPED + idx, true).apply();

        // Show "Hear again"
        hearAgainBtn.setVisibility(View.VISIBLE);

        // Congrats at 30
        if (tappedCount == 30) showCongrats();
    }

    // ── Showcase ─────────────────────────────────────────────────────────────

    private void updateShowcase(LetterData data) {
        View[] views = (View[]) showcasePanel.getTag();
        View hint = views[0];
        View row  = views[1];

        hint.setVisibility(View.GONE);
        row.setVisibility(View.VISIBLE);

        // Animate background color change
        int bg = lighten(data.tileColor, 0.88f);
        showcasePanel.setBackgroundColor(bg);

        // Update text
        showcaseLetter.setText(data.letter);
        showcaseLetter.setTextColor(data.tileColor);
        showcaseEmoji.setText(data.emoji);
        showcaseWord.setText(data.germanWord);
        showcasePronunciation.setText("pronounced: \"" + data.pronunciation + "\"");
        showcaseMeaning.setText(data.englishMeaning);

        // Pop the letter
        showcaseLetter.setScaleX(0.5f);
        showcaseLetter.setScaleY(0.5f);
        showcaseLetter.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();
    }

    // ── Progress ─────────────────────────────────────────────────────────────

    private void updateProgress() {
        progressBar.setProgress(tappedCount);
        progressLabel.setText(tappedCount + " / 30 letters learned");
    }

    // ── Congrats ─────────────────────────────────────────────────────────────

    private void showCongrats() {
        congratsPanel.setVisibility(View.VISIBLE);
        congratsPanel.setAlpha(0f);
        congratsPanel.animate().alpha(1f).setDuration(500).start();
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    private void resetAll() {
        tappedCount = 0;
        lastTapped  = null;

        // Reset all tiles
        for (LetterTileView tile : tiles) tile.reset();

        // Reset showcase
        View[] views = (View[]) showcasePanel.getTag();
        views[0].setVisibility(View.VISIBLE);
        views[1].setVisibility(View.GONE);
        showcasePanel.setBackgroundColor(Color.WHITE);

        // Reset progress
        updateProgress();

        // Hide congrats + hear again
        congratsPanel.setVisibility(View.GONE);
        hearAgainBtn.setVisibility(View.GONE);

        // Clear saved state
        prefs.edit().clear().apply();

        if (speech != null) speech.stop();
    }

    // ── Restore saved state ──────────────────────────────────────────────────

    private void restoreSavedState() {
        for (int i = 0; i < LetterData.ALL.length; i++) {
            if (prefs.getBoolean(KEY_TAPPED + i, false)) {
                tiles[i].simulateTapSilent(); // restore visual without playing sound
                tappedCount++;
            }
        }
        updateProgress();
        if (tappedCount == 30) showCongrats();
        if (tappedCount > 0) hearAgainBtn.setVisibility(View.VISIBLE);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int calculateTileSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int padding = dp(14) * 2;
        int margins  = dp(3) * 2 * 6;
        return (screenWidth - padding - margins) / 6;
    }

    private int indexOf(LetterData data) {
        for (int i = 0; i < LetterData.ALL.length; i++) {
            if (LetterData.ALL[i] == data) return i;
        }
        return -1;
    }

    /** Creates a light pastel version of a color for the showcase background */
    private int lighten(int color, float factor) {
        int r = (int)(Color.red(color)   + (255 - Color.red(color))   * factor);
        int g = (int)(Color.green(color) + (255 - Color.green(color)) * factor);
        int b = (int)(Color.blue(color)  + (255 - Color.blue(color))  * factor);
        return Color.rgb(r, g, b);
    }

    private void setRoundedBackground(View v, int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(radius);
        v.setBackground(d);
    }

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(dp)));
        return v;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}