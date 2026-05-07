package com.ai_model_hub.runtime

import com.ai_model_hub.sdk.Model
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import kotlinx.coroutines.sync.Mutex

/**
 * Wraps a single [Engine] and serializes access to its one-at-a-time [Conversation].
 *
 * LiteRT only supports one [Conversation] per [Engine] at a time. All sessions that
 * share the same model share this holder. The [mutex] ensures that only one coroutine
 * can own the active conversation at any moment — other callers block until the current
 * generation completes before the session can be switched.
 */
class EngineHolder(
    val engine: Engine,
    val model: Model,
) {
    val mutex = Mutex()

    /** ID of the [LlmSession] whose [Conversation] is currently open. */
    var activeSessionId: String? = null

    /** The currently open [Conversation], or null if no session has started yet. */
    var activeConversation: Conversation? = null
}
