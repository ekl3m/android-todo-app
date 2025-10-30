package notifications;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.todolist.R;

import activities.AddEditTaskActivity;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra("task_id", -1);
        String title = intent.getStringExtra("task_title");

        Log.d("NotifDebug", "Received alarm for taskId=" + taskId + ", title=" + title);

        Intent activityIntent = new Intent(context, AddEditTaskActivity.class);
        activityIntent.putExtra("edit_task_id", taskId);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, taskId, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "task_channel")
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("Zbliża się termin na wykonanie zadania:")
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManagerCompat.from(context).notify(taskId, builder.build());
    }
}
