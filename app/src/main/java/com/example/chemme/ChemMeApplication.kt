/**
 * Application setup for ChemMe. Creates the shared Room database and repository when first needed.
 */
package com.example.chemme

import android.app.Application
import androidx.room.Room
import com.example.chemme.data.ChemMeDatabase
import com.example.chemme.data.ChemMeRepository
import com.example.chemme.data.SeedImporter

/** Owns one database/repository instance for the entire application process. */
class ChemMeApplication : Application() {
    val repository: ChemMeRepository by lazy {
        val database = Room.databaseBuilder(
            applicationContext, ChemMeDatabase::class.java, "chemme.db"
        ).build()
        ChemMeRepository(database, SeedImporter(applicationContext, database))
    }
}
