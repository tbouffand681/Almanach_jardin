package com.almanach.jardin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almanach.jardin.R
import com.almanach.jardin.data.Plant
import com.almanach.jardin.databinding.FragmentMonthlyBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class MonthlyFragment : Fragment() {

    private var _binding: FragmentMonthlyBinding? = null
    private val binding get() = _binding!!
    private val plantVm: PlantViewModel by activityViewModels()

    private var currentMonth = LocalDate.now().monthValue   // 1..12

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MonthlyPlantAdapter()
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        updateHeader()
        loadMonth(adapter)

        binding.btnPrev.setOnClickListener {
            currentMonth = if (currentMonth == 1) 12 else currentMonth - 1
            updateHeader()
            loadMonth(adapter)
        }

        binding.btnNext.setOnClickListener {
            currentMonth = if (currentMonth == 12) 1 else currentMonth + 1
            updateHeader()
            loadMonth(adapter)
        }

        // Bouton "Ce mois-ci" pour revenir au mois actuel
        binding.btnToday.setOnClickListener {
            currentMonth = LocalDate.now().monthValue
            updateHeader()
            loadMonth(adapter)
        }
    }

    private fun updateHeader() {
        val monthName = monthName(currentMonth)
        binding.tvMonthName.text = monthName
        binding.tvMonthSubtitle.text = "Que semer en $monthName ?"
        // Indiquer si c'est le mois en cours
        val isCurrentMonth = currentMonth == LocalDate.now().monthValue
        binding.btnToday.visibility = if (isCurrentMonth) View.GONE else View.VISIBLE
    }

    private fun loadMonth(adapter: MonthlyPlantAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            val allPlants = plantVm.plants.first()
            val toSow = allPlants
                .filter { plant ->
                    plant.sowingMonths
                        .split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .contains(currentMonth)
                }
                .sortedBy { it.name }

            if (_binding == null) return@launch
            adapter.submitList(toSow)
            if (toSow.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.recycler.visibility  = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.recycler.visibility  = View.VISIBLE
                binding.tvCount.text =
                    "${toSow.size} plante${if (toSow.size > 1) "s" else ""} à semer"
            }
        }
    }

    private fun monthName(month: Int): String =
        LocalDate.of(2024, month, 1)
            .month
            .getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class MonthlyPlantAdapter : ListAdapter<Plant, MonthlyPlantAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji:    TextView = view.findViewById(R.id.tv_emoji)
        val name:     TextView = view.findViewById(R.id.tv_name)
        val latin:    TextView = view.findViewById(R.id.tv_latin)
        val details:  TextView = view.findViewById(R.id.tv_details)
        val sun:      TextView = view.findViewById(R.id.tv_sun)
        val water:    TextView = view.findViewById(R.id.tv_water)
        val notes:    TextView = view.findViewById(R.id.tv_notes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_monthly_plant, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        holder.emoji.text = p.emoji
        holder.name.text  = p.name

        if (p.latinName.isNotEmpty()) {
            holder.latin.text       = p.latinName
            holder.latin.visibility = View.VISIBLE
        } else {
            holder.latin.visibility = View.GONE
        }

        holder.details.text = buildString {
            append("⏱ ${p.occupationDays} j sol")
            append("  ↔️ ${p.spacingCm} cm")
            append("  🌱 Germ. ${p.germinationDays} j")
        }

        holder.sun.text   = when {
            p.sunExposure.contains("Plein", ignoreCase = true) -> "☀️ Plein soleil"
            p.sunExposure.contains("Mi",    ignoreCase = true) -> "⛅ Mi-ombre"
            else -> "🌑 Ombre"
        }
        holder.water.text = when {
            p.waterNeeds.contains("Élevé", ignoreCase = true)  -> "💧💧💧 Arrosage élevé"
            p.waterNeeds.contains("Moyen", ignoreCase = true)  -> "💧💧 Arrosage moyen"
            else -> "💧 Arrosage faible"
        }

        if (p.notes.isNotEmpty()) {
            holder.notes.text       = "📝 ${p.notes}"
            holder.notes.visibility = View.VISIBLE
        } else {
            holder.notes.visibility = View.GONE
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Plant>() {
            override fun areItemsTheSame(a: Plant, b: Plant) = a.id == b.id
            override fun areContentsTheSame(a: Plant, b: Plant) = a == b
        }
    }
}
