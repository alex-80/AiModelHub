package com.ai_model_hub.service;

interface ICreateSessionCallback {
    void onSuccess(String sessionId);
    void onError(String errorMessage);
}
