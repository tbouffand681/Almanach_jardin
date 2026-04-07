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

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = PlantDatabase.get(app).gardenTaskDao()
    fun tasksFor(month: Int): Flow<List<GardenTask>> = dao.getTasksForMonth(month)
    fun addTask(month: Int, title: String) = viewModelScope.launch(Dispatchers.IO) {
        if (title.isNotBlank()) dao.insert(GardenTask(month = month, title = title.trim()))
    }
    fun toggleDone(task: GardenTask) = viewModelScope.launch(Dispatchers.IO) {
        dao.setDone(task.id, !task.done)
    }
    fun deleteTask(task: GardenTask) = viewModelScope.launch(Dispatchers.IO) {
        dao.delete(task)
    }
}

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

        val plantAdapter = MonthlyPlantAdapter()
        binding.recyclerPlants.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPlants.adapter = plantAdapter

        val taskAdapter = TaskAdapter(
            onToggle = { task -> taskVm.toggleDone(task) },
            onDelete = { task ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Supprimer cette tâche ?")
                    .setPositiveButton("Supprimer") { _, _ -> taskVm.deleteTask(task) }
                    .setNegativeButton("Annuler", null)
                    .show()
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
            .setNegativeButton("Annuler", null)
            .show()

        etTask.requestFocus()
    }

    private fun monthName(month: Int) =
        LocalDate.of(2024, month, 1)
            .month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() }

    override fun onDestroyView() { taskJob?.cancel(); super.onDestroyView(); _binding = null }
}

class MonthlyPlantAdapter : ListAdapter<Plant, MonthlyPlantAdapter.VH>(DIFF) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji: TextView = view.findViewById(R.id.tv_emoji)
        val name:  TextView = view.findViewById(R.id.tv_name)
        val latin: TextView = view.findViewById(R.id.tv_latin)
        val details: TextView = view.findViewById(R.id.tv_details)
        val sun:   TextView = view.findViewById(R.id.tv_sun)
        val water: TextView = view.findViewById(R.id.tv_water)
        val notes: TextView = view.findViewById(R.id.tv_notes)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_monthly_plant, parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        holder.emoji.text = p.emoji
        holder.name.text  = p.name
        if (p.latinName.isNotEmpty()) { holder.latin.text = p.latinName; holder.latin.visibility = View.VISIBLE }
        else holder.latin.visibility = View.GONE
        holder.details.text = "⏱ ${p.occupationDays} j sol  ↔️ ${p.spacingCm} cm  🌱 Germ. ${p.germinationDays} j"
        holder.sun.text   = when { p.sunExposure.contains("Plein", ignoreCase=true)->"☀️ Plein soleil"; p.sunExposure.contains("Mi", ignoreCase=true)->"⛅ Mi-ombre"; else->"🌑 Ombre" }
        holder.water.text = when { p.waterNeeds.contains("Élevé", ignoreCase=true)->"💧💧💧 Élevé"; p.waterNeeds.contains("Moyen", ignoreCase=true)->"💧💧 Moyen"; else->"💧 Faible" }
        if (p.notes.isNotEmpty()) { holder.notes.text = "📝 ${p.notes}"; holder.notes.visibility = View.VISIBLE }
        else holder.notes.visibility = View.GONE
    }
    companion object { val DIFF = object : DiffUtil.ItemCallback<Plant>() {
        override fun areItemsTheSame(a: Plant, b: Plant) = a.id == b.id
        override fun areContentsTheSame(a: Plant, b: Plant) = a == b
    }}
}

class TaskAdapter(
    private val onToggle: (GardenTask) -> Unit,
    private val onDelete: (GardenTask) -> Unit
) : ListAdapter<GardenTask, TaskAdapter.VH>(DIFF) {
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.cb_task)
        val title:    TextView = view.findViewById(R.id.tv_task_title)
        val btnDel:   View     = view.findViewById(R.id.btn_delete_task)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = getItem(position)
        holder.checkbox.isChecked = task.done
        holder.title.text = task.title
        holder.title.alpha = if (task.done) 0.4f else 1.0f
        holder.checkbox.setOnClickListener { onToggle(task) }
        holder.btnDel.setOnClickListener { onDelete(task) }
    }
    companion object { val DIFF = object : DiffUtil.ItemCallback<GardenTask>() {
        override fun areItemsTheSame(a: GardenTask, b: GardenTask) = a.id == b.id
        override fun areContentsTheSame(a: GardenTask, b: GardenTask) = a == b
    }}
}
