package br.com.mgltechnology.vision;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "mgl_vision_settings";
    private static final int FILE_REQUEST = 300;
    private static final int MEDIA_REQUEST = 301;
    private final int navy = Color.rgb(11, 35, 67);
    private final int blue = Color.rgb(25, 118, 232);
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingMediaRequest;
    private String origin;
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        String saved = getPreferences().getString("origin", "");
        if (saved.isEmpty()) showSettings(); else openPanel(saved);
    }

    private SharedPreferences getPreferences() { return getSharedPreferences(PREFS, MODE_PRIVATE); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        return t;
    }
    private int dp(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }
    private void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(25), dp(20), dp(18));
        root.setBackgroundColor(Color.rgb(245, 248, 253));
        setContentView(root);
    }
    private void showSettings() {
        if (webView != null) { webView.destroy(); webView = null; }
        base();
        TextView title = text("MGL Vision", 30, navy);
        title.setTypeface(null, 1); root.addView(title);
        TextView subtitle = text("MONITORAMENTO EM NUVEM", 12, blue);
        root.addView(subtitle);
        TextView info = text("Conectar ao servidor", 22, navy);
        LinearLayout.LayoutParams spacing = new LinearLayout.LayoutParams(-1, -2);
        spacing.topMargin = dp(42); root.addView(info, spacing);
        root.addView(text("Endereço ou IP", 14, navy));
        EditText host = new EditText(this);
        host.setSingleLine(true); host.setHint("mgltech.duckdns.org ou 192.168.1.10");
        host.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(host);
        root.addView(text("Porta", 14, navy));
        EditText port = new EditText(this);
        port.setSingleLine(true); port.setHint("18443");
        port.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(port);
        String saved = getPreferences().getString("origin", "https://mgltech.duckdns.org:18443");
        try { Uri parsed = Uri.parse(saved); host.setText(parsed.getHost()); port.setText(parsed.getPort() == -1 ? "443" : String.valueOf(parsed.getPort())); }
        catch (Exception ignored) { host.setText("mgltech.duckdns.org"); port.setText("18443"); }
        Button connect = new Button(this);
        connect.setText("CONECTAR AO PAINEL"); connect.setTextColor(Color.WHITE);
        connect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(54));
        buttonParams.topMargin = dp(24); root.addView(connect, buttonParams);
        connect.setOnClickListener(v -> {
            String h = host.getText().toString().trim();
            String p = port.getText().toString().trim();
            if (h.isEmpty() || p.isEmpty()) { toast("Informe o endereço e a porta."); return; }
            String scheme = "https";
            if (h.startsWith("http://")) { scheme = "http"; h = h.substring(7); }
            else if (h.startsWith("https://")) h = h.substring(8);
            while (h.endsWith("/")) h = h.substring(0, h.length() - 1);
            if (h.contains("/") || h.contains("@") || h.contains(":") && !h.startsWith("[")) { toast("Informe somente o domínio ou IP, sem caminho ou porta."); return; }
            int number;
            try { number = Integer.parseInt(p); } catch (NumberFormatException ex) { toast("Porta inválida."); return; }
            if (number < 1 || number > 65535) { toast("A porta deve estar entre 1 e 65535."); return; }
            String url = scheme + "://" + h + ":" + number;
            if (scheme.equals("http")) {
                new AlertDialog.Builder(this).setTitle("Conexão sem criptografia")
                    .setMessage("HTTP não protege suas credenciais nem oferece suporte completo a recursos de câmera e microfone. Prefira HTTPS.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Continuar", (d, w) -> saveAndOpen(url)).show();
            } else saveAndOpen(url);
        });
        TextView hint = text("Use HTTPS com certificado válido. Você poderá trocar de servidor pelo menu do aplicativo.", 13, Color.DKGRAY);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(20); root.addView(hint, hp);
    }
    private void saveAndOpen(String url) {
        getPreferences().edit().putString("origin", url).apply(); openPanel(url);
    }
    private void openPanel(String url) {
        origin = url;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(navy);
        setContentView(root);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), 0, dp(10), 0);
        toolbar.setBackgroundColor(navy);
        TextView brand = text("MGL Vision", 19, Color.WHITE);
        brand.setTypeface(null, 1);
        toolbar.addView(brand, new LinearLayout.LayoutParams(0, dp(52), 1));
        brand.setGravity(Gravity.CENTER_VERTICAL);
        Button refresh = new Button(this); refresh.setText("↻"); refresh.setTextColor(Color.WHITE);
        toolbar.addView(refresh, new LinearLayout.LayoutParams(dp(50), dp(48)));
        Button change = new Button(this); change.setText("SERVIDOR"); change.setTextSize(10); change.setTextColor(Color.WHITE);
        toolbar.addView(change, new LinearLayout.LayoutParams(dp(92), dp(48)));
        root.addView(toolbar);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String target = uri.getScheme();
                if ("http".equals(target) || "https".equals(target)) {
                    if (uri.getHost() != null && Uri.parse(origin).getHost().equalsIgnoreCase(uri.getHost())) return false;
                    startActivity(new Intent(Intent.ACTION_VIEW, uri)); return true;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { toast("Não foi possível abrir o link."); }
                return true;
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError error) {
                if (req.isForMainFrame()) toast("Não foi possível acessar o servidor. Confira endereço, porta e rede.");
            }
            // Do not bypass TLS certificate validation.
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (!request.getOrigin().getScheme().equals("https") ||
                        !Uri.parse(origin).getHost().equalsIgnoreCase(request.getOrigin().getHost())) { request.deny(); return; }
                    pendingMediaRequest = request;
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, MEDIA_REQUEST);
                    } else grantMedia(request);
                });
            }
            @Override public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingMediaRequest == request) pendingMediaRequest = null;
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_REQUEST); return true; }
                catch (Exception ex) { fileCallback = null; toast("Seletor de arquivos indisponível."); return false; }
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        refresh.setOnClickListener(v -> webView.reload());
        change.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Trocar servidor")
            .setMessage("Deseja voltar à configuração do endereço e da porta?")
            .setNegativeButton("Cancelar", null).setPositiveButton("Configurar", (d, w) -> showSettings()).show());
        webView.loadUrl(origin);
    }
    private void grantMedia(PermissionRequest request) {
        java.util.ArrayList<String> approved = new java.util.ArrayList<>();
        for (String resource : request.getResources()) {
            if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE) && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) approved.add(resource);
            if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE) && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) approved.add(resource);
            if (resource.equals(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) approved.add(resource);
        }
        if (approved.isEmpty()) request.deny(); else request.grant(approved.toArray(new String[0]));
        pendingMediaRequest = null;
    }
    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == MEDIA_REQUEST && pendingMediaRequest != null) grantMedia(pendingMediaRequest);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCallback = null;
        }
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else if (webView != null) new AlertDialog.Builder(this).setTitle("MGL Vision")
            .setMessage("Sair do aplicativo?").setNegativeButton("Cancelar", null)
            .setPositiveButton("Sair", (d, w) -> finish()).show();
        else super.onBackPressed();
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override protected void onDestroy() {
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
