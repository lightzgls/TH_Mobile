package com.example.studentmanager.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.studentmanager.MainActivity;
import com.example.studentmanager.R;
import com.example.studentmanager.dal.DBHelper;
import com.example.studentmanager.dal.DbContract;
import com.example.studentmanager.models.Session;
import com.example.studentmanager.utils.DateTimeHelper;
import com.example.studentmanager.utils.DbExecutor;
import com.example.studentmanager.utils.SessionManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/** Public API: co the goi tu UI hoac sau commit DB; moi viec chay tren DbExecutor. */
public final class NotificationHelper {
    private static final String CHANNEL = "study_reminders";
    private static final String TAG = "study_reminder";
    private NotificationHelper() {}

    public static void refresh(Context context) {
        refresh(context, () -> {});
    }

    /** Receiver dung callback de giu process song den khi cap nhat xong. */
    public static void refresh(Context context, Runnable onComplete) {
        Context app = context.getApplicationContext();
        DbExecutor.execute(() -> {
            try {
                WorkManager manager = WorkManager.getInstance(app);
                // Cho huy xong roi moi enqueue, tranh huy nham cac viec vua tao.
                manager.cancelAllWorkByTag(TAG).getResult().get(5, TimeUnit.SECONDS);
                SessionManager sessions = new SessionManager(app);
                Session session = sessions.getSession();
                if (session == null || !DbContract.STUDENT.equals(session.role)) {
                    notifications(app).cancelAll();
                    return;
                }
                String token = sessions.getSessionToken();
                clearStaleNotifications(app, token, session.studentId);
                String zone = ZoneId.systemDefault().getId();
                try (Cursor cursor = DBHelper.getInstance(app).getReadableDatabase().rawQuery(
                        "SELECT id,studyDate,startTime FROM StudySchedule WHERE studentId=? "
                                + "AND status IN ('PENDING','CONFIRMED')",
                        new String[]{String.valueOf(session.studentId)})) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        long start;
                        try {
                            start = startMillis(cursor.getString(1), cursor.getString(2));
                        } catch (RuntimeException invalidDate) {
                            continue; // Mot ban ghi hong khong duoc chan cac lich khac.
                        }
                        long now = System.currentTimeMillis();
                        if (start <= now) continue;
                        Data data = new Data.Builder().putLong("scheduleId", id)
                                .putLong("studentId", session.studentId).putLong("start", start)
                                .putString("token", token).putString("zone", zone).build();
                        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                                .setInputData(data).addTag(TAG)
                                .setInitialDelay(Math.max(0, start - now - TimeUnit.MINUTES.toMillis(15)),
                                        TimeUnit.MILLISECONDS).build();
                        manager.enqueueUniqueWork(TAG + "_" + id, ExistingWorkPolicy.REPLACE, request);
                    }
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } catch (Exception error) {
                // Loi thong bao khong duoc lam thao tac luu DB that bai.
                Log.e("StudyNotify", "Cannot refresh reminders", error);
            } finally {
                onComplete.run();
            }
        });
    }

    public static void showChange(Context context, long studentId, long scheduleId) {
        Context app = context.getApplicationContext();
        DbExecutor.execute(() -> {
            try {
                Session session = new SessionManager(app).getSession();
                if (session == null || !DbContract.STUDENT.equals(session.role)
                        || session.studentId != studentId || scheduleId <= 0) return;
                post(app, "change_" + scheduleId, app.getString(R.string.notify_change_title),
                        app.getString(R.string.notify_change_body));
            } catch (RuntimeException error) {
                Log.e("StudyNotify", "Cannot show schedule change", error);
            }
        });
    }

    /** Worker goi ham nay tren DbExecutor de khong gui xen giua logout va doi tai khoan. */
    static void deliverReminder(Context context, Data data) {
        SessionManager sessions = new SessionManager(context);
        Session session = sessions.getSession();
        if (session == null || !DbContract.STUDENT.equals(session.role)
                || session.studentId != data.getLong("studentId", -1)
                || !sessions.getSessionToken().equals(data.getString("token"))
                || !ZoneId.systemDefault().getId().equals(data.getString("zone"))) return;
        long id = data.getLong("scheduleId", -1);
        try (Cursor cursor = DBHelper.getInstance(context).getReadableDatabase().rawQuery(
                "SELECT s.studyDate,s.startTime,s.location,m.subjectName FROM StudySchedule s "
                        + "JOIN Subject m ON m.id=s.subjectId WHERE s.id=? AND s.studentId=? "
                        + "AND s.status IN ('PENDING','CONFIRMED')",
                new String[]{String.valueOf(id), String.valueOf(session.studentId)})) {
            if (!cursor.moveToFirst()) return;
            long start = startMillis(cursor.getString(0), cursor.getString(1));
            long now = System.currentTimeMillis();
            if (start != data.getLong("start", -1) || start <= now
                    || start - now > TimeUnit.MINUTES.toMillis(15)) return;
            // Khong nhac lap lai cung mot buoi moi lan mo app/refresh.
            String key = "sent_" + id;
            String signature = sessions.getSessionToken() + ":" + start;
            android.content.SharedPreferences prefs = context.getSharedPreferences("notification_state", Context.MODE_PRIVATE);
            if (signature.equals(prefs.getString(key, ""))) return;
            String text = context.getString(R.string.notify_reminder_body,
                    cursor.getString(3), DateTimeHelper.displayDate(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2));
            if (post(context, "reminder_" + id, context.getString(R.string.notify_reminder_title), text, start)) {
                prefs.edit().putString(key, signature).apply();
            }
        }
    }

    private static long startMillis(String date, String time) {
        return LocalDate.parse(date, DateTimeHelper.DB_DATE)
                .atTime(LocalTime.parse(time, DateTimeHelper.TIME))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static NotificationManager notifications(Context context) {
        return context.getSystemService(NotificationManager.class);
    }

    private static void clearStaleNotifications(Context context, String token, long studentId) {
        NotificationManager manager = notifications(context);
        for (android.service.notification.StatusBarNotification item : manager.getActiveNotifications()) {
            boolean keep = token.equals(item.getNotification().extras.getString("sessionToken"));
            String tag = item.getTag();
            if (keep && tag != null && tag.startsWith("reminder_")) {
                keep = false;
                try (Cursor cursor = DBHelper.getInstance(context).getReadableDatabase().rawQuery(
                        "SELECT studyDate,startTime FROM StudySchedule WHERE id=? AND studentId=? "
                                + "AND status IN ('PENDING','CONFIRMED')",
                        new String[]{tag.substring("reminder_".length()), String.valueOf(studentId)})) {
                    if (cursor.moveToFirst()) {
                        long start = startMillis(cursor.getString(0), cursor.getString(1));
                        keep = start == item.getNotification().extras.getLong("studyStart", -1)
                                && start > System.currentTimeMillis();
                    }
                } catch (RuntimeException invalidRow) {
                    keep = false;
                }
            }
            if (!keep) manager.cancel(tag, item.getId());
        }
    }

    private static boolean post(Context context, String tag, String title, String text) {
        return post(context, tag, title, text, -1);
    }

    private static boolean post(Context context, String tag, String title, String text, long start) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager manager = notifications(context);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                context.getString(R.string.notify_channel_name), NotificationManager.IMPORTANCE_DEFAULT));
        if (!manager.areNotificationsEnabled() || manager.getNotificationChannel(CHANNEL).getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.os.Bundle extras = new android.os.Bundle();
        extras.putString("sessionToken", new SessionManager(context).getSessionToken());
        extras.putLong("studyStart", start);
        try {
            manager.notify(tag, 1, new NotificationCompat.Builder(context, CHANNEL)
                    .addExtras(extras)
                    .setSmallIcon(R.drawable.ic_launcher).setContentTitle(title).setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(pending).setAutoCancel(true).build());
            return true;
        } catch (SecurityException denied) {
            return false;
        }
    }
}
