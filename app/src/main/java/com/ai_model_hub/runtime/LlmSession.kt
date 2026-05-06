package com.ai_model_hub.runtime

import com.ai_model_hub.sdk.Model
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.Role

/** A single turn in the conversation: the user prompt and the model's reply. */
data class HistoryEntry(val role: Role, val text: String)

/**
 * A virtual session backed by an [EngineHolder].
 *
 * Because LiteRT only supports one physical [com.google.ai.edge.litertlm.Conversation] per
 * [com.google.ai.edge.litertlm.Engine] at a time, the actual Conversation is owned by
 * [EngineHolder] and may be shared/switched between sessions. Each [LlmSession] keeps its
 * own [history] so the context can be restored via
 * [ConversationConfig.initialMessages] when the engine switches back to this session.
 */
data class LlmSession(
    internal val engineHolder: EngineHolder,
    val id: String,
    val model: Model,
    val conversationConfig: ConversationConfig,
    val history: MutableList<HistoryEntry> = mutableListOf(),
)

/** Reconstruct the full conversation history as [Message] objects for [ConversationConfig.initialMessages]. */
internal fun LlmSession.buildInitialMessages(): List<Message> =
    history.map { entry ->
        when (entry.role) {
            Role.USER -> Message.user(entry.text)
            Role.MODEL -> Message.model(entry.text)
            Role.SYSTEM -> Message.system(entry.text)
            Role.TOOL -> Message.tool(Contents.of(listOf(Content.Text(entry.text))))
        }
    }