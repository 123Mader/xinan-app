package com.xinan.app

/**
 * 心安 Application — 全局未捕获异常处理器
 * Java 崩溃时写日志到: ① app 私有目录(保底) ② /sdcard/Download/xinan_crash_*.log (MediaStore, 可被外部读取定位)
 * 注: native 信号(SIGSEGV)不走此 handler, 需避免触发(如不自动 init MediaPipe genai)
 */
class XinanApp : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            val text = "=== 心安崩溃日志 ===\n" +
                "time: ${java.util.Date()}\n" +
                "thread: ${t.name}\n" +
                "process: ${android.os.Process.myPid()}\n" +
                "exception: ${e.javaClass.name}: ${e.message}\n\n" +
                android.util.Log.getStackTraceString(e) + "\n"
            // 1. app 私有目录保底
            try { java.io.File(getExternalFilesDir(null), "xinan_crash.log").writeText(text) } catch (_: Throwable) {}
            // 2. 公共 Download (MediaStore, 外部可读 → 我能 cat 定位)
            try {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    val resolver = applicationContext.contentResolver
                    val v = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "xinan_crash_${System.currentTimeMillis()}.log")
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download")
                    }
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                    uri?.let { resolver.openOutputStream(it)?.use { os -> os.write(text.toByteArray()); os.flush() } }
                }
            } catch (_: Throwable) {}
            prev?.uncaughtException(t, e)
        }
    }
}
