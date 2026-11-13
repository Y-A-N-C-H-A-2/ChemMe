/**
 * The three RecyclerView adapters: saved days, insight cards and chemicals.
 * Each one turns a list of objects into recycled row views and reports taps back to its activity.
 */
package com.example.chemme.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chemme.R
import com.example.chemme.data.Chemical
import com.example.chemme.data.DailyLog
import com.example.chemme.data.InsightCard
import com.example.chemme.databinding.ItemChemicalBinding
import com.example.chemme.databinding.ItemInsightBinding
import com.example.chemme.databinding.ItemLogBinding

/**
 * History rows. A ViewHolder keeps references to one row's views, and RecyclerView reuses the
 * holders that scroll off screen, so only a screenful of rows is ever created.
 */
class LogAdapter(private val onClick: (DailyLog) -> Unit) :
    ListAdapter<DailyLog, LogAdapter.Holder>(Diff) {

    class Holder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    /** Inflates item_log.xml when RecyclerView needs another holder, not for every bind. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    /** Replaces a reused row's contents with the log at this position. */
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val log = getItem(position)
        with(holder.binding) {
            textDate.text = Format.date(log.logDate)
            textSummary.text = root.context.getString(R.string.history_row, log.mood, Format.hours(log.sleepHours))
            root.setOnClickListener { onClick(log) }
        }
    }

    // ListAdapter uses these to work out which rows changed and animate only those.
    private object Diff : DiffUtil.ItemCallback<DailyLog>() {
        override fun areItemsTheSame(old: DailyLog, new: DailyLog) = old.logId == new.logId
        override fun areContentsTheSame(old: DailyLog, new: DailyLog) = old == new
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
            face.setBackgroundResource(ChemicalIcons.drawableFor(card.iconName))
            face.text = card.characterName.take(1)
            textTitle.text = root.context.getString(R.string.insight_title, card.characterName, card.chemicalName)
            textMessage.text = card.message
            root.setOnClickListener { onClick(card) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<InsightCard>() {
        override fun areItemsTheSame(old: InsightCard, new: InsightCard) = old.ruleId == new.ruleId
        override fun areContentsTheSame(old: InsightCard, new: InsightCard) = old == new
    }
}

/** Chemical rows in the library. */
class ChemicalAdapter(private val onClick: (Chemical) -> Unit) :
    ListAdapter<Chemical, ChemicalAdapter.Holder>(Diff) {

    class Holder(val binding: ItemChemicalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemChemicalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val chemical = getItem(position)
        with(holder.binding) {
            face.setBackgroundResource(ChemicalIcons.drawableFor(chemical.iconName))
            face.text = chemical.characterName.take(1)
            textName.text = root.context.getString(R.string.insight_title, chemical.characterName, chemical.name)
            textCategory.text = chemical.category
            root.setOnClickListener { onClick(chemical) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Chemical>() {
        override fun areItemsTheSame(old: Chemical, new: Chemical) = old.chemicalId == new.chemicalId
        override fun areContentsTheSame(old: Chemical, new: Chemical) = old == new
    }
}
