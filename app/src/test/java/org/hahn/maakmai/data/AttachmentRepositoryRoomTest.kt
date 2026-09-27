package org.hahn.maakmai.data

import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.hahn.maakmai.data.source.local.MaakMaiDatabase
import org.hahn.maakmai.model.Attachment
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AttachmentRepositoryRoomTest {

    private lateinit var database: MaakMaiDatabase
    private lateinit var repository: AttachmentRepositoryRoom

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MaakMaiDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = AttachmentRepositoryRoom(database.attachmentDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `reads data larger than one chunk intact`() = runBlocking {
        // Over the ~2 MB row limit, and not a multiple of the chunk size
        val data = Random(42).nextBytes(3 * 1024 * 1024 + 123)
        val id = UUID.randomUUID()
        repository.create(Attachment(id = id, data = data, title = null))

        assertArrayEquals(data, repository.getData(id))
        assertEquals(data.size, repository.getAllInfo().single().size)
    }

    @Test
    fun `replaces data and deletes`() = runBlocking {
        val id = UUID.randomUUID()
        repository.create(Attachment(id = id, data = byteArrayOf(1, 2, 3), title = null))

        repository.replaceData(id, byteArrayOf(9))
        assertArrayEquals(byteArrayOf(9), repository.getData(id))

        repository.delete(id)
        assertNull(repository.getData(id))
    }
}
