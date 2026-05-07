package com.ai_model_hub.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_model_hub.data.AppRepository
import com.ai_model_hub.sdk.BackendPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {

    val backendPreference: StateFlow<BackendPreference> = appRepository.backendPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackendPreference.CPU)

    val enableSpeculativeDecoding: StateFlow<Boolean> = appRepository.speculativeDecoding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setBackendPreference(pref: BackendPreference) {
        viewModelScope.launch { appRepository.setBackendPreference(pref) }
    }

    fun setEnableSpeculativeDecoding(enabled: Boolean) {
        viewModelScope.launch { appRepository.setSpeculativeDecoding(enabled) }
    }
}
