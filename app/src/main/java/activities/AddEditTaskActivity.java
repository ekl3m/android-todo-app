package activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todolist.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import models.Task;
import notifications.NotificationUtils;
import utilities.AttachmentManager;
import utilities.TaskDatabase;

public class AddEditTaskActivity extends AppCompatActivity {

    private EditText editTitle, editDescription;
    private SwitchCompat switchDone, switchNotification;
    private String selectedCategory = "Dom"; // domyślna
    private View categoryDom, categoryPraca, categorySzkola, categoryInne;
    private Button btnPickDate;
    private ImageView iconSave;
    private int editingTaskId = -1;
    private LinearLayout attachmentsContainer;
    private List<Uri> attachmentsUriList = new ArrayList<>();
    private List<String> attachmentsStoredPaths = new ArrayList<>();
    private long dueTimeInMillis = 0;
    private long createdTime = 0;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<String[]> pickFilesLauncher;

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        switchDone = findViewById(R.id.switchDone);
        switchNotification = findViewById(R.id.switchNotification);
        btnPickDate = findViewById(R.id.btnPickDate);
        iconSave = findViewById(R.id.iconSave);
        categoryDom = findViewById(R.id.categoryHome);
        categoryPraca = findViewById(R.id.categoryWork);
        categorySzkola = findViewById(R.id.categorySchool);
        categoryInne = findViewById(R.id.categoryOther);
        attachmentsContainer = findViewById(R.id.attachmentsContainer);

        btnPickDate.setText("Wybierz termin");

        resetCategoryBorders();
        categoryDom.setBackground(getDrawable(R.drawable.gold_outline_rounded));
        selectedCategory = "Dom";

        btnPickDate.setOnClickListener(v -> openDateTimePicker());
        iconSave.setOnClickListener(v -> saveTask());

        View.OnClickListener categoryClickListener = v -> {
            resetCategoryBorders();
            v.setBackgroundResource(R.drawable.gold_outline_rounded);

            int id = v.getId();
            if (id == R.id.categoryHome) selectedCategory = "Dom";
            else if (id == R.id.categoryWork) selectedCategory = "Praca";
            else if (id == R.id.categorySchool) selectedCategory = "Szkoła";
            else if (id == R.id.categoryOther) selectedCategory = "Inne";
        };

