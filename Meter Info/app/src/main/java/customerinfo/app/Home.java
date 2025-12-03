package customerinfo.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;

public class Home extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        // Check Firebase initialization
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Toast.makeText(this, "Firebase init failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Continue anyway - app might work without Firebase
        }

        Button lookupBtn = findViewById(R.id.lookupBtn);
        Button applicationBtn = findViewById(R.id.applicationBtn);

        lookupBtn.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, LookupHtmlActivity.class));
        });

        applicationBtn.setOnClickListener(v -> {
            startActivity(new Intent(Home.this, HtmlActivity.class));
        });
    }
}
