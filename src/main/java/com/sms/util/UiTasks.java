package com.sms.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/** Runs disk/database-heavy UI operations away from the JavaFX application thread. */
public final class UiTasks {
    private UiTasks() { }

    public static <T> void run(String name, Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> {
            AppLogger.error(UiTasks.class, name + " failed", task.getException());
            onFailure.accept(task.getException());
        });
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
