package com.ai_model_hub.service;

import com.ai_model_hub.service.IAiResponseCallback;
import com.ai_model_hub.service.ICreateSessionCallback;

interface IAiModelHubService {
    List<String> getAvailableModels();
    void createSession(String modelName, ICreateSessionCallback callback);
    void closeSession(String sessionId);
    boolean isSessionAlive(String sessionId);
    void sendMessage(String sessionId, String message, IAiResponseCallback callback);
    void stopGeneration(String sessionId);
    void resetSession(String sessionId);
}
