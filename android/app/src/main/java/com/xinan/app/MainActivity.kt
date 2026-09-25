// ============================================================
// 「心安」Android 主界面 — 双模式入口 (聊天 + 视频陪伴) · 最稳版
// 去掉 toolbar menu / toggle group, 用纯 Button click, 确保 inflate 不崩
// ============================================================
package com.xinan.app

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.xinan.app.chat.ChatFragment
import com.xinan.app.video.VideoFragment
import com.xinan.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var videoFragment: VideoFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 未登录跳转登录页
        if (!com.xinan.app.auth.LoginActivity.isLoggedIn(this)) {
            startActivity(Intent(this, com.xinan.app.auth.LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnChatMode.setOnClickListener { switchMode("chat") }
        binding.btnVideoMode.setOnClickListener { switchMode("video") }

        // 默认进入聊天
        switchMode("chat")
    }

    private fun switchMode(mode: String) {
        val fragment: Fragment = when (mode) {
            "video" -> VideoFragment().also { videoFragment = it }
            else -> ChatFragment().also { videoFragment = null }
        }
        try {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Fragment 切换失败: ${e.message}")
        }
        binding.btnChatMode.isSelected = mode == "chat"
        binding.btnVideoMode.isSelected = mode == "video"
    }

    /** 供子 Fragment 拿到当前视频实例 (跨模块联动) */
    fun currentVideoFragment(): VideoFragment? = videoFragment
}
