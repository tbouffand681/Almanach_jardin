package com.almanach.jardin.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.almanach.jardin.data.EventCategory
import com.almanach.jardin.data.NaturalEvent
import com.almanach.jardin.databinding.DialogEventBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class EventDialog : BottomSheetDialogFragment() {

    private var _b: DialogEventBinding? = null
    private val b get() = _b!!
    private val vm: JournalViewModel by viewModels()

    private var editEvent: NaturalEvent? = null
    private var selectedDate: LocalDate = LocalDate.now()
    private val displayFmt = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.FRENCH)
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE

    companion object {
        private const val ARG_ID       = "id"
        private const val ARG_DATE     = "date"
        private const val ARG_CAT      = "cat"
        private const val ARG_TITLE    = "title"
        private const val ARG_DESC     = "desc"
        private const val ARG_LOCATION = "location"

        fun add() = EventDialog()

        fun edit(event: NaturalEvent) = EventDialog().apply {
            arguments = Bundle().apply {
                putLong(ARG_ID,       event.id)
                putString(ARG_DATE,   event.eventDate)
                putString(ARG_CAT,    event.category.name)
                putString(ARG_TITLE,  event.title)
                putString(ARG_DESC,   event.description)
                putString(ARG_LOCATION, event.location)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = DialogEventBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dropdown catégories
        val categoryLabels = JournalViewModel.CATEGORIES.map { "${it.emoji} ${it.label}" }
        b.spinnerCategory.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, categoryLabels))

        // Pré-remplir si modification
        arguments?.let { args ->
            val id = args.getLong(ARG_ID, 0L)
            if (id > 0L) {
                val cat = try { EventCategory.valueOf(args.getString(ARG_CAT, "FAUNA")) }
                          catch (e: Exception) { EventCategory.FAUNA }
                editEvent = NaturalEvent(
                    id          = id,
                    eventDate   = args.getString(ARG_DATE, LocalDate.now().format(isoFmt))!!,
                    category    = cat,
                    title       = args.getString(ARG_TITLE, "")!!,
                    description = args.getString(ARG_DESC, "")!!,
                    location    = args.getString(ARG_LOCATION, "")!!
                )
                b.tvTitle.text = "✏️ Modifier l'observation"
                selectedDate = try { LocalDate.parse(editEvent!!.eventDate, isoFmt) }
                              catch (e: Exception) { LocalDate.now() }
                val catIdx = JournalViewModel.CATEGORIES.indexOf(editEvent!!.category)
                b.spinnerCategory.setText(categoryLabels[catIdx.coerceAtLeast(0)], false)
                b.etTitle.setText(editEvent!!.title)
                b.etDescription.setText(editEvent!!.description)
                b.etLocation.setText(editEvent!!.location)
            }
        } ?: run {
            b.tvTitle.text = "📝 Nouvelle observation"
            b.spinnerCategory.setText(categoryLabels[0], false) // Flore par défaut
        }

        b.tvDate.text = selectedDate.format(displayFmt)

        b.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d)
                b.tvDate.text = selectedDate.format(displayFmt)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        b.btnSave.setOnClickListener {
            val title = b.etTitle.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) { b.tilTitle.error = "Le titre est requis"; return@setOnClickListener }
            b.tilTitle.error = null
            b.btnSave.isEnabled = false

            // Retrouver la catégorie sélectionnée
            val labelSelected = b.spinnerCategory.text?.toString() ?: categoryLabels[0]
            val category = JournalViewModel.CATEGORIES.firstOrNull {
                "${it.emoji} ${it.label}" == labelSelected
            } ?: EventCategory.OTHER

            val event = (editEvent ?: NaturalEvent(
                eventDate = selectedDate.format(isoFmt),
                category = category,
                title = title
            )).copy(
                eventDate   = selectedDate.format(isoFmt),
                category    = category,
                title       = title,
                description = b.etDescription.text?.toString()?.trim().orEmpty(),
                location    = b.etLocation.text?.toString()?.trim().orEmpty()
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

            if (editEvent != null) vm.updateEvent(event, onDone)
            else vm.addEvent(event, onDone)
        }

        b.btnCancel.setOnClickListener { dismissAllowingStateLoss() }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
