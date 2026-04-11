package com.almanach.jardin.ui

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almanach.jardin.R
import com.almanach.jardin.data.GardenTask
import com.almanach.jardin.data.Plant
import com.almanach.jardin.data.PlantDatabase
import com.almanach.jardin.databinding.FragmentMonthlyBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ─── ViewModel ───────────────────────────────────────────────────────────────

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = PlantDatabase.get(app).gardenTaskDao()

    fun tasksFor(month: Int): Flow<List<GardenTask>> = dao.getTasksForMonth(month)

    fun addTask(month: Int, title: String) = viewModelScope.launch(Dispatchers.IO) {
        if (title.isNotBlank()) dao.insert(GardenTask(month = month, title = title.trim(), isDefault = false))
    }

    fun updateTask(task: GardenTask, newTitle: String) = viewModelScope.launch(Dispatchers.IO) {
        if (newTitle.isNotBlank()) dao.update(task.copy(title = newTitle.trim()))
    }

    fun toggleDone(task: GardenTask) = viewModelScope.launch(Dispatchers.IO) {
        dao.setDone(task.id, !task.done)
    }

    fun deleteTask(task: GardenTask) = viewModelScope.launch(Dispatchers.IO) {
        dao.delete(task)
    }

    fun populateDefaultTasksIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        if (dao.countDefaults() > 0) return@launch
        defaultOrchardTasks.forEach { dao.insert(it) }
    }

    companion object {
        // Calendrier verger bio — pommiers, poiriers, petits fruitiers
        // Rouille grillagée (RG) et carpocapse (CP) intégrés
        private val defaultOrchardTasks = listOf(
            // Janvier
            GardenTask(month=1, title="🍎 Taille d'hiver pommiers/poiriers (repos végétatif)", isDefault=true),
            GardenTask(month=1, title="🍎 Supprimer les momies de fruits sur les branches", isDefault=true),
            GardenTask(month=1, title="🍎 Traitement bouillie bordelaise sur blessures de taille", isDefault=true),
            GardenTask(month=1, title="🫐 Vérifier tuteurs et protections hivernales petits fruitiers", isDefault=true),
            // Février
            GardenTask(month=2, title="🍎 Fin de taille d'hiver avant le débourrement", isDefault=true),
            GardenTask(month=2, title="🍎 Traitement préventif bouillie bordelaise (tavelure)", isDefault=true),
            GardenTask(month=2, title="🫐 Tailler framboisiers et groseilliers (couper à 5 yeux)", isDefault=true),
            GardenTask(month=2, title="🐛 [CP] Poser des bandes de glu sur les troncs contre les fourmis", isDefault=true),
            // Mars
            GardenTask(month=3, title="🍎 Terminer la taille avant l'ouverture des bourgeons", isDefault=true),
            GardenTask(month=3, title="🍎 Apporter engrais au pied des arbres (compost, cendres)", isDefault=true),
            GardenTask(month=3, title="🐛 [CP] Surveiller premières attaques de pucerons", isDefault=true),
            GardenTask(month=3, title="🍓 Désherber et pailler le pied des fraisiers", isDefault=true),
            // Avril
            GardenTask(month=4, title="🍎 Floraison : ne plus tailler, éviter tout traitement", isDefault=true),
            GardenTask(month=4, title="🍎 Protéger fleurs contre gelées tardives si nécessaire", isDefault=true),
            GardenTask(month=4, title="🐛 [RG] Surveiller l'apparition de taches orangées sur feuilles (rouille grillagée)", isDefault=true),
            GardenTask(month=4, title="🐛 [RG] Traitement préventif soufre ou bouillie soufrée si rouille détectée", isDefault=true),
            GardenTask(month=4, title="🍓 Protéger fraisiers avec filet anti-oiseaux dès la floraison", isDefault=true),
            // Mai
            GardenTask(month=5, title="🍎 Attacher branches des fruitiers palissés à leur support", isDefault=true),
            GardenTask(month=5, title="🍎 Apporter engrais riche en potasse (cendres de bois)", isDefault=true),
            GardenTask(month=5, title="🐛 [CP] Poser pièges à phéromones contre le carpocapse", isDefault=true),
            GardenTask(month=5, title="🐛 [RG] Contrôler et renouveler traitement soufre si rouille présente", isDefault=true),
            GardenTask(month=5, title="⚠️ Saints de Glace (11-13 mai) : protéger jeunes plantations si gel annoncé", isDefault=true),
            GardenTask(month=5, title="🫐 Protéger groseilles et framboises avec filet anti-oiseaux", isDefault=true),
            GardenTask(month=5, title="🍎 Ne pas tailler avant le 15 juin", isDefault=true),
            // Juin
            GardenTask(month=6, title="🍎 Taille en vert à partir du 20-21 juin (solstice)", isDefault=true),
            GardenTask(month=6, title="🍎 Éclaircir les fruits sur arbres trop chargés", isDefault=true),
            GardenTask(month=6, title="🐛 [CP] Vérifier pièges à phéromones, noter les captures", isDefault=true),
            GardenTask(month=6, title="🐛 [CP] Traitement bio carpocapse si captures élevées (kaolin, spinosad)", isDefault=true),
            GardenTask(month=6, title="🫐 Couper tiges de framboisiers après récolte", isDefault=true),
            GardenTask(month=6, title="🍎 Arroser régulièrement les arbres plantés récemment", isDefault=true),
            // Juillet
            GardenTask(month=7, title="🍎 Taille d'été légère pour aérer et laisser passer la lumière", isDefault=true),
            GardenTask(month=7, title="🍎 Arroser tôt le matin ou en soirée (15-20 L/m² par semaine si sécheresse)", isDefault=true),
            GardenTask(month=7, title="🍎 Poser paillage pour conserver l'humidité", isDefault=true),
            GardenTask(month=7, title="🍎 Éclaircer les pommes (améliore calibre et qualité)", isDefault=true),
            GardenTask(month=7, title="🐛 [CP] Vérifier pièges, renouveler traitement kaolin si nécessaire", isDefault=true),
            GardenTask(month=7, title="🐛 Surveiller feuilles et rameaux (maladies, parasites)", isDefault=true),
            // Août
            GardenTask(month=8, title="🍎 Arroser abondamment (surtout jeunes arbres)", isDefault=true),
            GardenTask(month=8, title="🍎 Étayer les branches chargées (pommiers, poiriers)", isDefault=true),
            GardenTask(month=8, title="🍎 Ensacher poires et pommes pour protéger des nuisibles", isDefault=true),
            GardenTask(month=8, title="🫐 Couper tiges de framboisiers ayant porté fruits", isDefault=true),
            GardenTask(month=8, title="🐛 [CP] Poser pièges à guêpes/frelons (bouteille + sirop)", isDefault=true),
            GardenTask(month=8, title="🍎 Greffe en écusson possible ce mois", isDefault=true),
            // Septembre
            GardenTask(month=9, title="🍎 Récolter pommes précoces, poires, quetsches", isDefault=true),
            GardenTask(month=9, title="🍎 Taille en vert encore possible sur pommiers/poiriers", isDefault=true),
            GardenTask(month=9, title="🍎 Ramasser et détruire les fruits atteints de moniliose", isDefault=true),
            GardenTask(month=9, title="🫐 Couper cannes de framboisiers à 10 cm du sol", isDefault=true),
            GardenTask(month=9, title="🫐 Récolter mûres, noisettes, raisins", isDefault=true),
            GardenTask(month=9, title="🐛 Greffe en écusson avant le 15 septembre", isDefault=true),
            // Octobre
            GardenTask(month=10, title="🍎 Récolter pommes et poires de conservation (vers le 15/10)", isDefault=true),
            GardenTask(month=10, title="🍎 Stocker fruits à 10°C max, hygrométrie 70-80%", isDefault=true),
            GardenTask(month=10, title="🍎 Nettoyer arbres : couper branches mortes, malades, croisées", isDefault=true),
            GardenTask(month=10, title="🫐 Bouturer groseilliers, cassis, framboisiers", isDefault=true),
            GardenTask(month=10, title="🍎 Préparer trous de plantation pour novembre", isDefault=true),
            // Novembre
            GardenTask(month=11, title="🍎 Planter nouveaux fruitiers (à partir de la Ste-Catherine, 25 nov)", isDefault=true),
            GardenTask(month=11, title="🫐 Planter groseilliers, cassis, framboisiers (tailler à 5 yeux)", isDefault=true),
            GardenTask(month=11, title="🍎 Apporter engrais de fond (compost, fumier)", isDefault=true),
            GardenTask(month=11, title="🍎 Traiter arbres à noyaux à la bouillie bordelaise", isDefault=true),
            GardenTask(month=11, title="🍎 Supprimer branches mortes et fruits momifiés", isDefault=true),
            GardenTask(month=11, title="🍎 Préparer greffons pour le printemps", isDefault=true),
            // Décembre
            GardenTask(month=12, title="🍎 Repos végétatif — entretien et affûtage des outils", isDefault=true),
            GardenTask(month=12, title="🍎 Vérifier régulièrement les fruits en conservation", isDefault=true),
            GardenTask(month=12, title="🍎 Protéger jeunes arbres contre le gel si nécessaire", isDefault=true),
            GardenTask(month=12, title="📋 Planifier la taille et les plantations de la prochaine saison", isDefault=true)
        )
    }
}

