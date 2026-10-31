package com.example.studentmanager.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** TODO An: doc lai phien va lich truoc khi gui notification. */
public class ReminderWorker extends Worker {
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        // Khong len lich Worker nay cho den khi An trien khai xong.
        return Result.failure();
    }
}
