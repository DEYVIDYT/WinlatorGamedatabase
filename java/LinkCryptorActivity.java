package com.winlator.Download; // Assuming this is the correct package

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
// Using EditText for simplicity, cast from TextInputEditText
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors; // Added
import com.google.android.material.textfield.TextInputEditText; // For direct use if preferred

public class LinkCryptorActivity extends AppCompatActivity {

    private TextInputEditText etInputLink;
    private TextInputEditText etBase64Link;
    private Button btnEncrypt;
    private Button btnDecrypt;
    private TextView tvResultLink;
    private Button btnShareResult;
    private Button btnCopyResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this); // Added
        setContentView(R.layout.activity_link_cryptor);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("Link Cryptor/Decryptor");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        etInputLink = findViewById(R.id.et_input_link);
        etBase64Link = findViewById(R.id.et_base64_link);
        btnEncrypt = findViewById(R.id.btn_encrypt);
        btnDecrypt = findViewById(R.id.btn_decrypt);
        tvResultLink = findViewById(R.id.tv_result_link);
        btnShareResult = findViewById(R.id.btn_share_result);
        btnCopyResult = findViewById(R.id.btn_copy_result);

        btnEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptLink();
            }
        });

        btnDecrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptLink();
            }
        });

        btnShareResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResult();
            }
        });

        btnCopyResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyResultToClipboard();
            }
        });
    }

    private void encryptLink() {
        String originalLink = etInputLink.getText().toString().trim();
        if (originalLink.isEmpty()) {
            Toast.makeText(this, "Por favor, insira um link original.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = originalLink.getBytes("UTF-8");
            String base64Encoded = Base64.encodeToString(data, Base64.DEFAULT);
            tvResultLink.setText(base64Encoded);
            etBase64Link.setText(base64Encoded); // Also populate the other field
        } catch (java.io.UnsupportedEncodingException e) {
            Toast.makeText(this, "Erro de codificação.", Toast.LENGTH_SHORT).show();
            tvResultLink.setText("");
        }
    }

    private void decryptLink() {
        String base64Link = etBase64Link.getText().toString().trim();
        if (base64Link.isEmpty()) {
            Toast.makeText(this, "Por favor, insira um link em Base64.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] decodedBytes = Base64.decode(base64Link, Base64.DEFAULT);
            String decodedString = new String(decodedBytes, "UTF-8");
            tvResultLink.setText(decodedString);
            etInputLink.setText(decodedString); // Also populate the other field
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Entrada Base64 inválida.", Toast.LENGTH_SHORT).show();
            tvResultLink.setText("");
        } catch (java.io.UnsupportedEncodingException e) {
            Toast.makeText(this, "Erro de decodificação.", Toast.LENGTH_SHORT).show();
            tvResultLink.setText("");
        }
    }

    private void shareResult() {
        String resultText = tvResultLink.getText().toString();
        if (resultText.isEmpty()) {
            Toast.makeText(this, "Nenhum resultado para compartilhar.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, resultText);
        startActivity(Intent.createChooser(shareIntent, "Compartilhar Link Via"));
    }

    private void copyResultToClipboard() {
        String resultText = tvResultLink.getText().toString();
        if (resultText.isEmpty()) {
            Toast.makeText(this, "Nenhum resultado para copiar.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Link", resultText);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Link copiado para a área de transferência!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Não foi possível acessar a área de transferência.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
