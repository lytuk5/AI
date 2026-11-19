package vn.jovi.assistant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

import java.util.function.Consumer;

final class VoiceInput {
    private final Context context;
    private final boolean appCommands;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeout;
    private SpeechRecognizer recognizer;
    private VietnameseVoiceInput vietnamese;

    VoiceInput(Context context) {
        this(context, context instanceof MainActivity);
    }

    VoiceInput(Context context, boolean appCommands) {
        this.context = context;
        this.appCommands = appCommands;
    }

    void listen(Runnable ready, Consumer<String> result, Consumer<String> error) {
        listen(ready, null, result, error);
    }

    void listen(Runnable ready, Consumer<String> partial, Consumer<String> result, Consumer<String> error) {
        close();
        if (context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("vi")) {
            vietnamese = new VietnameseVoiceInput(context, handler, appCommands, ready, partial, result, error);
            vietnamese.start();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error.accept(context.getString(R.string.recognizer_missing));
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        SpeechRecognizer session = recognizer;
        WakeService.pauseForCommand();
        timeout = () -> {
            if (recognizer != session) return;
            close();
            error.accept(context.getString(R.string.voice_timeout));
        };
        handler.postDelayed(timeout, 12000);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle extras) {
                if (recognizer == session) ready.run();
            }
            @Override public void onBeginningOfSpeech() {
                if (recognizer == session && timeout != null) {
                    handler.removeCallbacks(timeout);
                    handler.postDelayed(timeout, 12000);
                }
            }
            @Override public void onRmsChanged(float rms) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle extras) {
                if (recognizer != session || partial == null || extras == null) return;
                var phrases = extras.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (phrases != null && !phrases.isEmpty()) partial.accept(phrases.get(0));
            }
            @Override public void onEvent(int type, Bundle extras) {}
            @Override public void onError(int code) {
                if (recognizer != session) return;
                close();
                android.util.Log.w("Jovi", "Speech recognition error: " + code);
                int message = switch (code) {
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.permission_hint;
                    case SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.voice_network;
                    case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                         SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> R.string.voice_language;
                    case SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                         SpeechRecognizer.ERROR_SERVER,
                         SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> R.string.voice_busy;
                    default -> R.string.no_command;
                };
                error.accept(context.getString(message));
            }
            @Override public void onResults(Bundle extras) {
                if (recognizer != session) return;
                close();
                ArrayList<String> phrases = extras.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (phrases == null || phrases.isEmpty()) {
                    error.accept(context.getString(R.string.no_command));
                } else {
                    result.accept(phrases.get(0));
                }
            }
        });
        Intent request = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partial != null);
        WakeService.whenMicrophoneReleased(handler, () -> recognizer == session,
                () -> startRecognition(session, request, error), () -> {
                    close();
                    error.accept(context.getString(R.string.voice_busy));
                });
    }

    private void startRecognition(SpeechRecognizer session, Intent request, Consumer<String> error) {
        try {
            session.startListening(request);
        } catch (RuntimeException failure) {
            close();
            android.util.Log.w("Jovi", "Could not start speech recognition", failure);
            error.accept(context.getString(R.string.voice_busy));
        }
    }

    static String command(String phrase) { return CommandParser.command(phrase); }

    void close() {
        if (vietnamese != null) {
            vietnamese.cancel();
            vietnamese = null;
        }
        if (timeout != null) {
            handler.removeCallbacks(timeout);
            timeout = null;
        }
        if (recognizer != null) {
            SpeechRecognizer active = recognizer;
            recognizer = null;
            try {
                active.destroy();
            } finally {
                WakeService.resumeWake();
            }
        }
    }
}
