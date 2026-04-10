package com.almanach.jardin.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.almanach.jardin.data.Plant
import com.almanach.jardin.databinding.DialogAddSowingBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class AddSowingDialog : BottomSheetDialogFragment() {

    private var _b: DialogAddSowingBinding? = null
    private val b get() = _b!!

    private val sowingVm: SowingViewModel by viewModels()
    private val plantVm: PlantViewModel by activityViewModels()

    private var selectedDate: LocalDate = LocalDate.now()
    private var plantList: List<Plant> = emptyList()
    private val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

    // Plante pré-sélectionnée (depuis la bibliothèque, optionnel)
    var preselectedPlantId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = DialogAddSowingBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.tvDate.text = selectedDate.format(fmt)

        // Charger la liste des plantes une seule fois
        viewLifecycleOwner.lifecycleScope.launch {
            plantList = plantVm.plants.first()
            val names = plantList.map { "${it.emoji} ${it.name}" }
            val adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_dropdown_item_1line, names)
            b.spinnerPlant.setAdapter(adapter)

            // Pré-sélection si venu de la fiche plante
            if (preselectedPlantId > 0) {
                val idx = plantList.indexOfFirst { it.id == preselectedPlantId }
                if (idx >= 0) {
                    b.spinnerPlant.setText(names[idx], false)
                    updateHarvestPreview(plantList[idx])
                }
            }

            b.spinnerPlant.setOnItemClickListener { _, _, position, _ ->
                updateHarvestPreview(plantList[position])
            }
        }

        // Sélecteur de date
        b.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d)
                b.tvDate.text = selectedDate.format(fmt)
                // Recalculer la récolte si une plante est déjà sélectionnée
                val name = b.spinnerPlant.text?.toString() ?: ""
                plantList.firstOrNull { "${it.emoji} ${it.name}" == name }
                    ?.let { updateHarvestPreview(it) }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        b.btnSave.setOnClickListener {
            val plantName = b.spinnerPlant.text?.toString() ?: ""
            val plant = plantList.firstOrNull { "${it.emoji} ${it.name}" == plantName }
            if (plant == null) {
                b.tilPlant.error = "Choisissez une plante"
                return@setOnClickListener
            }
            b.tilPlant.error = null
            b.btnSave.isEnabled = false

            sowingVm.addSowing(
                plantId = plant.id,
                sowingDate = selectedDate,
                location = b.etLocation.text?.toString()?.trim().orEmpty(),
                quantity = b.etQuantity.text?.toString()?.toIntOrNull() ?: 1,
                notes = b.etNotes.text?.toString()?.trim().orEmpty(),
                occupationDays = plant.occupationDays
            ) { success ->
                if (!isAdded) return@addSowing
                if (success) dismissAllowingStateLoss()
                else {
                    b.btnSave.isEnabled = true
                    Snackbar.make(requireView(), "Erreur lors de l'enregistrement", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        b.btnCancel.setOnClickListener { dismissAllowingStateLoss() }
    }

    private fun updateHarvestPreview(plant: Plant) {
        if (_b == null) return
        val harvest = selectedDate.plusDays(plant.occupationDays.toLong())
        b.tvHarvestPreview.text = "🗓 Récolte estimée : ${harvest.format(fmt)}"
        b.tvHarvestPreview.visibility = View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
