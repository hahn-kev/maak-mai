package org.hahn.maakmai.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.hahn.maakmai.data.source.local.MaakMaiDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * ContentProvider for serving attachment images from the database.
 */
class AttachmentProvider : ContentProvider() {
    private lateinit var database: MaakMaiDatabase
    
    companion object {
        private const val AUTHORITY = "org.hahn.maakmai.attachment"
        private const val ATTACHMENT_ID = 1
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "*", ATTACHMENT_ID)
        }
    }

    override fun onCreate(): Boolean {
        context?.let {
            database = Room.databaseBuilder(
                it.applicationContext,
                MaakMaiDatabase::class.java,
                "MaakMai.db"
            ).build()
            return true
        }
        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val match = uriMatcher.match(uri)
        if (match != ATTACHMENT_ID) {
            return null
        }

        // Extract the attachment ID from the URI
        val attachmentId = try {
            UUID.fromString(uri.lastPathSegment)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        // Create a cursor with the requested columns
        val cursor = MatrixCursor(projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))

        // Query the database for the attachment
        runBlocking {
            val attachment = database.attachmentDao().getAttachmentById(attachmentId)
            if (attachment != null) {
                val row = cursor.newRow()
                
                // Add the requested columns to the cursor
                projection?.forEach { column ->
                    when (column) {
                        OpenableColumns.DISPLAY_NAME -> row.add(attachment.title ?: "attachment_${attachmentId}")
                        OpenableColumns.SIZE -> row.add(attachment.data.size)
                    }
                } ?: run {
                    // If no projection is specified, add all columns
                    row.add(attachment.title ?: "attachment_${attachmentId}")
                    row.add(attachment.data.size)
                }
            }
        }

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return "image/*"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // This provider is read-only
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        // This provider is read-only
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        // This provider is read-only
        return 0
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val match = uriMatcher.match(uri)
        if (match != ATTACHMENT_ID) {
            return null
        }

        // Extract the attachment ID from the URI
        val attachmentId = try {
            UUID.fromString(uri.lastPathSegment)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        // Create a temporary file to store the attachment data
        val tempFile = File.createTempFile("attachment_${attachmentId}", ".tmp", context?.cacheDir)
        
        // Query the database for the attachment and write the data to the temporary file
        runBlocking {
            val attachment = database.attachmentDao().getAttachmentById(attachmentId)
            if (attachment != null) {
                FileOutputStream(tempFile).use { outputStream ->
                    outputStream.write(attachment.data)
                }
            } else {
                return@runBlocking null
            }
        }

        // Return a ParcelFileDescriptor for the temporary file
        return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}