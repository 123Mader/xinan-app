package com.xinan.app.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xinan.app.llm.LLMInference

/**
 * 聊天模式 — 文字对话疏导 (CBT)
 */
class ChatFragment : Fragment() {

    private lateinit var llm: LLMInference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    data class Message(val role: String, val content: String)  // role: user/ai

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // 简化 UI 骨架 (实际用 XML/Compose)
        val root = LinearLayoutCompat(requireContext())
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MessageAdapter(messages)
        }
        val inputRow = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        val input = EditText(requireContext()).apply { hint = "和我聊聊你的心情..." }
        val send = ImageButton(requireContext()).apply {
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    sendMessage(text)
                    input.text.clear()
                }
            }
        }
        inputRow.addView(input, android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        inputRow.addView(send)
        root.addView(recyclerView)
        root.addView(inputRow)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llm = LLMInference(requireContext())
        // ★ 不自动加载模型: MediaPipe genai native 在模型缺失时 init 可能 SIGSEGV(try/catch 抓不住 → 闪退)
        // 仅当模型文件存在才加载; 否则给引导, 不触发 native init。视频模式不受影响。
        val modelsDir = java.io.File(requireContext().getExternalFilesDir(null), "models")
        val modelFile = java.io.File(modelsDir, "qwen3-3b-q4.gguf")
        if (modelFile.exists()) {
            llm.loadModel(modelFile.absolutePath) { ok ->
                if (ok) {
                    messages.add(Message("ai", "你好呀，我是心安。今天感觉怎么样？"))
                } else {
                    messages.add(Message("ai", "模型加载失败, 请到菜单→🧠模型管理 重新下载"))
                }
                adapter.notifyDataSetChanged()
            }
        } else {
            messages.add(Message("ai", "你好, 我是心安 💙\n\n聊天对话需先装大模型: 右上角菜单 → 🧠模型管理 → 下载一个 3B 模型, 回到聊天即可对话。\n\n📹 视频陪伴模式的微表情/🧠心理预期分析可直接用, 不需要模型。"))
            adapter.notifyDataSetChanged()
        }
    }

    private fun sendMessage(text: String) {
        messages.add(Message("user", text))
        adapter.notifyDataSetChanged()
        // ★ 跨模式联动: 读取视频模式采集的最新微表情心理预期, 注入对话
        val visualContext = com.xinan.app.vision.EmotionStateHolder.formatVisualContext()
        val projection = com.xinan.app.vision.EmotionStateHolder.projection
        llm.chat(text, visualContext, projection) { reply ->
            messages.add(Message("ai", reply))
            activity?.runOnUiThread { adapter.notifyDataSetChanged() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        llm.close()
    }
}

/** 简化容器 (骨架用) */
class LinearLayoutCompat(context: android.content.Context) : android.widget.LinearLayout(context) {
    init {
        orientation = android.widget.LinearLayout.VERTICAL
    }
}