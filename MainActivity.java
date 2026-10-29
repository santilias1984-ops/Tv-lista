package es.guiatv.espana;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import android.util.Base64;

/**
 * Guía TV España: contenedor Android independiente sin servicios de terceros.
 * La interfaz en assets/index.html se ejecuta localmente. Internet se usa solo
 * para descargar XMLTV mediante el puente nativo y cargar posibles logos.
 */
public class MainActivity extends Activity {
    private static final int PICK_XML = 812;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 16, 32));
        getWindow().setNavigationBarColor(Color.rgb(11, 16, 32));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 16, 32));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.addJavascriptInterface(new GuideBridge(), "AndroidEPG");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equalsIgnoreCase(uri.getScheme())) return false;
                if ("https".equalsIgnoreCase(uri.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view,
                ValueCallback<Uri[]> filePathCallback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try { startActivityForResult(Intent.createChooser(intent, "Importar guía XMLTV"), PICK_XML); }
                catch (Exception ex) { fileCallback.onReceiveValue(null); fileCallback = null; return false; }
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class GuideBridge {
        @JavascriptInterface public void fetchUrl(String rawUrl) {
            io.execute(() -> {
                String error = null;
                String encoded = null;
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(rawUrl);
                    if (!"https".equalsIgnoreCase(url.getProtocol()))
                        throw new IllegalArgumentException("Se necesita una URL HTTPS");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(25000);
                    conn.setRequestProperty("Accept", "application/xml, text/xml, */*");
                    conn.setRequestProperty("Accept-Encoding", "gzip");
                    conn.setInstanceFollowRedirects(true);
                    int status = conn.getResponseCode();
                    if (status < 200 || status >= 300) throw new IllegalStateException("Error HTTP " + status);
                    InputStream stream = conn.getInputStream();
                    if ("gzip".equalsIgnoreCase(conn.getContentEncoding()) || rawUrl.toLowerCase(Locale.ROOT).endsWith(".gz"))
                        stream = new GZIPInputStream(stream);
                    byte[] data;
                    try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[16384];
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            if (out.size() + n > 15 * 1024 * 1024)
                                throw new IllegalStateException("XMLTV demasiado grande (máximo 15 MB)");
                            out.write(buffer, 0, n);
                        }
                        data = out.toByteArray();
                    }
                    // El fichero con gzip puede venir sin cabecera HTTP gzip.
                    if (data.length > 2 && (data[0] & 0xff) == 31 && (data[1] & 0xff) == 139) {
                        try (InputStream in = new GZIPInputStream(new java.io.ByteArrayInputStream(data));
                             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                            byte[] buffer = new byte[16384]; int n;
                            while ((n = in.read(buffer)) != -1) {
                                if (out.size() + n > 15 * 1024 * 1024) throw new IllegalStateException("Guía descomprimida demasiado grande");
                                out.write(buffer, 0, n);
                            }
                            data = out.toByteArray();
                        }
                    }
                    encoded = Base64.encodeToString(data, Base64.NO_WRAP);
                } catch (Exception ex) {
                    error = ex.getMessage() == null ? "Error de red" : ex.getMessage();
                } finally { if (conn != null) conn.disconnect(); }
                final String js = "window.nativeGuideResult(" + JSONObject.quote(rawUrl) + "," +
                    (encoded == null ? "null" : JSONObject.quote(encoded)) + "," +
                    (error == null ? "null" : JSONObject.quote(error)) + ");";
                runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(js, null); });
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_XML && fileCallback != null) {
            Uri[] value = resultCode == RESULT_OK && data != null && data.getData() != null
                ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(value);
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView == null) { super.onBackPressed(); return; }
        webView.evaluateJavascript("window.handleAppBack && window.handleAppBack()", value -> {
            if (!"true".equals(value)) {
                new AlertDialog.Builder(this).setTitle("Salir de Guía TV España")
                    .setMessage("¿Quieres cerrar la aplicación?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Salir", (dialog, which) -> finish())
                    .show();
            }
        });
    }
    @Override protected void onDestroy() {
        io.shutdownNow();
        if (webView != null) { webView.removeJavascriptInterface("AndroidEPG"); webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
