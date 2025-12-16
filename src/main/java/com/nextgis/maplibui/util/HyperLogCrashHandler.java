package com.nextgis.maplibui.util;

import com.hypertrack.hyperlog.HyperLog;

import java.io.PrintWriter;
import java.io.StringWriter;

public class HyperLogCrashHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler defaultHandler;

    public HyperLogCrashHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

        // Логируем падение
        HyperLog.e("CRASH",
                "Uncaught exception in thread: " + fullStackTrace(throwable),
                throwable);


        // Передаём управление стандартному хендлеру
        // (иначе система не покажет crash dialog)
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            // fallback
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }


    private static String fullStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}