        categoryDom.setOnClickListener(categoryClickListener);
        categoryPraca.setOnClickListener(categoryClickListener);
        categorySzkola.setOnClickListener(categoryClickListener);
        categoryInne.setOnClickListener(categoryClickListener);

        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(),
                uris -> {
                    if (uris != null) {
                        attachmentsUriList.addAll(uris);
                        refreshAttachmentsUI();
                    }
                });

        pickFilesLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null) {
                        attachmentsUriList.addAll(uris);
                        refreshAttachmentsUI();
                    }
                });

        findViewById(R.id.btnAddAttachments).setOnClickListener(v -> {
            String[] mimeTypes = {"image/*", "application/pdf", "text/plain", "application/zip", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
            pickFilesLauncher.launch(mimeTypes);
        });

        editingTaskId = getIntent().getIntExtra("edit_task_id", -1);
        Log.d("EditTask", "Editing ID: " + editingTaskId);
        if (editingTaskId != -1) {
            TaskDatabase.getInstance(this).taskDao().getTaskById(editingTaskId)
                    .observe(this, task -> {
                        if (task != null) {
                            findViewById(android.R.id.content).post(() -> fillFields(task));
                        }
                    });
        }

        View rootView = findViewById(R.id.rootViewEditForm);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + 20, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    private void openDateTimePicker() {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                calendar.set(year, month, dayOfMonth, hourOfDay, minute);
                dueTimeInMillis = calendar.getTimeInMillis();

                Locale locale = new Locale("pl", "PL");
                SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, 'godzina' HH:mm", locale);
                btnPickDate.setText(sdf.format(calendar.getTime()));
                findViewById(R.id.iconEditDate).setVisibility(View.VISIBLE);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveTask() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Należy wpisać tytuł zadania", Toast.LENGTH_SHORT).show();
            return;
        }
        String description = editDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Należy wpisać opis zadania", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dueTimeInMillis == 0) {
            Toast.makeText(this, "Należy wybrać termin wykonania", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dueTimeInMillis < System.currentTimeMillis() && switchNotification.isChecked()) {
            Toast.makeText(this, "Wskazano termin wykonania zadania\nw przeszłości - odznacz powiadomienia", Toast.LENGTH_SHORT).show();
            return;
        }

        long creationTimeToUse = (editingTaskId == -1) ? System.currentTimeMillis() : createdTime;

        Task task = new Task(
                title,
                description,
                creationTimeToUse,
                dueTimeInMillis,
                switchDone.isChecked(),
                switchNotification.isChecked(),
                selectedCategory,
                new ArrayList<>()
        );

        if (editingTaskId != -1) task.id = editingTaskId;

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int minutesBefore = prefs.getInt("notification_lead_time", 15);

        new Thread(() -> {
            TaskDatabase db = TaskDatabase.getInstance(getApplicationContext());

            if (editingTaskId == -1) {
                long newId = db.taskDao().insert(task);
                task.id = (int) newId;
            } else {
                task.id = editingTaskId;
                db.taskDao().update(task);
            }

            task.attachments = AttachmentManager.copyAttachmentsForTask(getApplicationContext(), attachmentsUriList, task.id);
            db.taskDao().update(task);

            if (task.isNotificationOn) {
                NotificationUtils.scheduleNotification(getApplicationContext(), task, minutesBefore);
            }

            runOnUiThread(this::finish);
        }).start();
    }

    private void fillFields(Task task) {
        editTitle.setText(task.title);
        editDescription.setText(task.description);
        switchDone.setChecked(task.isDone);
        switchNotification.setChecked(task.isNotificationOn);
        dueTimeInMillis = task.dueTime;
        createdTime = task.createdTime;

        Locale locale = new Locale("pl", "PL");
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, 'godzina' HH:mm", locale);
        btnPickDate.setText(sdf.format(new Date(dueTimeInMillis)));
        findViewById(R.id.iconEditDate).setVisibility(View.VISIBLE);

        switch (task.category) {
            case "Dom": categoryDom.performClick(); break;
            case "Praca": categoryPraca.performClick(); break;
            case "Szkoła": categorySzkola.performClick(); break;
            case "Inne": categoryInne.performClick(); break;
        }

        attachmentsUriList.clear();
        attachmentsStoredPaths.clear();
        attachmentsStoredPaths.addAll(task.attachments);
        refreshAttachmentsUI();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void resetCategoryBorders() {
        categoryDom.setBackground(getDrawable(R.drawable.gray_outline_rounded));
        categoryPraca.setBackground(getDrawable(R.drawable.gray_outline_rounded));
        categorySzkola.setBackground(getDrawable(R.drawable.gray_outline_rounded));
        categoryInne.setBackground(getDrawable(R.drawable.gray_outline_rounded));
    }

    private void refreshAttachmentsUI() {
        attachmentsContainer.removeAllViews();
        List<String> pathsToShow = !attachmentsStoredPaths.isEmpty() ? attachmentsStoredPaths : uriListToPaths(attachmentsUriList);

        for (int i = 0; i < pathsToShow.size(); i++) {
            String path = pathsToShow.get(i);
            File file = new File(path);
            String name = file.getName();
            View attachmentView = getLayoutInflater().inflate(R.layout.item_attachment, attachmentsContainer, false);

            TextView fileName = attachmentView.findViewById(R.id.fileName);
            ImageView deleteBtn = attachmentView.findViewById(R.id.deleteAttachment);
            ImageView icon = attachmentView.findViewById(R.id.icon);

            fileName.setText(name);

            if (name.matches(".*\\.(jpg|jpeg|png|bmp|webp|svg)")) {
                icon.setImageResource(R.drawable.ic_photo);
            } else {
                icon.setImageResource(R.drawable.ic_file);
            }

            int index = i;
            deleteBtn.setOnClickListener(v -> {
                if (!attachmentsStoredPaths.isEmpty()) {
                    attachmentsStoredPaths.remove(index);
                } else {
                    attachmentsUriList.remove(index);
                }
                refreshAttachmentsUI();
            });

            attachmentsContainer.addView(attachmentView);
        }
    }

    private List<String> uriListToPaths(List<Uri> uris) {
        List<String> paths = new ArrayList<>();
        for (Uri uri : uris) {
            paths.add(uri.toString());
        }
        return paths;
    }
}