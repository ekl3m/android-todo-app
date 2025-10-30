package adapters;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.text.HtmlCompat;

import com.example.todolist.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import models.Task;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks = new ArrayList<>();
    private final OnTaskClickListener onTaskClick;
    private final OnEditClickListener onEditClick;
    private final OnDeleteClickListener onDeleteClick;
    private final OnFinishClickListener onFinishClick;

    public Task getTaskAt(int position) {
        return tasks.get(position);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnEditClickListener {
        void onEdit(Task task);
    }

    public interface OnDeleteClickListener {
        void onDelete(Task task);
    }

    public interface OnFinishClickListener {
        void onFinish(Task task);
    }

    public TaskAdapter(OnTaskClickListener onTaskClick, OnEditClickListener onEditClick, OnDeleteClickListener onDeleteClick, OnFinishClickListener onFinishClick) {
        this.onTaskClick = onTaskClick;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
        this.onFinishClick = onFinishClick;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        Task current = tasks.get(position);
        holder.bind(current);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView dueTime;
        private final TextView description;
        private final ImageView attachmentIcon;
        private final View detailLayout;
        private final Button editBtn;
        private final Button deleteBtn;
        private final Button finishBtn;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.taskTitle);
            dueTime = itemView.findViewById(R.id.taskDueTime);
            description = itemView.findViewById(R.id.taskDescription);
            attachmentIcon = itemView.findViewById(R.id.attachmentIcon);
            detailLayout = itemView.findViewById(R.id.detailsLayout);
            editBtn = itemView.findViewById(R.id.btnEdit);
            deleteBtn = itemView.findViewById(R.id.btnDelete);
            finishBtn = itemView.findViewById(R.id.btnFinish);
        }

        public void bind(Task task) {
            title.setText(task.title);

            Locale locale = new Locale("pl", "PL");
            Date date = new Date(task.dueTime);
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, 'godzina' HH:mm", locale);
            dueTime.setText(HtmlCompat.fromHtml("<b>Termin:</b> " + sdf.format(date), HtmlCompat.FROM_HTML_MODE_LEGACY));

            long timeLeft = task.dueTime - System.currentTimeMillis();
            if (timeLeft < 6 * 60 * 60 * 1000) {
                if (task.isDone) {
                    dueTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.backgroundDark)); // Zrobione to ok
                } else {
                    dueTime.setTextColor(Color.RED); // Mniej niż 6h
                }
            } else {
                dueTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.backgroundDark)); // Jest czas
            }

            description.setText(HtmlCompat.fromHtml("<b>Opis:</b> " + task.description + "<br>", HtmlCompat.FROM_HTML_MODE_LEGACY));
            TextView createdView = itemView.findViewById(R.id.detailCreated);
            TextView categoryView = itemView.findViewById(R.id.detailCategory);
            TextView statusView = itemView.findViewById(R.id.detailStatus);
            TextView notificationView = itemView.findViewById(R.id.detailNotification);
            TextView attachmentsView = itemView.findViewById(R.id.detailAttachments);

            createdView.setText(HtmlCompat.fromHtml("<b>Dodano:</b> " + sdf.format(new Date(task.createdTime)), HtmlCompat.FROM_HTML_MODE_LEGACY));
            categoryView.setText(HtmlCompat.fromHtml("<b>Kategoria:</b> " + task.category, HtmlCompat.FROM_HTML_MODE_LEGACY));
            statusView.setText(HtmlCompat.fromHtml("<b>Status:</b> " + (task.isDone ? "Zakończone" : "Niezakończone"), HtmlCompat.FROM_HTML_MODE_LEGACY));
            notificationView.setText(HtmlCompat.fromHtml("<b>Powiadomienia:</b> " + (task.isNotificationOn ? "Włączone" : "Wyłączone"), HtmlCompat.FROM_HTML_MODE_LEGACY));

            LinearLayout container = itemView.findViewById(R.id.attachmentsContainer);
            container.removeAllViews();

            if (task.attachments != null && !task.attachments.isEmpty()) {
                container.removeAllViews();
                container.setVisibility(View.GONE);

                for (String uriString : task.attachments) {
                    Uri uri = Uri.parse(uriString);
                    View thumb = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.item_attachment_preview, container, false);

                    Log.d("TaskAdapter", "URI: " + uriString);
                    if (uri == null || uriString.isEmpty()) {
                        Log.e("TaskAdapter", "BŁĘDNY URI!");
                        continue;
                    }

                    TextView nameView = thumb.findViewById(R.id.previewName);
                    ImageView iconView = thumb.findViewById(R.id.previewIcon);

                    String name = getFileNameFromUri(uri);
                    nameView.setText(name != null ? name : "Załącznik");

                    if (name != null && name.matches(".*\\.(jpg|jpeg|png|bmp|webp|svg)")) {
                        iconView.setImageResource(R.drawable.ic_photo);
                    } else {
                        iconView.setImageResource(R.drawable.ic_file);
                    }

                    thumb.setOnClickListener(v -> {
                        try {
                            Intent openIntent = new Intent(Intent.ACTION_VIEW);
                            openIntent.setDataAndType(uri, itemView.getContext().getContentResolver().getType(uri));
                            openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            itemView.getContext().startActivity(openIntent);
                        } catch (Exception e) {
                            Toast.makeText(itemView.getContext(), "Nie można otworzyć pliku", Toast.LENGTH_SHORT).show();
                        }
                    });

                    container.addView(thumb);
                }
                if (container.getChildCount() > 0) {
                    container.setVisibility(View.VISIBLE);
                } else {
                    container.setVisibility(View.GONE);
                }

                container.requestLayout();
                container.invalidate();
                itemView.requestLayout();
            } else {
                container.setVisibility(View.GONE);
            }

            attachmentsView.setVisibility(task.attachments != null && !task.attachments.isEmpty() ? View.VISIBLE : View.GONE);
            attachmentIcon.setVisibility(task.attachments != null && !task.attachments.isEmpty() ? View.VISIBLE : View.GONE);
            detailLayout.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> {
                if (detailLayout.getVisibility() == View.VISIBLE) {
                    detailLayout.setVisibility(View.GONE);
                } else {
                    detailLayout.setVisibility(View.VISIBLE);
                    detailLayout.requestLayout();
                    detailLayout.invalidate();
                }
            });

            if (task.isDone) {
                finishBtn.setText("Wznów");
            } else {
                finishBtn.setText("Zakończ");
            }

            editBtn.setOnClickListener(v -> onEditClick.onEdit(task));

            deleteBtn.setOnClickListener(v -> {
                SpannableString title = new SpannableString("Usuń zadanie");
                title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), R.color.accentGold)), 0, title.length(), 0);

                SpannableString message = new SpannableString("Czy na pewno chcesz usunąć to zadanie?");
                message.setSpan(new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), R.color.textPrimary)), 0, message.length(), 0);

                AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Usuń", (d, which) -> onDeleteClick.onDelete(task))
                        .setNegativeButton("Anuluj", null)
                        .show();

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accentGold));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textPrimary));
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(itemView.getContext(), R.color.cardBackground)));
            });

            finishBtn.setOnClickListener(v -> onFinishClick.onFinish(task));
        }

        private String getFileNameFromUri(Uri uri) {
            Cursor cursor = itemView.getContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    String name = cursor.getString(nameIndex);
                    cursor.close();
                    return name;
                }
                cursor.close();
            }
            return null;
        }
    }
}