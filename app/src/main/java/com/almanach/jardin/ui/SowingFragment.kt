package com.almanach.jardin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almanach.jardin.R
import com.almanach.jardin.data.SowingWithPlant
import com.almanach.jardin.databinding.FragmentSowingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SowingFragment : Fragment() {

    private var _binding: FragmentSowingBinding? = null
    private val binding get() = _binding!!
    private val sowingVm: SowingViewModel by viewModels()
    private val plantVm: PlantViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SowingAdapter(
            onEditClick   = { sowing ->
                EditSowingDialog.newInstance(sowing)
                    .show(childFragmentManager, "EditSowing")
            },
            onStatusClick = { sowing -> showStatusDialog(sowing) },
            onDeleteClick = { sowing -> confirmDelete(sowing) }
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            sowingVm.sowings.collectLatest { list ->
                adapter.submitList(list)
                binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fab.setOnClickListener {
            AddSowingDialog().show(childFragmentManager, "AddSowing")
        }
    }

    private fun showStatusDialog(sowing: SowingWithPlant) {
        val labels = SowingViewModel.STATUS_LABELS.map { it.second }.toTypedArray()
        val currentIdx = SowingViewModel.STATUS_LABELS.indexOfFirst { it.first == sowing.status }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Statut de ${sowing.plantName}")
            .setSingleChoiceItems(labels, currentIdx) { dialog, which ->
                sowingVm.updateStatus(sowing.sowingId, SowingViewModel.STATUS_LABELS[which].first)
                dialog.dismiss()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmDelete(sowing: SowingWithPlant) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Supprimer ce semis ?")
            .setMessage("${sowing.plantEmoji} ${sowing.plantName} — ${fmtDate(sowing.sowingDate)}\nCette action est irréversible.")
            .setPositiveButton("Supprimer") { _, _ -> sowingVm.deleteSowing(sowing.sowingId) {} }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class SowingAdapter(
    private val onEditClick:   (SowingWithPlant) -> Unit,
    private val onStatusClick: (SowingWithPlant) -> Unit,
    private val onDeleteClick: (SowingWithPlant) -> Unit
) : ListAdapter<SowingWithPlant, SowingAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji:    TextView = view.findViewById(R.id.tv_emoji)
        val name:     TextView = view.findViewById(R.id.tv_plant_name)
        val date:     TextView = view.findViewById(R.id.tv_sowing_date)
        val harvest:  TextView = view.findViewById(R.id.tv_harvest_date)
        val location: TextView = view.findViewById(R.id.tv_location)
        val status:   TextView = view.findViewById(R.id.tv_status)
        val btnEdit:    View   = view.findViewById(R.id.btn_edit)
        val btnStatus:  View   = view.findViewById(R.id.btn_status)
        val btnDelete:  View   = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sowing, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = getItem(position)
        holder.emoji.text    = s.plantEmoji
        holder.name.text     = s.plantName
        holder.date.text     = "🌱 Semé le ${fmtDate(s.sowingDate)}"
        holder.harvest.text  = "🗓 Récolte ~${fmtDate(s.expectedHarvestDate)}"
        holder.location.text = if (s.location.isNotEmpty()) "📍 ${s.location}" else ""
        holder.location.visibility = if (s.location.isNotEmpty()) View.VISIBLE else View.GONE
        holder.status.text   = SowingViewModel.statusLabel(s.status)
        holder.status.setTextColor(SowingViewModel.statusColor(s.status))
        holder.btnEdit.setOnClickListener   { onEditClick(s) }
        holder.btnStatus.setOnClickListener { onStatusClick(s) }
        holder.btnDelete.setOnClickListener { onDeleteClick(s) }
    }

    companion object {
        private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE
        private val displayFmt = DateTimeFormatter.ofPattern("dd/MM/yy")

        fun fmtDate(iso: String): String = try {
            LocalDate.parse(iso, isoFmt).format(displayFmt)
        } catch (e: Exception) { iso }

        val DIFF = object : DiffUtil.ItemCallback<SowingWithPlant>() {
            override fun areItemsTheSame(a: SowingWithPlant, b: SowingWithPlant) = a.sowingId == b.sowingId
            override fun areContentsTheSame(a: SowingWithPlant, b: SowingWithPlant) = a == b
        }
    }
}
