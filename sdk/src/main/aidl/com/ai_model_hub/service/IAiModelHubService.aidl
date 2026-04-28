package com.ai_model_hub.service;

import com.ai_model_hub.service.IAiResponseCallback;
import com.ai_model_hub.service.ILoadModelCallback;

interface IAiModelHubService {
    List<String> getLoadedModels();
    void loadModel(String modelName, ILoadModelCallback callback);
    void unloadModel(String modelName);
    boolean isModelLoaded(String modelName);
    void sendMessage(String modelName, String message, IAiResponseCallback callback);
    void stopGeneration(String modelName);
    void resetSession(String modelName);
}
