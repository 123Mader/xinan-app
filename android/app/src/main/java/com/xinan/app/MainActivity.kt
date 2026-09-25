// ============================================================
// 「心安」Android 主界面 — 双模式入口 (聊天 + 视频陪伴)  · M3
// 位置: android/app/src/main/java/com/xinan/app/MainActivity.kt
// ============================================================
package com.xinan.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.content.Intent
import com.google.android.material.button.MaterialButtonToggleGroup
import com.xinan.app.chat.ChatFragment
import com.xinan.app.video.VideoFragment
import com.xinan.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentMode: String = "chat"
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

        // toolbar 菜单: 成长报告 / 模型管理 入口
        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_report -> { startActivity(Intent(this, com.xinan.app.report.GrowthReportActivity::class.java)); true }
                R.id.action_models -> { startActivity(Intent(this, com.xinan.app.llm.ModelManageActivity::class.java)); true }
                else -> false
            }
        }

        // M3 双模式切换: 单选高亮 + 模式路由
        binding.modeToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnChatMode -> switchMode("chat")
                    R.id.btnVideoMode -> switchMode("video")
                }
            }
        }

        // 默认进入聊天
        binding.modeToggleGroup.check(R.id.btnChatMode)
    }

    private fun switchMode(mode: String) {
        currentMode = mode
        val fragment: Fragment = when (mode) {
            "video" -> VideoFragment().also { videoFragment = it }
            else -> ChatFragment().also { videoFragment = null }
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    /** 供子 Fragment 拿到当前视频实例 (用于跨模块联动, 如聊天读取微表情心理预期) */
    fun currentVideoFragment(): VideoFragment? = videoFragment
}
