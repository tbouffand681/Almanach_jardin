package com.almanach.jardin.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.almanach.jardin.data.WeatherService
import kotlinx.coroutines.launch

sealed class WeatherState {
    object Idle    : WeatherState()
    object Loading : WeatherState()
    data class Success(val data: com.almanach.jardin.data.WeatherResult) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("almanach_prefs", Context.MODE_PRIVATE)

    private val _state = MutableLiveData<WeatherState>(WeatherState.Idle)
    val state: LiveData<WeatherState> = _state

    // Ville mémorisée (null si aucune)
    val savedCity: String? get() = prefs.getString("last_city", null)

    init {
        // Recharger automatiquement la dernière ville connue
        savedCity?.let { fetchWeather(it) }
    }

    fun fetchWeather(city: String) {
        if (city.isBlank()) return
        _state.value = WeatherState.Loading
        viewModelScope.launch {
            WeatherService.fetchByCity(city).fold(
                onSuccess = {
                    prefs.edit().putString("last_city", city.trim()).apply()
                    _state.postValue(WeatherState.Success(it))
                },
                onFailure = {
                    _state.postValue(WeatherState.Error(
                        it.message ?: "Erreur réseau. Vérifiez votre connexion."
                    ))
                }
            )
        }
    }

    fun resetToIdle() { _state.value = WeatherState.Idle }
}
