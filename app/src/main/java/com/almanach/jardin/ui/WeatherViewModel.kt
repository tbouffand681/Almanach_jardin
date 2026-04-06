package com.almanach.jardin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.almanach.jardin.data.CitySuggestion
import com.almanach.jardin.data.WeatherResult
import com.almanach.jardin.data.WeatherService
import kotlinx.coroutines.launch

sealed class WeatherState {
    object Idle    : WeatherState()
    object Loading : WeatherState()
    data class Success(val data: WeatherResult) : WeatherState()
    data class Error(val message: String)       : WeatherState()
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableLiveData<WeatherState>(WeatherState.Idle)
    val state: LiveData<WeatherState> = _state

    fun fetchWeather(city: String) {
        if (city.isBlank()) return
        _state.value = WeatherState.Loading
        viewModelScope.launch {
            WeatherService.fetchByCity(city).fold(
                onSuccess = { _state.value = WeatherState.Success(it) },
                onFailure = { _state.value = WeatherState.Error(
                    it.message ?: "Erreur réseau. Vérifiez votre connexion."
                )}
            )
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double, cityLabel: String) {
        _state.value = WeatherState.Loading
        viewModelScope.launch {
            runCatching {
                com.almanach.jardin.data.WeatherService.fetchByCoords(lat, lon, cityLabel.split(",").first().trim())
            }.fold(
                onSuccess = { _state.value = WeatherState.Success(it) },
                onFailure = { _state.value = WeatherState.Error(it.message ?: "Erreur réseau.") }
            )
        }
    }

    fun resetToIdle() {
        _state.value = WeatherState.Idle
    }

    /** Retourne une liste de suggestions pour l'autocomplétion — appelé depuis le Fragment */
    suspend fun fetchSuggestions(query: String): List<CitySuggestion> =
        WeatherService.fetchSuggestions(query).getOrElse { emptyList() }
}
