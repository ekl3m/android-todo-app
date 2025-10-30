package com.example.todolist;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import activities.AddEditTaskActivity;
import activities.SettingsActivity;
import adapters.TaskAdapter;
import models.Task;
import notifications.NotificationUtils;
import utilities.TaskDatabase;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TaskAdapter adapter;

    // Filtrowanie
    private List<Task> allTasks = new ArrayList<>();
    private String currentQuery = "";
    private String selectedCategory = "Wszystko";
    private String selectedStatus = "Wszystko";

    // Przyciski
    private List<AppCompatButton> filterButtons = new ArrayList<>();
    private AppCompatButton lastSelectedButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new TaskAdapter(
                task -> {
                    // Rozwijanie szczegółów obsługiwane wewnątrz adaptera
                },
                task -> {
                    Intent intent = new Intent(this, AddEditTaskActivity.class);
                    intent.putExtra("edit_task_id", task.id);
                    startActivity(intent);
                },
                task -> {
                    new Thread(() -> {
                        if (task.isNotificationOn) {
                            NotificationUtils.cancelNotification(this, task);
                        }
                        TaskDatabase.getInstance(this).taskDao().delete(task);
                    }).start();
                },
                task -> {
                    task.setDone(!task.isDone());
                    new Thread(() -> {
                        TaskDatabase.getInstance(this).taskDao().update(task);
                    }).start();
                }
        );
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = adapter.getTaskAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    task.setDone(!task.isDone());
                    new Thread(() -> TaskDatabase.getInstance(MainActivity.this).taskDao().update(task)).start();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    Intent intent = new Intent(MainActivity.this, AddEditTaskActivity.class);
                    intent.putExtra("edit_task_id", task.id);
                    startActivity(intent);
                }

                adapter.notifyItemChanged(position); // przywraca widok po swipe
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                int iconSize = 48;
                int iconMargin = (itemView.getHeight() - iconSize) / 2;
                float cornerRadius = 24f;
                float backgroundInset = 24f; // Im większy, tym bardziej wchodzi pod kafelek

                Path path = new Path();
                float[] radii;
                RectF background;

                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    return;
                }
                Task task = adapter.getTaskAt(position);

                if (dX > 0) { // Swipe right → edycja
                    paint.setColor(ContextCompat.getColor(MainActivity.this, R.color.cardBackground));
                    background = new RectF(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + dX + backgroundInset, itemView.getBottom());

                    radii = new float[]{cornerRadius, cornerRadius, 0f, 0f, 0f, 0f, cornerRadius, cornerRadius};
                    path.addRoundRect(background, radii, Path.Direction.CW);
                    c.drawPath(path, paint);

                    Drawable icon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_pencil);
                    if (icon != null) {
                        int top = itemView.getTop() + iconMargin;
                        int left = itemView.getLeft() + iconMargin;
                        icon.setBounds(left, top, left + iconSize, top + iconSize);
                        icon.draw(c);
                    }

                } else if (dX < 0) { // Swipe left → wykonane
                    paint.setColor(ContextCompat.getColor(MainActivity.this, R.color.accentGold));
                    background = new RectF(itemView.getRight() + dX - backgroundInset, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());

                    radii = new float[]{0f, 0f, cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f};
                    path.addRoundRect(background, radii, Path.Direction.CW);
                    c.drawPath(path, paint);

                    int iconRes = task.isDone() ? R.drawable.ic_undo : R.drawable.ic_checkmark;
                    Drawable icon = ContextCompat.getDrawable(MainActivity.this, iconRes);
                    if (icon != null) {
                        int top = itemView.getTop() + iconMargin;
                        int right = itemView.getRight() - iconMargin;
                        icon.setBounds(right - iconSize, top, right, top + iconSize);
                        icon.setTint(ContextCompat.getColor(MainActivity.this, R.color.textSecondary));
                        icon.draw(c);
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "task_channel", "Przypomnienia o zadaniach", NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Powiadomienia przed wykonaniem zadania");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);

        // Zbierz przyciski
        filterButtons.add(findViewById(R.id.btnAll));
        filterButtons.add(findViewById(R.id.btnToDo));
        filterButtons.add(findViewById(R.id.btnDone));
        filterButtons.add(findViewById(R.id.btnHome));
        filterButtons.add(findViewById(R.id.btnWork));
        filterButtons.add(findViewById(R.id.btnSchool));
        filterButtons.add(findViewById(R.id.btnOther));

        for (AppCompatButton btn : filterButtons) {
            btn.setOnClickListener(v -> {
                handleFilterClick((AppCompatButton) v);
            });
        }

        // Zaznacz domyślnie pierwszy
        handleFilterClick(filterButtons.get(0));

        // Obserwuj dane z Rooma
        TaskDatabase db = TaskDatabase.getInstance(this);
        db.taskDao().getAllTasks().observe(this, tasks -> {
            allTasks.clear();
            allTasks.addAll(tasks);
            applyFilters();
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditTaskActivity.class);
            startActivity(intent);
        });

        View header = findViewById(R.id.headerLayout);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + 40, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ImageView searchIcon = findViewById(R.id.searchIcon);
        EditText searchEdit = findViewById(R.id.searchEditText);

        searchIcon.setOnClickListener(v -> {
            if (searchEdit.getVisibility() == View.GONE) {
                searchEdit.setVisibility(View.VISIBLE);
                searchEdit.requestFocus();
                searchIcon.setBackgroundResource(R.drawable.search_icon_bg);
            } else {
                searchEdit.setVisibility(View.GONE);
                searchIcon.setBackgroundResource(0); // Usuwa tło

                // Schowaj klawiaturę
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
                }
            }
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                applyFilters();
            }
        });

        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFilters(); // odśwież dane, bo mogły się zmienić ustawienia
    }

    private void applyFilters() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean hideCompleted = prefs.getBoolean("hide_completed", false);
        List<Task> filtered = new ArrayList<>();

        for (Task task : allTasks) {
            boolean matchesQuery = task.getTitle().toLowerCase().contains(currentQuery.toLowerCase()) ||
                    task.getDescription().toLowerCase().contains(currentQuery.toLowerCase());

            boolean matchesCategory = selectedCategory.equals("Wszystko") ||
                    task.getCategory().equalsIgnoreCase(selectedCategory);

            boolean matchesStatus = selectedStatus.equals("Wszystko") ||
                    (selectedStatus.equals("Wykonane") && task.isDone()) ||
                    (selectedStatus.equals("Do zrobienia") && !task.isDone());

            if (hideCompleted && task.isDone() && !selectedStatus.equals("Wykonane")) continue;

            if (matchesQuery && matchesCategory && matchesStatus) {
                filtered.add(task);
            }
        }

        adapter.setTasks(filtered);

        TextView emptyTextView = findViewById(R.id.emptyTextView);
        emptyTextView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void handleFilterClick(AppCompatButton clicked) {
        if (lastSelectedButton != null) {
            // Styl nieaktywny
            lastSelectedButton.setBackgroundResource(R.drawable.rounded_button_gold);
        }

        // Styl aktywny
        clicked.setBackgroundResource(R.drawable.rounded_button_selected);
        lastSelectedButton = clicked;

        // Przesuń do środka (jeśli nie ostatni)
        clicked.post(() -> centerButtonInScroll(clicked));

        String text = clicked.getText().toString();

        if (text.equals("Wszystko")) {
            selectedStatus = "Wszystko";
            selectedCategory = "Wszystko";
        } else if (text.equals("Do zrobienia") || text.equals("Wykonane")) {
            selectedStatus = text;
            selectedCategory = "Wszystko";
        } else {
            selectedCategory = text;
            selectedStatus = "Wszystko";
        }

        applyFilters();
    }

    private void centerButtonInScroll(View button) {
        HorizontalScrollView scrollView = findViewById(R.id.scrollViewFilters);

        // Jeśli to jeden z dwóch ostatnich przycisków
        if (button == filterButtons.get(filterButtons.size() - 1) ||
                button == filterButtons.get(filterButtons.size() - 2)) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_RIGHT));
        } else {
            int scrollX = button.getLeft() - (scrollView.getWidth() / 2) + (button.getWidth() / 2);
            scrollView.smoothScrollTo(scrollX, 0);
        }
    }
}