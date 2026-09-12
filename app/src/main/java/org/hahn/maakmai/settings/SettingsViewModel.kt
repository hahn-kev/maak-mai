import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hahn.maakmai.data.source.local.MaakMaiDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MaakMaiDatabase
) : ViewModel() {

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
