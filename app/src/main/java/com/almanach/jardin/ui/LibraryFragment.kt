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
import com.almanach.jardin.databinding.FragmentLibraryBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlantViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PlantAdapter(
            onClick = { plant ->
                // Ouvrir le dialog modification
                PlantDialog.edit(plant).show(childFragmentManager, "EditPlant")
            }
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        // Observer le Flow — mis à jour automatiquement par Room
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.plants.collectLatest { list ->
                adapter.submitList(list)
                binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // FAB → dialog ajout
        binding.fab.setOnClickListener {
            PlantDialog.add().show(childFragmentManager, "AddPlant")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class PlantAdapter(
    private val onClick: (Plant) -> Unit
) : ListAdapter<Plant, PlantAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji: TextView = view.findViewById(R.id.tv_emoji)
        val name: TextView  = view.findViewById(R.id.tv_name)
        val cat: TextView   = view.findViewById(R.id.tv_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val plant = getItem(position)
        holder.emoji.text = plant.emoji
        holder.name.text  = plant.name
        holder.cat.text   = plant.category
        holder.itemView.setOnClickListener { onClick(plant) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Plant>() {
            override fun areItemsTheSame(a: Plant, b: Plant) = a.id == b.id
            override fun areContentsTheSame(a: Plant, b: Plant) = a == b
        }
    }
}
