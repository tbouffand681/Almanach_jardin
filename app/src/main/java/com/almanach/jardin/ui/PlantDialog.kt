package com.almanach.jardin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.almanach.jardin.data.Plant
import com.almanach.jardin.databinding.DialogPlantBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class PlantDialog : BottomSheetDialogFragment() {

    private var _b: DialogPlantBinding? = null
    private val b get() = _b!!
    private val vm: PlantViewModel by activityViewModels()
    private var editPlant: Plant? = null

    companion object {
        private const val ARG_ID         = "id"
        private const val ARG_NAME       = "name"
        private const val ARG_LATIN      = "latin"
        private const val ARG_EMOJI      = "emoji"
        private const val ARG_CAT        = "cat"
        private const val ARG_MONTHS     = "months"
        private const val ARG_OCCUP      = "occup"
        private const val ARG_SPACING    = "spacing"
        private const val ARG_SUN        = "sun"
        private const val ARG_WATER      = "water"
        private const val ARG_GERM       = "germ"
        private const val ARG_TEMP_MIN   = "tempMin"
        private const val ARG_TEMP_MAX   = "tempMax"
        private const val ARG_NOTES      = "notes"
        private const val ARG_IS_DEFAULT = "isDefault"

        fun add() = PlantDialog()

        fun edit(plant: Plant) = PlantDialog().apply {
            arguments = Bundle().apply {
                putLong(ARG_ID, plant.id)
                putString(ARG_NAME, plant.name)
                putString(ARG_LATIN, plant.latinName)
                putString(ARG_EMOJI, plant.emoji)
                putString(ARG_CAT, plant.category)
                putString(ARG_MONTHS, plant.sowingMonths)
                putInt(ARG_OCCUP, plant.occupationDays)
                putInt(ARG_SPACING, plant.spacingCm)
                putString(ARG_SUN, plant.sunExposure)
                putString(ARG_WATER, plant.waterNeeds)
                putInt(ARG_GERM, plant.germinationDays)
                putInt(ARG_TEMP_MIN, plant.germinationTempMin)
                putInt(ARG_TEMP_MAX, plant.germinationTempMax)
                putString(ARG_NOTES, plant.notes)
                putBoolean(ARG_IS_DEFAULT, plant.isDefault)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = DialogPlantBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.spinnerCategory.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, PlantViewModel.CATEGORIES))
        b.spinnerSun.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, PlantViewModel.SUN_OPTIONS))
        b.spinnerWater.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, PlantViewModel.WATER_OPTIONS))

        arguments?.let { args ->
            val id = args.getLong(ARG_ID, 0L)
            if (id > 0L) {
                editPlant = Plant(
                    id              = id,
                    name            = args.getString(ARG_NAME, ""),
                    latinName       = args.getString(ARG_LATIN, ""),
                    emoji           = args.getString(ARG_EMOJI, "🌱"),
                    category        = args.getString(ARG_CAT, "Légume"),
                    sowingMonths    = args.getString(ARG_MONTHS, ""),
                    occupationDays  = args.getInt(ARG_OCCUP, 90),
                    spacingCm       = args.getInt(ARG_SPACING, 30),
                    sunExposure     = args.getString(ARG_SUN, "Plein soleil"),
                    waterNeeds      = args.getString(ARG_WATER, "Moyen"),
                    germinationDays = args.getInt(ARG_GERM, 10),
                    germinationTempMin = args.getInt(ARG_TEMP_MIN, 10),
                    germinationTempMax = args.getInt(ARG_TEMP_MAX, 25),
                    notes           = args.getString(ARG_NOTES, ""),
                    isDefault       = args.getBoolean(ARG_IS_DEFAULT, false)
                )
                b.tvTitle.text = "✏️ Modifier"
                b.etName.setText(editPlant!!.name)
                b.etLatin.setText(editPlant!!.latinName)
                b.etEmoji.setText(editPlant!!.emoji)
                b.spinnerCategory.setText(editPlant!!.category, false)
                b.etMonths.setText(editPlant!!.sowingMonths)
                b.etOccup.setText(editPlant!!.occupationDays.toString())
                b.etSpacing.setText(editPlant!!.spacingCm.toString())
                b.spinnerSun.setText(editPlant!!.sunExposure, false)
                b.spinnerWater.setText(editPlant!!.waterNeeds, false)
                b.etGerm.setText(editPlant!!.germinationDays.toString())
                b.etTempMin.setText(editPlant!!.germinationTempMin.toString())
                b.etTempMax.setText(editPlant!!.germinationTempMax.toString())
                b.etNotes.setText(editPlant!!.notes)
                b.btnDelete.visibility = View.VISIBLE
            }
        } ?: run {
            b.tvTitle.text = "🌱 Nouvelle plante"
            b.spinnerCategory.setText(PlantViewModel.CATEGORIES[0], false)
            b.spinnerSun.setText(PlantViewModel.SUN_OPTIONS[0], false)
            b.spinnerWater.setText(PlantViewModel.WATER_OPTIONS[1], false)
        }

        b.btnSave.setOnClickListener {
            val name = b.etName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) { b.tilName.error = "Le nom est requis"; return@setOnClickListener }
            b.tilName.error = null
            b.btnSave.isEnabled = false

            val plant = (editPlant ?: Plant(name = name)).copy(
                name               = name,
                latinName          = b.etLatin.text?.toString()?.trim().orEmpty(),
                emoji              = b.etEmoji.text?.toString()?.trim().orEmpty().ifEmpty { "🌱" },
                category           = b.spinnerCategory.text?.toString()?.trim().orEmpty().ifEmpty { "Légume" },
                sowingMonths       = b.etMonths.text?.toString()?.trim().orEmpty(),
                occupationDays     = b.etOccup.text?.toString()?.toIntOrNull() ?: 90,
                spacingCm          = b.etSpacing.text?.toString()?.toIntOrNull() ?: 30,
                sunExposure        = b.spinnerSun.text?.toString()?.trim().orEmpty().ifEmpty { "Plein soleil" },
                waterNeeds         = b.spinnerWater.text?.toString()?.trim().orEmpty().ifEmpty { "Moyen" },
                germinationDays    = b.etGerm.text?.toString()?.toIntOrNull() ?: 10,
                germinationTempMin = b.etTempMin.text?.toString()?.toIntOrNull() ?: 10,
                germinationTempMax = b.etTempMax.text?.toString()?.toIntOrNull() ?: 25,
                notes              = b.etNotes.text?.toString()?.trim().orEmpty()
            )

            val onDone: (Boolean) -> Unit = { success ->
                if (isAdded) {
                    if (success) dismissAllowingStateLoss()
                    else {
                        b.btnSave.isEnabled = true
                        Snackbar.make(requireView(), "Erreur lors de l'enregistrement", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            if (editPlant != null) vm.updatePlant(plant, onDone) else vm.addPlant(plant, onDone)
        }

        b.btnDelete.setOnClickListener {
            val plant = editPlant ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer ${plant.name} ?")
                .setMessage("Cette plante sera retirée de la bibliothèque.")
                .setPositiveButton("Supprimer") { _, _ ->
                    vm.deletePlant(plant) { if (isAdded) dismissAllowingStateLoss() }
                }
                .setNegativeButton("Annuler", null).show()
        }

        b.btnCancel.setOnClickListener { dismissAllowingStateLoss() }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
