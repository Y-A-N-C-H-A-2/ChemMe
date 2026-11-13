/**
 * The three RecyclerView adapters: saved days, insight cards and chemicals.
 * Each one turns a list of objects into recycled row views and reports taps back to its activity.
 */
package com.example.chemme.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chemme.R
import com.example.chemme.data.Chemical
import com.example.chemme.data.HistoryRow
import com.example.chemme.data.InsightCard
import com.example.chemme.databinding.ItemChemicalBinding
import com.example.chemme.databinding.ItemInsightBinding
import com.example.chemme.databinding.ItemLogBinding
import java.time.LocalDate

/**
 * History rows. A ViewHolder keeps references to one row's views, and RecyclerView reuses the
 * holders that scroll off screen, so only a screenful of rows is ever created.
 */
class LogAdapter(private val onClick: (HistoryRow) -> Unit) :
    ListAdapter<HistoryRow, LogAdapter.Holder>(Diff) {

    class Holder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    /** Inflates item_log.xml when RecyclerView needs another holder, not for every bind. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    /** Replaces a reused row's contents with the day at this position. */
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val log = row.log
        val date = LocalDate.parse(log.logDate)
        with(holder.binding) {
            textDay.text = Format.dayName(date)
            textNumber.text = Format.dayNumber(date)
            textMonth.text = Format.monthName(date)
            textSummary.text = root.context.getString(R.string.history_row, log.mood, Format.hours(log.sleepHours))

            // Up to three faces; a reused row must also hide the ones it no longer needs.
            val faces = listOf(mascot1, mascot2, mascot3)
            faces.forEachIndexed { index, view ->
                val icon = row.icons.getOrNull(index)
                view.isVisible = icon != null
                if (icon != null) view.setImageResource(ChemicalIcons.drawableFor(icon))
            }
            val extra = row.icons.size - faces.size
            textMore.isVisible = extra > 0
            textMore.text = root.context.getString(R.string.history_more, extra)
            rowMascots.isVisible = row.icons.isNotEmpty()

            root.setOnClickListener { onClick(row) }
        }
    }

    // ListAdapter uses these to work out which rows changed and animate only those.
    private object Diff : DiffUtil.ItemCallback<HistoryRow>() {
        override fun areItemsTheSame(old: HistoryRow, new: HistoryRow) = old.log.logId == new.log.logId
        override fun areContentsTheSame(old: HistoryRow, new: HistoryRow) = old == new
    }
}

/** Insight cards on the Insights screen. */
class InsightAdapter(private val onClick: (InsightCard) -> Unit) :
    ListAdapter<InsightCard, InsightAdapter.Holder>(Diff) {

    class Holder(val binding: ItemInsightBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemInsightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val card = getItem(position)
        with(holder.binding) {
            val context = root.context
            imageMascot.setImageResource(ChemicalIcons.drawableFor(card.iconName))
            textTitle.text = context.getString(R.string.insight_title, card.characterName, card.chemicalName)
            textMessage.text = card.message
            textSource.text = context.getString(R.string.source_label, Format.sourceName(card.sourceUrl))
            root.setOnClickListener { onClick(card) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<InsightCard>() {
        override fun areItemsTheSame(old: InsightCard, new: InsightCard) = old.ruleId == new.ruleId
        override fun areContentsTheSame(old: InsightCard, new: InsightCard) = old == new
    }
}

/** Character cards in the library grid. */
class ChemicalAdapter(private val onClick: (Chemical) -> Unit) :
    ListAdapter<Chemical, ChemicalAdapter.Holder>(Diff) {

    class Holder(val binding: ItemChemicalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemChemicalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val chemical = getItem(position)
        with(holder.binding) {
            imageMascot.setImageResource(ChemicalIcons.drawableFor(chemical.iconName))
            textName.text = chemical.characterName
            textChemical.text = chemical.name
            textCategory.text = chemical.category
            root.setOnClickListener { onClick(chemical) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Chemical>() {
        override fun areItemsTheSame(old: Chemical, new: Chemical) = old.chemicalId == new.chemicalId
        override fun areContentsTheSame(old: Chemical, new: Chemical) = old == new
    }
}
