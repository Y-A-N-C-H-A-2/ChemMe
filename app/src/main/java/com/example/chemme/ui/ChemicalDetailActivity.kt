/**
 * Screen 6, Chemical Detail: one chemical's character, role, linked habits and source link.
 */
package com.example.chemme.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.chemme.R
import com.example.chemme.data.Chemical
import com.example.chemme.databinding.ActivityChemicalDetailBinding
import com.example.chemme.logic.LibraryViewModel
import kotlinx.coroutines.launch

class ChemicalDetailActivity : BackActivity() {
    private lateinit var binding: ActivityChemicalDetailBinding
    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChemicalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chemicalId = intent.getLongExtra(EXTRA_CHEMICAL_ID, 0L)
        lifecycleScope.launch {
            val chemical = viewModel.getChemical(chemicalId)
            if (chemical == null) finish() else show(chemical)
        }
    }

    private fun show(chemical: Chemical) = with(binding) {
        title = chemical.name
        face.setBackgroundResource(ChemicalIcons.drawableFor(chemical.iconName))
        face.text = chemical.characterName.take(1)
        textName.text = getString(R.string.insight_title, chemical.characterName, chemical.name)
        textCategory.text = chemical.category
        textRole.text = chemical.role
        textSupportedBy.text = chemical.supportedBy
        buttonSource.setOnClickListener { openSource(chemical.sourceUrl) }
    }

    /** An implicit Intent: ask Android for any app that can view a web address. */
    private fun openSource(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.message_no_browser, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val EXTRA_CHEMICAL_ID = "extra_chemical_id"

        fun intent(context: Context, chemicalId: Long): Intent =
            Intent(context, ChemicalDetailActivity::class.java).putExtra(EXTRA_CHEMICAL_ID, chemicalId)
    }
}
