package models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Calendar;
import java.util.List;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String title;

    public String description;

    public long createdTime;
    public long dueTime;

    public boolean isDone;
    public boolean isNotificationOn;

    public String category;

    public List<String> attachments;

    public Task(@NonNull String title, String description, long createdTime, long dueTime,
                boolean isDone, boolean isNotificationOn, String category, List<String> attachments) {
        this.title = title;
        this.description = description;
        this.createdTime = createdTime;
        this.dueTime = dueTime;
        this.isDone = isDone;
        this.isNotificationOn = isNotificationOn;
        this.category = category;
        this.attachments = attachments;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean b) {
        isDone = b;
    }

    public int getId() {
        return id;
    }

    public long getExecutionTime() {
        return dueTime;
    }
}
