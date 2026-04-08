package com.almanach.jardin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almanach.jardin.R
import com.almanach.jardin.data.NaturalEvent
import com.almanach.jardin.databinding.FragmentJournalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val J_ISO_FMT     = DateTimeFormatter.ISO_LOCAL_DATE
private val J_DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yy")

private fun fmtEventDate(iso: String): String = try {
    LocalDate.parse(iso, J_ISO_FMT).format(J_DISPLAY_FMT)
} catch (e: Exception) { iso }

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!
    private val vm: JournalViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = EventAdapter(
            onEditClick = { event ->
                EventDialog.edit(event).show(childFragmentManager, "EditEvent")
            },
            onDeleteClick = { event ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Supprimer cette observation ?")
                    .setMessage("\"${event.title}\" sera définitivement supprimée.")
                    .setPositiveButton("Supprimer") { _, _ -> vm.deleteEvent(event) {} }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            vm.events.collectLatest { list ->
                adapter.submitList(list)
                binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fab.setOnClickListener {
            EventDialog.add().show(childFragmentManager, "AddEvent")
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class EventAdapter(
    private val onEditClick: (NaturalEvent) -> Unit,
    private val onDeleteClick: (NaturalEvent) -> Unit
) : ListAdapter<NaturalEvent, EventAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji:    TextView = view.findViewById(R.id.tv_emoji)
        val title:    TextView = view.findViewById(R.id.tv_title)
        val date:     TextView = view.findViewById(R.id.tv_date)
        val category: TextView = view.findViewById(R.id.tv_category)
        val desc:     TextView = view.findViewById(R.id.tv_description)
        val location: TextView = view.findViewById(R.id.tv_location)
        val btnEdit:  View     = view.findViewById(R.id.btn_edit)
        val btnDelete:View     = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = getItem(position)
        holder.emoji.text    = e.category.emoji
        holder.title.text    = e.title
        holder.date.text     = fmtEventDate(e.eventDate)
        holder.category.text = e.category.label
        holder.desc.text     = e.description
        holder.desc.visibility = if (e.description.isNotEmpty()) View.VISIBLE else View.GONE
        holder.location.text = if (e.location.isNotEmpty()) "📍 ${e.location}" else ""
        holder.location.visibility = if (e.location.isNotEmpty()) View.VISIBLE else View.GONE
        holder.btnEdit.setOnClickListener   { onEditClick(e) }
        holder.btnDelete.setOnClickListener { onDeleteClick(e) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<NaturalEvent>() {
            override fun areItemsTheSame(a: NaturalEvent, b: NaturalEvent) = a.id == b.id
            override fun areContentsTheSame(a: NaturalEvent, b: NaturalEvent) = a == b
        }
    }
}
