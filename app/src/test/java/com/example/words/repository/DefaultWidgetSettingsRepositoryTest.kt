package com.example.words.repository

import com.example.words.ProtoSettings
import com.example.words.ProtoWidget
import com.example.words.getRandomWidgetId
import com.example.words.model.Widget
import com.example.words.persistence.Persistence
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@MockKExtension.ConfirmVerification
class DefaultWidgetSettingsRepositoryTest {

    private lateinit var repository: DefaultWidgetSettingsRepository
    private lateinit var fakePersistence: Persistence<ProtoSettings>

    @Before
    fun setUp() {
        fakePersistence = mockk()
        repository = DefaultWidgetSettingsRepository(fakePersistence)
    }

    @Test
    fun `when settings are observed_given widget does not exist_null is returned`() = runTest {
        every { fakePersistence.data } returns flowOf(ProtoSettings.newBuilder().build())

        val settings = mutableListOf<Widget?>()
        repository.observeSettings(getRandomWidgetId()).toList(settings)

        assertNull(settings.single())
        verify { fakePersistence.data }
    }

    @Test
    fun `when settings are observed_given widget has ever been updated_settings for the requested widget are returned`() = runTest {
        val widgetId = getRandomWidgetId()
        val spreadsheetId = UUID.randomUUID().toString()
        val sheetId = Random.nextInt()
        val sheetName = UUID.randomUUID().toString()
        val lastUpdatedAt = Instant.parse("2007-12-03T10:15:30Z")
        every { fakePersistence.data } returns flowOf(
            ProtoSettings.newBuilder()
                .addWidgets(
                    ProtoWidget.newBuilder()
                        .setId(widgetId.value)
                        .setSpreadsheetId(spreadsheetId)
                        .setSheetId(sheetId)
                        .setSheetName(sheetName)
                        .setLastUpdatedAt(lastUpdatedAt.epochSecond)
                        .build()
                ).build()
        )

        val settings = mutableListOf<Widget?>()
        repository.observeSettings(widgetId).toList(settings)

        assertEquals(
            Widget(
                id = widgetId,
                spreadsheetId = spreadsheetId,
                sheetId = sheetId,
                sheetName = sheetName,
                lastUpdatedAt = lastUpdatedAt
            ),
            settings.single()
        )
        verify { fakePersistence.data }
    }

    @Test
    fun `when settings are observed_given widget has never been updated_settings for the requested widget are returned`() = runTest {
        val widgetId = getRandomWidgetId()
        val spreadsheetId = UUID.randomUUID().toString()
        val sheetId = Random.nextInt()
        val sheetName = UUID.randomUUID().toString()
        every { fakePersistence.data } returns flowOf(
            ProtoSettings.newBuilder()
                .addWidgets(
                    ProtoWidget.newBuilder()
                        .setId(widgetId.value)
                        .setSpreadsheetId(spreadsheetId)
                        .setSheetId(sheetId)
                        .setSheetName(sheetName)
                        .build()
                ).build()
        )

        val settings = mutableListOf<Widget?>()
        repository.observeSettings(widgetId).toList(settings)

        assertEquals(
            Widget(
                id = widgetId,
                spreadsheetId = spreadsheetId,
                sheetId = sheetId,
                sheetName = sheetName,
                lastUpdatedAt = null
            ),
            settings.single()
        )

        verify { fakePersistence.data }
    }

    @Test
    fun `when widget is added_it is persisted`() = runTest {
        val widget = getRandomWidget()
        val otherWidgetId = getRandomWidgetId()
        val expectedProtoSettings = ProtoSettings.newBuilder()
            .addWidgets(ProtoWidget.newBuilder().setId(otherWidgetId.value).build())
            .addWidgets(
                ProtoWidget.newBuilder()
                    .setId(widget.id.value)
                    .setSpreadsheetId(widget.spreadsheetId)
                    .setSheetId(widget.sheetId)
                    .setSheetName(widget.sheetName)
                    .build()
            )
            .build()
        var storedProtoSettings: ProtoSettings? = null
        coEvery { fakePersistence.updateData(captureLambda()) } coAnswers {
            lambda<suspend (ProtoSettings) -> ProtoSettings>()
                .captured
                .invoke(
                    ProtoSettings.newBuilder()
                        .addWidgets(ProtoWidget.newBuilder().setId(otherWidgetId.value).build())
                        .build()
                )
                .also { storedProtoSettings = it }
        }

        repository.addWidget(widget)

        assertEquals(expectedProtoSettings, storedProtoSettings)
        coVerify { fakePersistence.updateData(any()) }
    }

