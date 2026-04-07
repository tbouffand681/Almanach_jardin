package com.almanach.jardin.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.almanach.jardin.R
import com.almanach.jardin.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    val viewModel: PlantViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.populateDefaultsIfNeeded()

        binding.tabs.addTab(binding.tabs.newTab().setText("🌱 Semis"))
        binding.tabs.addTab(binding.tabs.newTab().setText("📅 Du mois"))
        binding.tabs.addTab(binding.tabs.newTab().setText("🌦️ Météo"))
        binding.tabs.addTab(binding.tabs.newTab().setText("📔 Journal"))
        binding.tabs.addTab(binding.tabs.newTab().setText("📚 Plantes"))
        binding.tabs.addTab(binding.tabs.newTab().setText("💾 Export"))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SowingFragment())
                .commit()
        }

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val fragment = when (tab.position) {
                    0 -> SowingFragment()
                    1 -> MonthlyFragment()
                    2 -> WeatherFragment()
                    3 -> JournalFragment()
                    4 -> LibraryFragment()
                    5 -> ExportFragment()
                    else -> SowingFragment()
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}
