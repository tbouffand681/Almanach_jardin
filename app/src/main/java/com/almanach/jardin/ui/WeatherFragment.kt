package com.almanach.jardin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.almanach.jardin.data.AgroWarning
import com.almanach.jardin.data.DayWeather
import com.almanach.jardin.data.WeatherResult
import com.almanach.jardin.databinding.FragmentWeatherBinding
import com.google.android.material.snackbar.Snackbar

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private val vm: WeatherViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSearch.setOnClickListener { doSearch() }
        binding.etCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
        binding.btnRefresh.setOnClickListener { vm.resetToIdle() }
        binding.btnPrevDay.setOnClickListener { vm.prevDay() }
        binding.btnNextDay.setOnClickListener { vm.nextDay() }

        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WeatherState.Idle    -> showIdle()
                is WeatherState.Loading -> showLoading()
                is WeatherState.Success -> {
                    val idx = vm.dayIndex.value ?: 0
                    showWeather(state.data, idx)
                }
                is WeatherState.Error   -> showError(state.message)
            }
        }

        vm.dayIndex.observe(viewLifecycleOwner) { idx ->
            val state = vm.state.value
            if (state is WeatherState.Success) showWeather(state.data, idx)
        }
    }

    private fun doSearch() {
        val city = binding.etCity.text?.toString()?.trim() ?: ""
        if (city.isEmpty()) { binding.tilCity.error = "Entrez une ville"; return }
        binding.tilCity.error = null
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(binding.etCity.windowToken, 0)
        vm.fetchWeather(city)
    }

    private fun showIdle() {
        binding.progressBar.visibility  = View.GONE
        binding.cardWeather.visibility  = View.GONE
        binding.cardCumul.visibility    = View.GONE
        binding.cardWarnings.visibility = View.GONE
        binding.btnRefresh.visibility   = View.GONE
        binding.tilCity.visibility      = View.VISIBLE
        binding.btnSearch.visibility    = View.VISIBLE
        binding.etCity.setText("")
    }

    private fun showLoading() {
        binding.progressBar.visibility  = View.VISIBLE
        binding.cardWeather.visibility  = View.GONE
        binding.cardCumul.visibility    = View.GONE
        binding.cardWarnings.visibility = View.GONE
    }

    private fun showWeather(w: WeatherResult, idx: Int) {
        binding.progressBar.visibility  = View.GONE
        binding.cardWeather.visibility  = View.VISIBLE
        binding.cardCumul.visibility    = View.VISIBLE
        binding.cardWarnings.visibility = View.VISIBLE
        binding.btnRefresh.visibility   = View.VISIBLE
        binding.tilCity.visibility      = View.GONE
        binding.btnSearch.visibility    = View.GONE

        // Ville
        binding.tvCity.text = "📍 ${w.cityName}"

        // Navigation
        binding.btnPrevDay.isEnabled = idx > 0
        binding.btnNextDay.isEnabled = idx < w.days.size - 1

        // Jour affiché
        val day = w.days.getOrNull(idx) ?: return
        bindDay(day)

        // Cumulés ET₀ et précipitations (toujours J)
        binding.tvEt0Today.text    = f(w.et0Today)
        binding.tvEt048h.text      = f(w.et0Cumul48h)
        binding.tvEt05d.text       = f(w.et0Cumul5d)
        binding.tvPrecipToday.text = f(w.precipToday)
        binding.tvPrecip48h.text   = f(w.precipCumul48h)
        binding.tvPrecip5d.text    = f(w.precipCumul5d)

        // Avertissements
        showWarnings(w.warnings)
    }

    private fun bindDay(day: DayWeather) {
        binding.tvDayLabel.text    = day.label
        binding.tvIcon.text        = day.weatherEmoji
        binding.tvDescription.text = day.weatherDescription
        binding.tvMinMax.text      = "↓${day.tempMin.toInt()}°  ↑${day.tempMax.toInt()}°"
        binding.tvHumidity.text    = "💧 ${day.humidity}%"
        binding.tvWind.text        = "💨 ${day.windSpeed.toInt()} km/h"
        binding.tvPrecip.text      = "🌧 ${f(day.precipitation)} mm"
    }

    private fun showWarnings(warnings: List<AgroWarning>) {
        if (warnings.isEmpty()) {
            binding.tvNoWarnings.visibility   = View.VISIBLE
            binding.layoutWarnings.visibility = View.GONE
        } else {
            binding.tvNoWarnings.visibility   = View.GONE
            binding.layoutWarnings.visibility = View.VISIBLE
            binding.layoutWarnings.removeAllViews()
            warnings.forEach { w ->
                val tv = TextView(requireContext())
                tv.text = "${w.emoji}  ${w.message}"
                tv.textSize = 13f
                tv.setPadding(0, 0, 0, 20)
                tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                binding.layoutWarnings.addView(tv)
            }
        }
    }

    private fun showError(msg: String) {
        binding.progressBar.visibility = View.GONE
        binding.tilCity.visibility     = View.VISIBLE
        binding.btnSearch.visibility   = View.VISIBLE
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    private fun f(v: Double) = "%.1f".format(v)

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
