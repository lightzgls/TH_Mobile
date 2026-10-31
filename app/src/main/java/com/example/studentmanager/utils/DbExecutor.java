package com.example.studentmanager.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Moi thao tac ghi database cua app chay tuan tu tren executor nay. */
public final class DbExecutor {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private DbExecutor() {}
    public static void execute(Runnable task) { EXECUTOR.execute(task); }
}
