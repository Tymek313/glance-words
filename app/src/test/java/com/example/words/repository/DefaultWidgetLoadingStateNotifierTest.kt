package com.example.words.repository

import com.example.words.coroutines.collectToListInBackground
import com.example.words.randomWidgetId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWidgetLoadingStateNotifierTest {

    private lateinit var synchronizer: DefaultWidgetLoadingStateNotifier

    @Before
    fun setUp() {
        synchronizer = DefaultWidgetLoadingStateNotifier()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when observing widget loading state_given it was not set to loading beforehand_then initial loading state should be emitted`() = runTest(UnconfinedTestDispatcher()) {
        val widgetId = randomWidgetId()

        val isWidgetLoadingValues = collectToListInBackground(synchronizer.observeIsWidgetLoading(widgetId))

        assertEquals(0, isWidgetLoadingValues.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when observing widget loading state_given it was set to loading beforehand_then initial loading state should be emitted`() = runTest(UnconfinedTestDispatcher()) {
        val widgetId = randomWidgetId()
        synchronizer.setIsWidgetLoading(widgetId)

        val isWidgetLoadingValues = collectToListInBackground(synchronizer.observeIsWidgetLoading(widgetId))

        assertEquals(1, isWidgetLoadingValues.size)
    }

    @Test
    fun `when observing multiple widgets loading state_given all were set to loading beforehand_then initial loading state should be emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            val widget1Id = randomWidgetId()
            val widget2Id = randomWidgetId()
            synchronizer.setIsWidgetLoading(widget1Id)
            synchronizer.setIsWidgetLoading(widget2Id)

            val isWidget1LoadingValues = collectToListInBackground(
                synchronizer.observeIsWidgetLoading(widget1Id)
            )
            val isWidget2LoadingValues = collectToListInBackground(
                synchronizer.observeIsWidgetLoading(widget2Id)
            )

            assertEquals(1, isWidget1LoadingValues.size)
            assertEquals(1, isWidget2LoadingValues.size)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when widget is set to loading_given the same widget is observed_loading state for this widget should be emitted`() = runTest(UnconfinedTestDispatcher()) {
        val widgetId = randomWidgetId()
        val isWidgetLoadingValues = collectToListInBackground(synchronizer.observeIsWidgetLoading(widgetId))

        synchronizer.setIsWidgetLoading(widgetId)

        assertEquals(1, isWidgetLoadingValues.size)
    }

    @Test
    fun `when widget is set to loading_given observing a different widget_loading state for this widget should not be emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            val isWidgetLoadingValues = collectToListInBackground(
                synchronizer.observeIsWidgetLoading(randomWidgetId())
            )

            synchronizer.setIsWidgetLoading(randomWidgetId())

            assertEquals(0, isWidgetLoadingValues.size)
        }
}
