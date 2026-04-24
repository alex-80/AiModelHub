package com.ai_model_hub.sdk

/** Service binding state. */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val service: com.ai_model_hub.service.IAiModelHubService) :
        ConnectionState()

    data class Error(val message: String) : ConnectionState()
}
