/**
 * Screen 5, Chemical Library: a searchable RecyclerView of every chemical and its character.
 */
package com.example.chemme.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chemme.data.Chemical
import com.example.chemme.databinding.ActivityLibraryBinding
import com.example.chemme.logic.LibraryViewModel
import kotlinx.coroutines.launch

class LibraryActivity : BackActivity() {
    private lateinit var binding: ActivityLibraryBinding
    private val viewModel: LibraryViewModel by viewModels()

    private val adapter = ChemicalAdapter { chemical ->
        startActivity(ChemicalDetailActivity.intent(this, chemical.chemicalId))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerChemicals.layoutManager = LinearLayoutManager(this)
        binding.recyclerChemicals.adapter = adapter

        // Every keystroke updates the query; the ViewModel filters and the list below redraws.
        binding.editSearch.doAfterTextChanged { text -> onSearch(text?.toString().orEmpty()) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chemicals.collect { chemicals -> showChemicals(chemicals) }
            }
        }
    }

    private fun onSearch(query: String) = viewModel.search(query)

    private fun showChemicals(chemicals: List<Chemical>) {
        adapter.submitList(chemicals)
        // Only say "no match" when the user has typed something; an empty box means still loading.
        binding.textNoResults.isVisible = chemicals.isEmpty() && binding.editSearch.text.isNotBlank()
    }
}
