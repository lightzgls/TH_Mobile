package com.example.studentmanager.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.studentmanager.utils.DbExecutor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ReminderWorker extends Worker {
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Cung hang doi voi login/logout va thao tac DB cua cac thanh vien.
        FutureTask<Void> task = new FutureTask<>(() -> {
            if (!isStopped()) NotificationHelper.deliverReminder(getApplicationContext(), getInputData());
            return null;
        });
        DbExecutor.execute(task);
        try {
            task.get();
            return Result.success();
        } catch (InterruptedException error) {
            task.cancel(false);
            Thread.currentThread().interrupt();
            return Result.failure();
        } catch (ExecutionException error) {
            return getRunAttemptCount() < 2 ? Result.retry() : Result.failure();
        }
    }
}
