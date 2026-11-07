package com.example.naviglas.testsupport;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/** Runs in the test APK process, which intentionally has no app/Kotlin runtime dependency. */
public final class ScriptedRecognitionService extends RecognitionService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Override protected void onStartListening(Intent intent, Callback callback) {
        String text = intent.getStringExtra("script.text");
        int error = intent.getIntExtra("script.error", -1);
        long delay = intent.getLongExtra("script.delay", 200);
        try { callback.readyForSpeech(new Bundle()); }
        catch (Exception e) { return; }
        if (text != null || error >= 0) handler.postDelayed(() -> {
            try {
                if (error >= 0) callback.error(error);
                else {
                    Bundle result = new Bundle();
                    ArrayList<String> values = new ArrayList<>();
                    values.add(text);
                    result.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, values);
                    callback.results(result);
                }
            } catch (Exception ignored) { }
        }, delay);
    }
    @Override protected void onCancel(Callback callback) { handler.removeCallbacksAndMessages(null); }
    @Override protected void onStopListening(Callback callback) {
        handler.removeCallbacksAndMessages(null);
        try { callback.error(SpeechRecognizer.ERROR_NO_MATCH); } catch (Exception ignored) { }
    }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}
