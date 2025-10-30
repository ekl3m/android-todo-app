package notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.Date;

import models.Task;

public class NotificationUtils {
    public static void scheduleNotification(Context context, Task task, int minutesBefore) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("NotifDebug", "Exact alarm not allowed.");
                return;
            }
        }

        PendingIntent pendingIntent = getPendingIntent(context, task);

        long triggerTime = task.getExecutionTime() - (long) minutesBefore * 60 * 1000;

        Log.d("NotifDebug", "TriggerTime: " + triggerTime + ", CurrentTime: " + System.currentTimeMillis());

        if (triggerTime > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d("NotifDebug", "Scheduling task ID: " + task.getId()
                + ", title: " + task.getTitle()
                + ", trigger: " + new Date(triggerTime));
    }

    public static void cancelNotification(Context context, Task task) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = getPendingIntent(context, task);
        alarmManager.cancel(pendingIntent);
    }

    private static PendingIntent getPendingIntent(Context context, Task task) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());

        return PendingIntent.getBroadcast(
                context,
                task.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}