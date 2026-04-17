package com.almanach.jardin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.almanach.jardin.data.WeatherResult
import com.almanach.jardin.data.WeatherService
import kotlinx.coroutines.launch

sealed class WeatherState {
    object Idle    : WeatherState()
    object Loading : WeatherState()
    data class Success(val data: WeatherResult) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableLiveData<WeatherState>(WeatherState.Idle)
    val state: LiveData<WeatherState> = _state

    private val _dayIndex = MutableLiveData(0)   // 0 = aujourd'hui, 1..5 = J+1..J+5
    val dayIndex: LiveData<Int> = _dayIndex

    fun fetchWeather(city: String) {
        if (city.isBlank()) return
        _state.value = WeatherState.Loading
        _dayIndex.value = 0
        viewModelScope.launch {
            WeatherService.fetchByCity(city).fold(
                onSuccess = { _state.postValue(WeatherState.Success(it)) },
                onFailure = { _state.postValue(WeatherState.Error(
                    it.message ?: "Erreur réseau. Vérifiez votre connexion."
                ))}
            )
        }
    }

    fun prevDay() {
        val cur = _dayIndex.value ?: 0
        if (cur > 0) _dayIndex.value = cur - 1
    }

    fun nextDay() {
        val cur = _dayIndex.value ?: 0
        if (cur < 5) _dayIndex.value = cur + 1
    }

    fun resetToIdle() { _state.value = WeatherState.Idle }
}
