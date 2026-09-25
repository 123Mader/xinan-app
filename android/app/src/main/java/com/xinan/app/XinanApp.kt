package com.xinan.app

/**
 * 心安 Application — 注册全局未捕获异常处理器
 * 任何 Activity/线程崩溃时把 Java 栈写入 app 私有目录 xinan_crash.log,
 * 便于无 logcat 权限时定位闪退原因。
 * (注: native 信号如 MediaPipe SIGSEGV 不走此 handler, 需避免触发其 native init)
 */
class XinanApp : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val dir = getExternalFilesDir(null)
                val log = java.io.File(dir, "xinan_crash.log")
                log.writeText(
                    "=== 心安崩溃日志 ===\n" +
                    "time: ${java.util.Date()}\n" +
                    "thread: ${t.name}\n" +
                    "process: ${android.os.Process.myPid()}\n" +
                    "exception: ${e.javaClass.name}: ${e.message}\n\n" +
                    android.util.Log.getStackTraceString(e) + "\n"
                )
            } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
    }
}
