package com.product.germanalphabet;


import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

/**
 * SpeechHelper
 * -------------
 * Wraps Android's built-in TextToSpeech engine for 100% offline playback.
 *
 * Sequence for each letter tap:
 *   1. Speak the letter name in German   (e.g. "Ah")       lang=de-DE
 *   2. After finish → speak German word  (e.g. "Apfel")    lang=de-DE
 *   3. After finish → speak English word (e.g. "apple")    lang=en-US
 *
 * Offline requirement:
 *   Android's TTS engine (Google TTS or Samsung TTS) must have the
 *   German language pack downloaded on the device. This is done once
 *   automatically when the user first installs. No internet needed
 *   after that. We check + prompt if it's missing (see checkLanguage()).
 *
 * Usage:
 *   SpeechHelper tts = new SpeechHelper(context);
 *   tts.speak(letterData);
 *   // in onDestroy():
 *   tts.shutdown();
 */
public class SpeechHelper {

    private static final String TAG = "SpeechHelper";

    private TextToSpeech tts;
    private boolean isReady = false;

    private static final Locale GERMAN  = Locale.GERMAN;
    private static final Locale ENGLISH = Locale.ENGLISH;

    // Utterance IDs for chaining
    private static final String UTT_LETTER  = "UTT_LETTER";
    private static final String UTT_WORD_DE = "UTT_WORD_DE";
    private static final String UTT_WORD_EN = "UTT_WORD_EN";

    // Pending data when speak() is called before TTS is ready
    private LetterData pendingData = null;

    public interface ReadyListener {
        void onReady(boolean germanAvailable);
    }

    public SpeechHelper(Context context, ReadyListener readyListener) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isReady = true;
                boolean germanOk = checkLanguage(GERMAN);
                setupChaining();
                if (readyListener != null) readyListener.onReady(germanOk);
                if (pendingData != null) {
                    speak(pendingData);
                    pendingData = null;
                }
            } else {
                Log.e(TAG, "TTS init failed with status: " + status);
                if (readyListener != null) readyListener.onReady(false);
            }
        });
    }

    /**
     * Speaks letter → German word → English word in sequence.
     * Safe to call before TTS is ready (queued internally).
     */
    public void speak(LetterData data) {
        if (!isReady) { pendingData = data; return; }
        tts.stop();

        // Speak letter name in German
        speakText(data.pronunciation, GERMAN, UTT_LETTER);

        // German word and English word are chained via UtteranceProgressListener
        // We store current data in a tag on the TTS engine via a small holder
        currentData = data;
    }

    private LetterData currentData = null;

    /** Set language — returns true if fully supported offline */
    private boolean checkLanguage(Locale locale) {
        int result = tts.isLanguageAvailable(locale);
        return result == TextToSpeech.LANG_AVAILABLE
                || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
                || result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
    }

    /**
     * Chains letter → word (de) → word (en) using UtteranceProgressListener.
     * onDone fires after each utterance; we queue the next one there.
     */
    private void setupChaining() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if (currentData == null) return;
                if (UTT_LETTER.equals(utteranceId)) {
                    // Letter name done → speak German word
                    speakText(currentData.germanWord, GERMAN, UTT_WORD_DE);
                } else if (UTT_WORD_DE.equals(utteranceId)) {
                    // German word done → speak English meaning
                    speakText(currentData.englishMeaning, ENGLISH, UTT_WORD_EN);
                } else if (UTT_WORD_EN.equals(utteranceId)) {
                    currentData = null;
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "TTS error on utterance: " + utteranceId);
            }
        });
    }

    private void speakText(String text, Locale locale, String utteranceId) {
        tts.setLanguage(locale);
        tts.setSpeechRate(0.78f);   // slightly slower for kids
        tts.setPitch(1.1f);         // slightly higher pitch, friendlier

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId);
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(text, TextToSpeech.QUEUE_ADD, params);
        }
    }

    /** Replay the last spoken letter. Call from "Hear again" button. */
    public void replay() {
        if (currentData != null) speak(currentData);
    }

    /** Stop any ongoing speech. */
    public void stop() {
        if (tts != null) tts.stop();
    }

    /** Call in Activity.onDestroy() to release TTS resources. */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
