/**
 * Fills the Chemical and InsightRule tables from the CSV files in assets the first time the app runs.
 */
package com.example.chemme.data

import android.content.Context
import androidx.room.withTransaction
import java.io.Reader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat

/**
 * Keeping the science text in CSV files means a new chemical or rule is a new line in a file,
 * with no Kotlin change. A CSV library is used because the text fields contain commas.
 */
class SeedImporter(
    private val context: Context,
    private val database: ChemMeDatabase
) {
    // Two screens can ask for the import at the same moment; the mutex lets only one run it.
    private val mutex = Mutex()

    private val format = CSVFormat.DEFAULT.builder()
        .setHeader() // The first line supplies the column names.
        .setSkipHeaderRecord(true)
        .build()

    /** Does nothing when chemicals already exist, so restarting the app never imports twice. */
    suspend fun importIfEmpty() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (database.chemicalDao().count() > 0) return@withLock

            val chemicals = context.assets.open("chemicals.csv").bufferedReader().use { readChemicals(it) }
            // One transaction: either both tables are filled or neither is.
            database.withTransaction {
                database.chemicalDao().insertAll(chemicals)
                // Rules name their chemical; look up the ids Room has just generated.
                val idByName = database.chemicalDao().getAllOnce().associate { it.name to it.chemicalId }
                val rules = context.assets.open("rules.csv").bufferedReader().use { readRules(it, idByName) }
                database.insightDao().insertRules(rules)
            }
        }
    }

    private fun readChemicals(reader: Reader): List<Chemical> =
        format.parse(reader).use { csv ->
            csv.map { row ->
                Chemical(
                    name = row.get("name"),
                    characterName = row.get("characterName"),
                    category = row.get("category"),
                    role = row.get("role"),
                    supportedBy = row.get("supportedBy"),
                    iconName = row.get("iconName"),
                    sourceUrl = row.get("sourceUrl")
                )
            }
        }

    private fun readRules(reader: Reader, idByName: Map<String, Long>): List<InsightRule> =
        format.parse(reader).use { csv ->
            csv.map { row ->
                val chemicalName = row.get("chemical")
                InsightRule(
                    chemicalId = requireNotNull(idByName[chemicalName]) {
                        "rules.csv mentions '$chemicalName', which is not in chemicals.csv"
                    },
                    habitField = row.get("habitField"),
                    comparator = row.get("comparator"),
                    threshold = row.get("threshold").toDouble(),
                    message = row.get("message"),
                    sourceUrl = row.get("sourceUrl")
                )
            }
        }
}
