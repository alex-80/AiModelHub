package com.ai_model_hub.service;

interface ILoadModelCallback {
    void onSuccess();
    void onError(String errorMessage);
}
