package utilities;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Task;

public class AttachmentManager {

    public static String saveAttachmentToInternalStorage(Context context, Uri uri, int taskId) {
        String originalName = getDisplayNameFromUri(context, uri);
        String sanitized = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = taskId + "_" + System.currentTimeMillis() + "_" + sanitized;

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File file = new File(context.getFilesDir(), fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e("AttachmentManager", "Błąd przy zapisie załącznika", e);
            return null;
        }
    }

    public static void deleteUnusedAttachments(Context context, Task deletedTask, List<Task> allTasks) {
        Set<String> usedPaths = new HashSet<>();
        for (Task task : allTasks) {
            if (task.id == deletedTask.id) continue;
            usedPaths.addAll(task.attachments);
        }

        for (String path : deletedTask.attachments) {
            if (!usedPaths.contains(path)) {
                File file = new File(path);
                if (file.exists()) {
                    boolean deleted = file.delete();
                    Log.d("AttachmentManager", "Usuwanie załącznika: " + path + ", sukces: " + deleted);
                }
            }
        }
    }

    public static List<String> copyAttachmentsForTask(Context context, List<Uri> uris, int taskId) {
        List<String> storedPaths = new ArrayList<>();
        for (Uri uri : uris) {
            String stored = saveAttachmentToInternalStorage(context, uri, taskId);
            if (stored != null) storedPaths.add(stored);
        }
        return storedPaths;
    }

    public static Map<String, Integer> countAttachmentUsage(List<models.Task> allTasks) {
        Map<String, Integer> usageMap = new HashMap<>();
        for (models.Task task : allTasks) {
            for (String path : task.attachments) {
                usageMap.put(path, usageMap.getOrDefault(path, 0) + 1);
            }
        }
        return usageMap;
    }

    public static File getInternalStorageFile(Context context, String fileName) {
        return new File(context.getFilesDir(), fileName);
    }

    public static boolean fileExists(Context context, String path) {
        File file = new File(path);
        return file.exists();
    }

    public static String getDisplayNameFromUri(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "zalacznik"; // fallback
    }
}