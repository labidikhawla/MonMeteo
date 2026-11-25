package com.example.monmeteo;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        final WebView webView = findViewById(R.id.myWebView);
        String url = getIntent().getStringExtra("url");
        if (url == null) url = "https://api.open-meteo.com/v1/forecast?latitude=35.0&longitude=9.75&current=temperature_2m";
        // loadUrl() : page distante, contrairement à loadData() qui affiche un HTML fourni par l'app.
        webView.loadUrl(url);

        Button btnRetour = findViewById(R.id.btnRetourDoc);
        btnRetour.setOnClickListener(v -> finish());
    }
}
