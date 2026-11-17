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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeout;
    private SpeechRecognizer recognizer;

    VoiceInput(Context context) {
        this.context = context;
    }

    void listen(Consumer<String> result, Consumer<String> error) {
        close();
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            error.accept(context.getString(R.string.recognizer_missing));
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        SpeechRecognizer session = recognizer;
        timeout = () -> {
            if (recognizer != session) return;
            close();
            error.accept(context.getString(R.string.voice_timeout));
        };
        handler.postDelayed(timeout, 12000);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle extras) {}
            @Override public void onBeginningOfSpeech() {
                if (recognizer == session && timeout != null) {
                    handler.removeCallbacks(timeout);
                    handler.postDelayed(timeout, 12000);
                }
            }
            @Override public void onRmsChanged(float rms) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle extras) {}
            @Override public void onEvent(int type, Bundle extras) {}
            @Override public void onError(int code) {
                if (recognizer != session) return;
                close();
                int message = switch (code) {
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.permission_hint;
                    case SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.voice_network;
                    case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                         SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> R.string.voice_language;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_SERVER,
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
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("vi") ? "vi-VN" : "en-US");
        try {
            session.startListening(request);
            if (android.os.Build.VERSION.SDK_INT >= 33
                    && "vi-VN".equals(request.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))) {
                session.checkRecognitionSupport(request, context.getMainExecutor(),
                        new android.speech.RecognitionSupportCallback() {
                            @Override public void onSupportResult(android.speech.RecognitionSupport support) {
                                if (recognizer == session
                                        && support.getSupportedOnDeviceLanguages().contains("vi-VN")
                                        && !support.getInstalledOnDeviceLanguages().contains("vi-VN")
                                        && !support.getPendingOnDeviceLanguages().contains("vi-VN")) {
                                    session.triggerModelDownload(request);
                                }
                            }

                            @Override public void onError(int code) {}
                        });
            }
        } catch (RuntimeException failure) {
            close();
            error.accept(context.getString(R.string.recognizer_missing));
        }
    }

    static String command(String phrase) { return CommandParser.command(phrase); }

    void close() {
        if (timeout != null) {
            handler.removeCallbacks(timeout);
            timeout = null;
        }
        if (recognizer != null) {
            SpeechRecognizer active = recognizer;
            recognizer = null;
            active.destroy();
        }
    }
}
