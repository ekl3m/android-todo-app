package interfaces;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import models.Task;

@Dao
public interface TaskDao {
    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY dueTime ASC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    LiveData<Task> getTaskById(int taskId);

    @Query("SELECT * FROM tasks")
    List<Task> getAllOnce();
}
