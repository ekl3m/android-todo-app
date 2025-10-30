package activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todolist.R;

public class SettingsActivity extends AppCompatActivity {

    private EditText editMinutes;
    private SwitchCompat switchHideCompleted;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Insety
        View rootView = findViewById(R.id.rootViewSettings);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + 20, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Przyciski
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        ImageView iconSave = findViewById(R.id.iconSave);
        editMinutes = findViewById(R.id.editMinutesTillNotify);
        switchHideCompleted = findViewById(R.id.switchHideCompleted);

        // Ładowanie zapisanych ustawień
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int savedMinutes = prefs.getInt("notification_lead_time", 15);
        boolean hideCompleted = prefs.getBoolean("hide_completed", false);

        editMinutes.setText(String.valueOf(savedMinutes));
        switchHideCompleted.setChecked(hideCompleted);

        // Obsługa przycisku zapisu
        iconSave.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        String text = editMinutes.getText().toString().trim();
        int minutes = 15; // domyślna

        try {
            minutes = Integer.parseInt(text);
        } catch (NumberFormatException ignored) {}

        boolean hideCompleted = switchHideCompleted.isChecked();

        prefs.edit()
                .putInt("notification_lead_time", minutes)
                .putBoolean("hide_completed", hideCompleted)
                .apply();

        Toast.makeText(this, "Ustawienia zapisane", Toast.LENGTH_SHORT).show();
        finish(); // zamyka activity po zapisie
    }
}