// ─── Fragment ─────────────────────────────────────────────────────────────────

class MonthlyFragment : Fragment() {

    private var _binding: FragmentMonthlyBinding? = null
    private val binding get() = _binding!!
    private val plantVm: PlantViewModel by activityViewModels()
    private val taskVm: TaskViewModel by lazy {
        ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[TaskViewModel::class.java]
    }

    private var currentMonth = LocalDate.now().monthValue
    private var taskJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMonthlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskVm.populateDefaultTasksIfNeeded()

        val plantAdapter = MonthlyPlantAdapter()
        binding.recyclerPlants.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPlants.adapter = plantAdapter

        val taskAdapter = TaskAdapter(
            onToggle = { task -> taskVm.toggleDone(task) },
            onEdit = { task ->
                // Les tâches verger ne peuvent pas être modifiées
                if (task.isDefault) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage("Les tâches du calendrier verger ne peuvent pas être modifiées.")
                        .setPositiveButton("OK", null).show()
                } else {
                    showEditTaskDialog(task)
                }
            },
            onDelete = { task ->
                if (task.isDefault) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage("Les tâches du calendrier verger ne peuvent pas être supprimées.")
                        .setPositiveButton("OK", null).show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Supprimer cette tâche ?")
                        .setPositiveButton("Supprimer") { _, _ -> taskVm.deleteTask(task) }
                        .setNegativeButton("Annuler", null).show()
                }
            }
        )
        binding.recyclerTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTasks.adapter = taskAdapter

        updateHeader()
        loadPlants(plantAdapter)
        observeTasks(taskAdapter)

        binding.btnPrev.setOnClickListener {
            currentMonth = if (currentMonth == 1) 12 else currentMonth - 1
            updateHeader(); loadPlants(plantAdapter); observeTasks(taskAdapter)
        }
        binding.btnNext.setOnClickListener {
            currentMonth = if (currentMonth == 12) 1 else currentMonth + 1
            updateHeader(); loadPlants(plantAdapter); observeTasks(taskAdapter)
        }
        binding.btnToday.setOnClickListener {
            currentMonth = LocalDate.now().monthValue
            updateHeader(); loadPlants(plantAdapter); observeTasks(taskAdapter)
        }
        binding.btnAddTask.setOnClickListener { showAddTaskDialog() }
    }

    private fun updateHeader() {
        binding.tvMonthName.text     = monthName(currentMonth)
        binding.tvMonthSubtitle.text = "Semis et tâches — ${monthName(currentMonth)}"
        binding.btnToday.visibility  =
            if (currentMonth == LocalDate.now().monthValue) View.GONE else View.VISIBLE
    }

    private fun loadPlants(adapter: MonthlyPlantAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            val toSow = plantVm.plants.first()
                .filter { it.sowingMonths.split(",").mapNotNull { m -> m.trim().toIntOrNull() }.contains(currentMonth) }
                .sortedBy { it.name }
            if (_binding == null) return@launch
            adapter.submitList(toSow)
            binding.tvSowTitle.visibility  = if (toSow.isEmpty()) View.GONE else View.VISIBLE
            binding.emptyPlants.visibility = if (toSow.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun observeTasks(adapter: TaskAdapter) {
        taskJob?.cancel()
        taskJob = viewLifecycleOwner.lifecycleScope.launch {
            taskVm.tasksFor(currentMonth).collectLatest { tasks ->
                if (_binding == null) return@collectLatest
                adapter.submitList(tasks)
                binding.emptyTasks.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddTaskDialog() {
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val til    = layout.findViewById<TextInputLayout>(R.id.til_task)
        val etTask = layout.findViewById<TextInputEditText>(R.id.et_task)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nouvelle tâche — ${monthName(currentMonth)}")
            .setView(layout)
            .setPositiveButton("Ajouter") { _, _ ->
                val title = etTask.text?.toString()?.trim() ?: ""
                if (title.isEmpty()) til.error = "Requis"
                else taskVm.addTask(currentMonth, title)
            }
            .setNegativeButton("Annuler", null).show()
        etTask.requestFocus()
    }

    private fun showEditTaskDialog(task: GardenTask) {
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val til    = layout.findViewById<TextInputLayout>(R.id.til_task)
        val etTask = layout.findViewById<TextInputEditText>(R.id.et_task)
        etTask.setText(task.title)
        etTask.setSelection(task.title.length)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Modifier la tâche")
            .setView(layout)
            .setPositiveButton("Enregistrer") { _, _ ->
                val newTitle = etTask.text?.toString()?.trim() ?: ""
                if (newTitle.isEmpty()) til.error = "Requis"
                else taskVm.updateTask(task, newTitle)
            }
            .setNegativeButton("Annuler", null).show()
        etTask.requestFocus()
    }

    private fun monthName(month: Int) =
        LocalDate.of(2024, month, 1)
            .month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() }

    override fun onDestroyView() { taskJob?.cancel(); super.onDestroyView(); _binding = null }
}

// ─── Adapter plantes ──────────────────────────────────────────────────────────

class MonthlyPlantAdapter : ListAdapter<Plant, MonthlyPlantAdapter.VH>(DIFF) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji:       TextView = view.findViewById(R.id.tv_emoji)
        val name:        TextView = view.findViewById(R.id.tv_name)
        val expandIcon:  TextView = view.findViewById(R.id.tv_expand_icon)
        val header:      View     = view.findViewById(R.id.layout_header)
        val details:     View     = view.findViewById(R.id.layout_details)
        val latin:       TextView = view.findViewById(R.id.tv_latin)
        val detailsText: TextView = view.findViewById(R.id.tv_details)
        val germTemp:    TextView = view.findViewById(R.id.tv_germ_temp)
        val sun:         TextView = view.findViewById(R.id.tv_sun)
        val water:       TextView = view.findViewById(R.id.tv_water)
        val notes:       TextView = view.findViewById(R.id.tv_notes)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_monthly_plant, parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        holder.emoji.text = p.emoji
        holder.name.text  = p.name
        if (p.latinName.isNotEmpty()) { holder.latin.text = p.latinName; holder.latin.visibility = View.VISIBLE }
        else holder.latin.visibility = View.GONE
        holder.detailsText.text = "⏱ ${p.occupationDays} j sol  ↔️ ${p.spacingCm} cm  🌱 Germ. ${p.germinationDays} j"
        holder.germTemp.text    = "🌡️ Germination : ${p.germinationTempMin}–${p.germinationTempMax} °C"
        holder.sun.text   = when { p.sunExposure.contains("Plein", ignoreCase=true)->"☀️ Plein soleil"; p.sunExposure.contains("Mi", ignoreCase=true)->"⛅ Mi-ombre"; else->"🌑 Ombre" }
        holder.water.text = when { p.waterNeeds.contains("Élevé", ignoreCase=true)->"💧💧💧 Élevé"; p.waterNeeds.contains("Moyen", ignoreCase=true)->"💧💧 Moyen"; else->"💧 Faible" }
        if (p.notes.isNotEmpty()) { holder.notes.text = "📝 ${p.notes}"; holder.notes.visibility = View.VISIBLE }
        else holder.notes.visibility = View.GONE
        holder.details.visibility = View.GONE
        holder.expandIcon.text = "▼"
        holder.header.setOnClickListener {
            val expanded = holder.details.visibility == View.VISIBLE
            holder.details.visibility = if (expanded) View.GONE else View.VISIBLE
            holder.expandIcon.text = if (expanded) "▼" else "▲"
        }
    }
    companion object { val DIFF = object : DiffUtil.ItemCallback<Plant>() {
        override fun areItemsTheSame(a: Plant, b: Plant) = a.id == b.id
        override fun areContentsTheSame(a: Plant, b: Plant) = a == b
    }}
}

// ─── Adapter tâches ───────────────────────────────────────────────────────────

class TaskAdapter(
    private val onToggle: (GardenTask) -> Unit,
    private val onEdit:   (GardenTask) -> Unit,
    private val onDelete: (GardenTask) -> Unit
) : ListAdapter<GardenTask, TaskAdapter.VH>(DIFF) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.cb_task)
        val title:    TextView = view.findViewById(R.id.tv_task_title)
        val btnEdit:  View     = view.findViewById(R.id.btn_edit_task)
        val btnDel:   View     = view.findViewById(R.id.btn_delete_task)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = getItem(position)
        holder.checkbox.isChecked = task.done
        holder.title.text  = task.title
        holder.title.alpha = if (task.done) 0.4f else 1.0f
        // Tâches verger : icônes légèrement atténuées pour indiquer non-modifiables
        holder.btnEdit.alpha = if (task.isDefault) 0.3f else 1.0f
        holder.btnDel.alpha  = if (task.isDefault) 0.3f else 1.0f
        holder.checkbox.setOnClickListener { onToggle(task) }
        holder.btnEdit.setOnClickListener  { onEdit(task) }
        holder.btnDel.setOnClickListener   { onDelete(task) }
    }
    companion object { val DIFF = object : DiffUtil.ItemCallback<GardenTask>() {
        override fun areItemsTheSame(a: GardenTask, b: GardenTask) = a.id == b.id
        override fun areContentsTheSame(a: GardenTask, b: GardenTask) = a == b
    }}
}
