package com.example.studentmanager.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** TODO An: cap nhat nhac khi doi gio hoac mui gio. */
public class TimeChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            NotificationHelper.refresh(context);
        }
    }
}
