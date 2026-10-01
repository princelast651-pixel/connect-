package com.connect.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int AUDIO_PERMISSION_REQUEST = 1001;

    // WebView ka pending microphone request
    private PermissionRequest pendingPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // WebRTC ke liye
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        /*
         * JavaScript <-> Android bridge
         */
        webView.addJavascriptInterface(
                new AndroidVoiceBridge(),
                "AndroidVoice"
        );

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(
                    final PermissionRequest request) {

                runOnUiThread(() -> {

                    boolean audioRequested = false;

                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE
                                .equals(resource)) {
                            audioRequested = true;
                            break;
                        }
                    }

                    if (!audioRequested) {
                        request.deny();
                        return;
                    }

                    /*
                     * Android microphone permission already granted
                     */
                    if (checkSelfPermission(
                            Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {

                        grantAudioPermission(request);

                    } else {

                        /*
                         * Permission request save karo.
                         * Permission allow hone ke baad isi request
                         * ko grant kiya jayega.
                         */
                        pendingPermissionRequest = request;

                        requestPermissions(
                                new String[]{
                                        Manifest.permission.RECORD_AUDIO
                                },
                                AUDIO_PERMISSION_REQUEST
                        );
                    }
                });
            }
        });

        webView.loadUrl(
                "https://princelast651-pixel.github.io/connect-/"
        );
    }

    /*
     * WebView ko microphone permission actually grant karta hai
     */
    private void grantAudioPermission(
            PermissionRequest request) {

        if (request == null) {
            return;
        }

        try {

            request.grant(new String[]{
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE
            });

            startVoiceService();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /*
     * Foreground voice service
     */
    private void startVoiceService() {

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        Intent intent =
                new Intent(this, VoiceService.class);

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void stopVoiceService() {

        try {

            Intent intent =
                    new Intent(this, VoiceService.class);

            stopService(intent);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /*
     * JavaScript bridge
     */
    private class AndroidVoiceBridge {

        @JavascriptInterface
        public void voiceStarted() {

            runOnUiThread(() -> {

                if (checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {

                    startVoiceService();
                }
            });
        }

        @JavascriptInterface
        public void voiceStopped() {

            runOnUiThread(() -> {

                stopVoiceService();
            });
        }
    }

    /*
     * Android permission result
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode != AUDIO_PERMISSION_REQUEST) {
            return;
        }

        if (grantResults.length > 0 &&
                grantResults[0]
                        == PackageManager.PERMISSION_GRANTED) {

            /*
             * IMPORTANT:
             * Ab wahi pending WebView request grant hoga.
             */
            if (pendingPermissionRequest != null) {

                grantAudioPermission(
                        pendingPermissionRequest
                );

                pendingPermissionRequest = null;

            } else {

                startVoiceService();
            }

        } else {

            if (pendingPermissionRequest != null) {

                try {
                    pendingPermissionRequest.deny();
                } catch (Exception ignored) {
                }

                pendingPermissionRequest = null;
            }
        }
    }

    @Override
    protected void onDestroy() {

        stopVoiceService();

        if (webView != null) {

            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}