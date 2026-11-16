package vn.jovi.assistant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;

final class VoiceInput {
    private final Context context;
    private SpeechRecognizer recognizer;

    VoiceInput(Context context) {
        this.context = context;
    }

    void listen(Consumer<String> result, Consumer<String> error) {
        close();
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error.accept("Máy chưa có dịch vụ nhận dạng giọng nói.");
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle extras) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rms) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle extras) {}
            @Override public void onEvent(int type, Bundle extras) {}
            @Override public void onError(int code) {
                error.accept("Chưa nhận được lệnh. Bạn thử lại nhé.");
            }
            @Override public void onResults(Bundle extras) {
                ArrayList<String> phrases = extras.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (phrases == null || phrases.isEmpty()) {
                    error.accept("Chưa nhận được lệnh. Bạn thử lại nhé.");
                } else {
                    result.accept(phrases.get(0));
                }
            }
        });
        Intent request = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        recognizer.startListening(request);
    }

    static String command(String phrase) {
        return phrase.toLowerCase(Locale.ROOT).trim()
                .replaceFirst("^jovi(?:[\\s,]+|$)", "").replaceFirst("[.!?]+$", "").trim();
    }

    void close() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }
}
