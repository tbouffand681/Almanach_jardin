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
import com.almanach.jardin.data.SowingWithPlant
import com.almanach.jardin.databinding.DialogAddSowingBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class EditSowingDialog : BottomSheetDialogFragment() {

    private var _b: DialogAddSowingBinding? = null
    private val b get() = _b!!

    private val sowingVm: SowingViewModel by viewModels()
    private val plantVm: PlantViewModel by activityViewModels()

    private var selectedDate: LocalDate = LocalDate.now()
    private var plantList: List<Plant> = emptyList()
    private val displayFmt = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.FRENCH)
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE

    companion object {
        private const val ARG_SOWING_ID  = "sowingId"
        private const val ARG_PLANT_ID   = "plantId"
        private const val ARG_PLANT_NAME = "plantName"
        private const val ARG_PLANT_EMOJI= "plantEmoji"
        private const val ARG_DATE       = "date"
        private const val ARG_LOCATION   = "location"
        private const val ARG_QUANTITY   = "quantity"
        private const val ARG_NOTES      = "notes"

        fun newInstance(sowing: SowingWithPlant) = EditSowingDialog().apply {
            arguments = Bundle().apply {
                putLong(ARG_SOWING_ID,   sowing.sowingId)
                putLong(ARG_PLANT_ID,    sowing.plantId)
                putString(ARG_PLANT_NAME, sowing.plantName)
                putString(ARG_PLANT_EMOJI,sowing.plantEmoji)
                putString(ARG_DATE,       sowing.sowingDate)
                putString(ARG_LOCATION,   sowing.location)
                putInt(ARG_QUANTITY,      sowing.quantity)
                putString(ARG_NOTES,      sowing.notes)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = DialogAddSowingBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val sowingId  = args.getLong(ARG_SOWING_ID)
        val plantId   = args.getLong(ARG_PLANT_ID)
        val plantName = args.getString(ARG_PLANT_NAME, "")
        val plantEmoji= args.getString(ARG_PLANT_EMOJI, "🌱")

        // Restaurer la date
        selectedDate = try {
            LocalDate.parse(args.getString(ARG_DATE, ""), isoFmt)
        } catch (e: Exception) { LocalDate.now() }
        b.tvDate.text = selectedDate.format(displayFmt)

        // Pré-remplir les champs simples
        b.etLocation.setText(args.getString(ARG_LOCATION, ""))
        b.etQuantity.setText(args.getInt(ARG_QUANTITY, 1).toString())
        b.etNotes.setText(args.getString(ARG_NOTES, ""))

        // Charger les plantes et pré-sélectionner la bonne
        viewLifecycleOwner.lifecycleScope.launch {
            plantList = plantVm.plants.first()
            val names = plantList.map { "${it.emoji} ${it.name}" }
            val adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_dropdown_item_1line, names)
            b.spinnerPlant.setAdapter(adapter)

            val preselected = "$plantEmoji $plantName"
            b.spinnerPlant.setText(preselected, false)

            // Afficher la récolte estimée avec les données actuelles
            plantList.firstOrNull { it.id == plantId }
                ?.let { updateHarvestPreview(it) }

            b.spinnerPlant.setOnItemClickListener { _, _, position, _ ->
                updateHarvestPreview(plantList[position])
            }
        }

        b.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d)
                b.tvDate.text = selectedDate.format(displayFmt)
                val name = b.spinnerPlant.text?.toString() ?: ""
                plantList.firstOrNull { "${it.emoji} ${it.name}" == name }
                    ?.let { updateHarvestPreview(it) }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        b.btnSave.setOnClickListener {
            val plantName2 = b.spinnerPlant.text?.toString() ?: ""
            val plant = plantList.firstOrNull { "${it.emoji} ${it.name}" == plantName2 }
            if (plant == null) { b.tilPlant.error = "Choisissez une plante"; return@setOnClickListener }
            b.tilPlant.error = null
            b.btnSave.isEnabled = false

            sowingVm.updateSowing(
                sowingId = sowingId,
                plantId = plant.id,
                sowingDate = selectedDate,
                location = b.etLocation.text?.toString()?.trim().orEmpty(),
                quantity = b.etQuantity.text?.toString()?.toIntOrNull() ?: 1,
                notes = b.etNotes.text?.toString()?.trim().orEmpty(),
                occupationDays = plant.occupationDays
            ) { success ->
                if (!isAdded) return@updateSowing
                if (success) dismissAllowingStateLoss()
                else {
                    b.btnSave.isEnabled = true
                    Snackbar.make(requireView(), "Erreur lors de la modification", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        b.btnCancel.setOnClickListener { dismissAllowingStateLoss() }
    }

    private fun updateHarvestPreview(plant: Plant) {
        if (_b == null) return
        val harvest = selectedDate.plusDays(plant.occupationDays.toLong())
        b.tvHarvestPreview.text = "🗓 Récolte estimée : ${harvest.format(displayFmt)}"
        b.tvHarvestPreview.visibility = View.VISIBLE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
