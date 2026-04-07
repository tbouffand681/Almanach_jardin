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

    private var currentMonth = LocalDate.now().monthValue

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
            updateHeader(); loadMonth(adapter)
        }
        binding.btnNext.setOnClickListener {
            currentMonth = if (currentMonth == 12) 1 else currentMonth + 1
            updateHeader(); loadMonth(adapter)
        }
        binding.btnToday.setOnClickListener {
            currentMonth = LocalDate.now().monthValue
            updateHeader(); loadMonth(adapter)
        }
    }

    private fun updateHeader() {
        binding.tvMonthName.text     = monthName(currentMonth)
        binding.tvMonthSubtitle.text = "Que semer en ${monthName(currentMonth)} ?"
        binding.btnToday.visibility  =
            if (currentMonth == LocalDate.now().monthValue) View.GONE else View.VISIBLE
        // Afficher les travaux du mois
        binding.tvGardenTasks.text = gardenTasks(currentMonth)
    }

    private fun loadMonth(adapter: MonthlyPlantAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            val allPlants = plantVm.plants.first()
            val toSow = allPlants
                .filter { it.sowingMonths.split(",").mapNotNull { m -> m.trim().toIntOrNull() }.contains(currentMonth) }
                .sortedBy { it.name }
            if (_binding == null) return@launch
            adapter.submitList(toSow)
            binding.emptyText.visibility = if (toSow.isEmpty()) View.VISIBLE else View.GONE
            binding.recycler.visibility  = if (toSow.isEmpty()) View.GONE  else View.VISIBLE
            binding.tvCount.text = if (toSow.isEmpty()) "" else
                "${toSow.size} plante${if (toSow.size > 1) "s" else ""} à semer"
        }
    }

    private fun monthName(month: Int) =
        LocalDate.of(2024, month, 1)
            .month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() }

    /**
     * Travaux du jardin et du verger par mois.
     * Contenu générique de jardinage bio (non reproduit depuis Terre Vivante).
     */
    private fun gardenTasks(month: Int): String = when (month) {
        1 -> "🌿 Potager : tailler les arbres fruitiers à pépins par temps doux, " +
             "planifier les rotations, commander les semences.\n" +
             "🍎 Verger : taille de formation des jeunes arbres, traitement bouillie bordelaise.\n" +
             "🛠 Autres : protéger les cultures sous châssis, préparer le compost."

        2 -> "🌿 Potager : premiers semis sous abri (tomates, poivrons), forcer la rhubarbe.\n" +
             "🍎 Verger : terminer la taille des arbres, traiter les maladies fongiques.\n" +
             "🛠 Autres : bêcher les planches de culture, apporter du compost mûr."

        3 -> "🌿 Potager : semis en pleine terre des radis, épinards, carottes. " +
             "Repiquer les salades et les oignons.\n" +
             "🍎 Verger : surveiller les pucerons, planter les arbustes.\n" +
             "🛠 Autres : désherber, pailler les premières plantations."

        4 -> "🌿 Potager : plantations de pommes de terre, semis de haricots sous abri. " +
             "Repiquer tomates et poivrons à l'abri du gel.\n" +
             "🍎 Verger : traiter contre la tavelure, tuteurer les jeunes arbres.\n" +
             "🛠 Autres : installer les filets anti-insectes, arroser si sol sec."

        5 -> "🌿 Potager : planter tomates, courgettes, concombres après les Saints de Glace (11-13 mai). " +
             "Semer les haricots et les courges en pleine terre.\n" +
             "🍎 Verger : éclaircir les fruits (pommiers, poiriers), surveiller les maladies.\n" +
             "🛠 Autres : pailler abondamment, mettre en place l'arrosage au goutte-à-goutte."

        6 -> "🌿 Potager : butter les pommes de terre, pincer les tomates, " +
             "récolter salades, radis, petits pois.\n" +
             "🍎 Verger : récolter les cerises, surveiller le mildiou.\n" +
             "🛠 Autres : arroser tôt le matin, désherber avant montée en graine."

        7 -> "🌿 Potager : arroser régulièrement, tuteurer les tomates, " +
             "semer les légumes d'automne (choux, poireaux). Récolter courgettes et haricots.\n" +
             "🍎 Verger : récolter abricots et premiers pêches, traiter contre la cloque.\n" +
             "🛠 Autres : pailler, récolter et congeler les herbes aromatiques."

        8 -> "🌿 Potager : semer les épinards, mâche, cresson pour l'automne. " +
             "Récolter tomates, haricots, courgettes en continu.\n" +
             "🍎 Verger : récolter pêches, prunes, premières pommes et poires.\n" +
             "🛠 Autres : surveiller les arrosages, commencer à désherber pour l'automne."

        9 -> "🌿 Potager : planter fraises et salades d'automne, arracher les pommes de terre, " +
             "semer l'ail dès la mi-septembre.\n" +
             "🍎 Verger : récolter pommes et poires, plantation de nouveaux arbres en fin de mois.\n" +
             "🛠 Autres : préparer le sol pour les cultures d'hiver, composter les résidus."

        10 -> "🌿 Potager : planter les bulbes (ail, oignons), semer les engrais verts. " +
              "Rentrer les dernières tomates vertes à mûrir.\n" +
              "🍎 Verger : planter arbres et arbustes, récolter les noix et châtaignes.\n" +
              "🛠 Autres : pailler les fraisiers, nettoyer les abris de jardin."

        11 -> "🌿 Potager : récolter poireaux, choux, carottes. Protéger les cultures fragiles.\n" +
              "🍎 Verger : débuter la taille des arbres à pépins par temps doux et sans gel.\n" +
              "🛠 Autres : rentrer les outils, entretenir le matériel, planifier l'année suivante."

        12 -> "🌿 Potager : récolter légumes racines et choux. " +
              "Commander les semences pour l'année prochaine.\n" +
              "🍎 Verger : taille des arbres par temps doux, appliquer la bouillie bordelaise.\n" +
              "🛠 Autres : amender le sol avec du compost, protéger les plantes du gel."

        else -> ""
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class MonthlyPlantAdapter : ListAdapter<Plant, MonthlyPlantAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji:   TextView = view.findViewById(R.id.tv_emoji)
        val name:    TextView = view.findViewById(R.id.tv_name)
        val latin:   TextView = view.findViewById(R.id.tv_latin)
        val details: TextView = view.findViewById(R.id.tv_details)
        val sun:     TextView = view.findViewById(R.id.tv_sun)
        val water:   TextView = view.findViewById(R.id.tv_water)
        val notes:   TextView = view.findViewById(R.id.tv_notes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_monthly_plant, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        holder.emoji.text = p.emoji
        holder.name.text  = p.name
        if (p.latinName.isNotEmpty()) {
            holder.latin.text = p.latinName; holder.latin.visibility = View.VISIBLE
        } else holder.latin.visibility = View.GONE

        holder.details.text = "⏱ ${p.occupationDays} j sol  ↔️ ${p.spacingCm} cm  🌱 Germ. ${p.germinationDays} j"
        holder.sun.text = when {
            p.sunExposure.contains("Plein", ignoreCase = true) -> "☀️ Plein soleil"
            p.sunExposure.contains("Mi", ignoreCase = true)    -> "⛅ Mi-ombre"
            else -> "🌑 Ombre"
        }
        holder.water.text = when {
            p.waterNeeds.contains("Élevé", ignoreCase = true) -> "💧💧💧 Arrosage élevé"
            p.waterNeeds.contains("Moyen", ignoreCase = true) -> "💧💧 Arrosage moyen"
            else -> "💧 Arrosage faible"
        }
        if (p.notes.isNotEmpty()) {
            holder.notes.text = "📝 ${p.notes}"; holder.notes.visibility = View.VISIBLE
        } else holder.notes.visibility = View.GONE
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Plant>() {
            override fun areItemsTheSame(a: Plant, b: Plant) = a.id == b.id
            override fun areContentsTheSame(a: Plant, b: Plant) = a == b
        }
    }
}