    @Test
    fun `when widget's last update timestamp is updated_updated timestamp is persisted`() = runTest {
        val widget = getRandomWidget()
        val newLastUpdatedAt = Instant.now()
        var storedSettings: ProtoSettings? = null
        val existingSettings = ProtoSettings.newBuilder()
            .addWidgets(
                ProtoWidget.newBuilder()
                    .setId(widget.id.value)
                    .setSpreadsheetId(widget.spreadsheetId)
                    .setSheetId(widget.sheetId)
                    .setSheetName(widget.sheetName)
                    .build()
            )
            .build()
        val expectedSettings = ProtoSettings.newBuilder()
            .addWidgets(
                ProtoWidget.newBuilder()
                    .setId(widget.id.value)
                    .setSpreadsheetId(widget.spreadsheetId)
                    .setSheetId(widget.sheetId)
                    .setSheetName(widget.sheetName)
                    .setLastUpdatedAt(newLastUpdatedAt.epochSecond)
                    .build()
            )
            .build()
        coEvery { fakePersistence.updateData(captureLambda()) } coAnswers {
            lambda<suspend (ProtoSettings) -> ProtoSettings>()
                .captured
                .invoke(existingSettings)
                .also { storedSettings = it }
        }

        repository.updateLastUpdatedAt(widget.id, newLastUpdatedAt)

        assertEquals(expectedSettings, storedSettings)
        coVerify { fakePersistence.updateData(any()) }
    }

    @Test
    fun `when widget's last update timestamp is updated_given widget does not exist_exception is thrown`() = runTest {
        coEvery { fakePersistence.updateData(captureLambda()) } coAnswers {
            lambda<suspend (ProtoSettings) -> ProtoSettings>().captured.invoke(ProtoSettings.getDefaultInstance())
        }
        var exceptionThrown = false

        try {
            repository.updateLastUpdatedAt(getRandomWidgetId(), Instant.now())
        } catch (e: DefaultWidgetSettingsRepository.WidgetDoesNotExistException) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)
        coVerify { fakePersistence.updateData(any()) }
    }

    @Test
    fun `when widget is deleted_it is removed from persistence`() = runTest {
        val widget = getRandomWidget()
        val otherWidgetId = getRandomWidgetId()
        var storedSettings: ProtoSettings? = null
        val existingSettings = ProtoSettings.newBuilder()
            .addWidgets(ProtoWidget.newBuilder().setId(widget.id.value).build())
            .addWidgets(ProtoWidget.newBuilder().setId(otherWidgetId.value).build())
            .build()
        val expectedSettings = ProtoSettings.newBuilder().addWidgets(
            ProtoWidget.newBuilder().setId(otherWidgetId.value)
        ).build()
        coEvery { fakePersistence.updateData(captureLambda()) } coAnswers {
            lambda<suspend (ProtoSettings) -> ProtoSettings>()
                .captured
                .invoke(existingSettings)
                .also { storedSettings = it }
        }

        repository.deleteWidget(widget.id)

        assertEquals(expectedSettings, storedSettings)
        coVerify { fakePersistence.updateData(any()) }
    }

    private companion object {
        fun getRandomWidget() = Widget(
            id = getRandomWidgetId(),
            spreadsheetId = UUID.randomUUID().toString(),
            sheetId = Random.nextInt(),
            sheetName = UUID.randomUUID().toString(),
            lastUpdatedAt = null
        )
    }
}