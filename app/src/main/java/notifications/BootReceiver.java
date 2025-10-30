package notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.List;

import models.Task;
import utilities.TaskDatabase;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            TaskDatabase db = TaskDatabase.getInstance(context);

            new Thread(() -> {
                List<Task> tasks = db.taskDao().getAllOnce();

                SharedPreferences prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
                int minutesBefore = prefs.getInt("notification_lead_time", 15);

                for (Task task : tasks) {
                    if (task.isNotificationOn && !task.isDone) {
                        NotificationUtils.scheduleNotification(context, task, minutesBefore);
                    }
                }
            }).start();
        }
    }
}