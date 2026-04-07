package com.almanach.jardin.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.almanach.jardin.databinding.FragmentExportBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!
    private val vm: ExportViewModel by viewModels()

    // Lanceur pour choisir un fichier à importer
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> vm.importFromUri(uri) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnExport.setOnClickListener { doExport() }
        binding.btnImport.setOnClickListener { openFilePicker() }

        vm.result.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            vm.clearResult()
        }
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    private fun doExport() {
        binding.btnExport.isEnabled = false
        binding.progressExport.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val json = vm.buildExportJson()()   // appel de la lambda suspend
                val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val filename = "almanach_$date.json"

                val file = File(requireContext().cacheDir, filename)
                file.writeText(json, Charsets.UTF_8)

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Almanach du jardin — sauvegarde $date")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Partager la sauvegarde via…"))
            } catch (e: Exception) {
                Snackbar.make(binding.root, "❌ Erreur export : ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                if (_binding != null) {
                    binding.btnExport.isEnabled = true
                    binding.progressExport.visibility = View.GONE
                }
            }
        }
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/json", "text/plain", "application/octet-stream"
            ))
        }
        importLauncher.launch(Intent.createChooser(intent, "Choisir une sauvegarde Almanach"))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
