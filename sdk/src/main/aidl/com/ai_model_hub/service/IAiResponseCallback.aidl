package com.ai_model_hub.service;

interface IAiResponseCallback {
    void onToken(String token);
    void onComplete(String fullText);
    void onError(String errorMessage);
}
