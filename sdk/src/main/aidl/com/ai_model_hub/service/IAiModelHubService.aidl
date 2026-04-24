package com.ai_model_hub.service;

import com.ai_model_hub.service.IAiResponseCallback;

interface IAiModelHubService {
    List<String> getLoadedModels();
    void loadModel(String modelName);
    void unloadModel(String modelName);
    boolean isModelLoaded(String modelName);
    void sendMessage(String modelName, String message, IAiResponseCallback callback);
    void stopGeneration(String modelName);
    void resetSession(String modelName);
}
