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

    private static final int AUDIO_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

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

                    if (checkSelfPermission(
                            Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {

                        request.grant(new String[]{
                                PermissionRequest.RESOURCE_AUDIO_CAPTURE
                        });

                        startVoiceService();

                    } else {

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
     * Starts the Android foreground service.
     */
    private void startVoiceService() {

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        Intent intent =
                new Intent(this, VoiceService.class);

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(intent);

        } else {

            startService(intent);
        }
    }

    /*
     * Stops the Android foreground service.
     */
    private void stopVoiceService() {

        Intent intent =
                new Intent(this, VoiceService.class);

        stopService(intent);
    }

    /*
     * JavaScript bridge.
     *
     * room.html calls:
     *
     * AndroidVoice.voiceStarted()
     * AndroidVoice.voiceStopped()
     */
    private class AndroidVoiceBridge {

        @JavascriptInterface
        public void voiceStarted() {

            runOnUiThread(() -> {

                startVoiceService();

            });
        }

        @JavascriptInterface
        public void voiceStopped() {

            runOnUiThread(() -> {

                stopVoiceService();

            });
        }
    }

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

        if (requestCode ==
                AUDIO_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startVoiceService();

                if (webView != null) {
                    webView.reload();
                }
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