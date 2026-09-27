package org.hahn.maakmai.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hahn.maakmai.data.source.local.MaakMaiDatabase
import org.hahn.maakmai.images.ImageOptimizer
import java.util.Locale
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MaakMaiDatabase,
    private val imageOptimizer: ImageOptimizer
) : ViewModel() {

    sealed interface OptimizeState {
        data object Idle : OptimizeState
        data object Running : OptimizeState
        data class Done(val message: String) : OptimizeState
    }

    private val _optimizeState = MutableStateFlow<OptimizeState>(OptimizeState.Idle)
    val optimizeState = _optimizeState.asStateFlow()

    fun optimizeImages() {
        if (_optimizeState.value == OptimizeState.Running) return
        _optimizeState.value = OptimizeState.Running
        viewModelScope.launch {
            val message = try {
                val before = databaseSize()
                val result = imageOptimizer.optimize()
                // Shrinking frees pages inside the file; VACUUM gives the space back
                withContext(Dispatchers.IO) { database.openHelper.writableDatabase.execSQL("VACUUM") }
                val after = databaseSize()
                "Shrank ${result.shrunk} ${plural(result.shrunk, "image")} and removed " +
                    "${result.removed} unused ${plural(result.removed, "image")}. " +
                    "Database: ${formatSize(before)} → ${formatSize(after)}."
            } catch (e: Exception) {
                e.printStackTrace()
                "Couldn't optimize images: ${e.message}"
            }
            _optimizeState.value = OptimizeState.Done(message)
        }
    }

    /** Size of the database file after merging the write-ahead log into it. */
    private suspend fun databaseSize(): Long = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        context.getDatabasePath("MaakMai.db").length()
    }

    private fun plural(count: Int, word: String) = if (count == 1) word else "${word}s"

    private fun formatSize(bytes: Long) = String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Force checkpoint to merge WAL into main DB file
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                
                val dbFile = context.getDatabasePath("MaakMai.db")
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(dbFile).use { input ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.close()
                val dbFile = context.getDatabasePath("MaakMai.db")
                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Delete WAL and SHM files to ensure they don't conflict with the imported DB
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                // Restart app to re-initialize everything
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                exitProcess(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
