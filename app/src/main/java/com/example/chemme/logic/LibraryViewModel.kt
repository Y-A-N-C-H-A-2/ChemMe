/**
 * App logic for the chemical library: the full list, the search filter and one chemical's details.
 */
package com.example.chemme.logic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chemme.ChemMeApplication
import com.example.chemme.data.Chemical
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ChemMeApplication).repository
    private val query = MutableStateFlow("")

    /** Recomputed automatically whenever the table or the search text changes. */
    val chemicals: StateFlow<List<Chemical>> =
        combine(repository.getChemicals(), query) { all, search ->
            val text = search.trim()
            all.filter {
                it.name.contains(text, ignoreCase = true) ||
                    it.characterName.contains(text, ignoreCase = true) ||
                    it.category.contains(text, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun search(text: String) {
        query.value = text
    }

    suspend fun getChemical(id: Long): Chemical? = repository.getChemical(id)
}
