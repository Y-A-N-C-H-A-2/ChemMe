/**
 * Chemical Detail: one character introduces itself, with its role, linked habits and source link.
 */
package com.example.chemme.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
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
        setUpTopBar(binding.topBar, "")

        val chemicalId = intent.getLongExtra(EXTRA_CHEMICAL_ID, 0L)
        lifecycleScope.launch {
            val chemical = viewModel.getChemical(chemicalId)
            if (chemical == null) finish() else show(chemical)
        }
    }

    private fun show(chemical: Chemical) = with(binding) {
        topBar.textTitle.text = chemical.name
        imageMascot.setImageResource(ChemicalIcons.drawableFor(chemical.iconName))
        // The block behind the character is a quarter-strength wash of its own colour.
        val colour = getColor(ChemicalIcons.colorFor(chemical.iconName))
        hero.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colour, HERO_ALPHA))

        textName.text = chemical.characterName
        textCategory.text = getString(R.string.detail_category, chemical.name, chemical.category)
        textHello.text = getString(R.string.detail_hello, chemical.characterName, chemical.name)
        textRole.text = chemical.role
        textSupportedBy.text = chemical.supportedBy
        textSource.text = getString(R.string.detail_source, Format.sourceName(chemical.sourceUrl))
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
        private const val HERO_ALPHA = 64 // Out of 255, about 25%.

        fun intent(context: Context, chemicalId: Long): Intent =
            Intent(context, ChemicalDetailActivity::class.java).putExtra(EXTRA_CHEMICAL_ID, chemicalId)
    }
}
