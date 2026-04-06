package com.almanach.jardin.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.almanach.jardin.data.WeatherResult
import com.almanach.jardin.databinding.FragmentWeatherBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private val vm: WeatherViewModel by viewModels()

    // Job de debounce pour l'autocomplétion (évite un appel réseau à chaque frappe)
    private var autocompleteJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearch()
        setupAutocomplete()

        // Bouton "Changer de ville" → retour à l'état Idle
        binding.btnRefresh.setOnClickListener {
            vm.resetToIdle()
        }

        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WeatherState.Idle    -> showIdle()
                is WeatherState.Loading -> showLoading()
                is WeatherState.Success -> showWeather(state.data)
                is WeatherState.Error   -> showError(state.message)
            }
        }
    }

    // ─── Recherche ────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener { doSearch() }
        binding.etCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
    }

    private fun doSearch() {
        val city = binding.etCity.text?.toString()?.trim() ?: ""
        if (city.isEmpty()) { binding.tilCity.error = "Entrez une ville"; return }
        binding.tilCity.error = null
        hideKeyboard()
        vm.fetchWeather(city)
    }

    // ─── Autocomplétion ───────────────────────────────────────────────────────

    private fun setupAutocomplete() {
        binding.etCity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length < 3) return   // Attendre 3 caractères minimum

                // Debounce 400 ms pour ne pas spammer Nominatim
                autocompleteJob?.cancel()
                autocompleteJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(400)
                    val suggestions = vm.fetchSuggestions(query)
                    if (_binding != null && suggestions.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            suggestions.map { it.displayLabel }
                        )
                        // Cast explicite requis par le compilateur Kotlin
                        val autoComplete = binding.etCity as android.widget.AutoCompleteTextView
                        autoComplete.setAdapter(adapter)
                        autoComplete.showDropDown()

                        // Quand l'utilisateur sélectionne une suggestion → météo par coords
                        autoComplete.setOnItemClickListener { _: android.widget.AdapterView<*>, _: android.view.View, position: Int, _: Long ->
                            val chosen = suggestions[position]
                            autoComplete.setText(chosen.displayLabel)
                            hideKeyboard()
                            vm.fetchWeatherByCoords(chosen.lat, chosen.lon, chosen.displayLabel)
                        }
                    }
                }
            }
        })
    }

    // ─── États visuels ────────────────────────────────────────────────────────

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.cardWeather.visibility = View.GONE
        binding.cardEt0.visibility     = View.GONE
        binding.cardAdvice.visibility  = View.GONE
        binding.btnRefresh.visibility  = View.GONE
        binding.tilCity.visibility     = View.VISIBLE
        binding.btnSearch.visibility   = View.VISIBLE
        binding.etCity.setText("")
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardWeather.visibility = View.GONE
        binding.cardEt0.visibility     = View.GONE
        binding.cardAdvice.visibility  = View.GONE
        binding.btnRefresh.visibility  = View.GONE
    }

    private fun showWeather(w: WeatherResult) {
        binding.progressBar.visibility = View.GONE
        binding.cardWeather.visibility = View.VISIBLE
        binding.cardEt0.visibility     = View.VISIBLE
        binding.cardAdvice.visibility  = View.VISIBLE
        binding.btnRefresh.visibility  = View.VISIBLE
        binding.tilCity.visibility     = View.GONE
        binding.btnSearch.visibility   = View.GONE

        binding.tvCity.text         = "📍 ${w.cityName}"
        binding.tvIcon.text         = w.weatherEmoji
        binding.tvTemp.text         = "${w.temperature.toInt()}°C"
        binding.tvDescription.text  = w.weatherDescription
        binding.tvFeelsLike.text    = "Ressenti ${w.feelsLike.toInt()}°C"
        binding.tvMinMax.text       = "↓${w.tempMin.toInt()}° ↑${w.tempMax.toInt()}°"
        binding.tvHumidity.text     = "💧 ${w.humidity}%"
        binding.tvWind.text         = "💨 ${w.windSpeed.toInt()} km/h"
        binding.tvPrecip.text       = "🌧 ${f(w.precipitation)} mm"

        binding.tvEt0Today.text     = "${f(w.et0Today)} L/m²"
        binding.tvEt0Cumul2.text    = "${f(w.et0Cumul2)} L/m²"
        binding.tvEt0Cumul5.text    = "${f(w.et0Cumul5)} L/m²"

        binding.tvSowingAdvice.text     = w.sowingAdvice()
        binding.tvIrrigationAdvice.text = w.irrigationAdvice()
    }

    private fun showError(msg: String) {
        binding.progressBar.visibility = View.GONE
        binding.tilCity.visibility     = View.VISIBLE
        binding.btnSearch.visibility   = View.VISIBLE
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(binding.etCity.windowToken, 0)
    }

    private fun f(v: Double) = "%.1f".format(v)

    override fun onDestroyView() {
        autocompleteJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
