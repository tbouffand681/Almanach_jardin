package com.almanach.jardin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.almanach.jardin.data.CitySuggestion
import com.almanach.jardin.data.WeatherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class WeatherState {
    object Idle    : WeatherState()
    object Loading : WeatherState()
    data class Success(val data: com.almanach.jardin.data.WeatherResult) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableLiveData<WeatherState>(WeatherState.Idle)
    val state: LiveData<WeatherState> = _state

    fun fetchWeather(city: String) {
        if (city.isBlank()) return
        _state.value = WeatherState.Loading      // Main thread — OK
        viewModelScope.launch {
            // fetchByCity fait déjà withContext(IO) en interne
            WeatherService.fetchByCity(city).fold(
                onSuccess  = { _state.postValue(WeatherState.Success(it)) },
                onFailure  = { _state.postValue(WeatherState.Error(
                    it.message ?: "Erreur réseau. Vérifiez votre connexion."
                ))}
            )
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double, cityLabel: String) {
        _state.value = WeatherState.Loading      // Main thread — OK
        viewModelScope.launch {
            runCatching {
                // fetchByCoords est suspend + withContext(IO) → résultat sur IO
                WeatherService.fetchByCoords(
                    lat, lon,
                    cityLabel.split(",").first().trim()
                )
            }.fold(
                onSuccess  = { _state.postValue(WeatherState.Success(it)) },
                onFailure  = { _state.postValue(WeatherState.Error(
                    it.message ?: "Erreur réseau."
                ))}
            )
        }
    }

    fun resetToIdle() {
        _state.value = WeatherState.Idle
    }

    suspend fun fetchSuggestions(query: String): List<CitySuggestion> =
        WeatherService.fetchSuggestions(query).getOrElse { emptyList() }
}
