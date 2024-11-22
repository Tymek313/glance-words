package com.example.words.repository

import com.example.words.coroutines.collectToListInBackground
import com.example.words.domain.DefaultWidgetLoadingStateNotifier
import com.example.words.fixture.randomWidgetId
import com.example.words.fixture.widgetIdFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWidgetLoadingStateNotifierTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var notifier: DefaultWidgetLoadingStateNotifier

    @Before
    fun setUp() {
        notifier = DefaultWidgetLoadingStateNotifier()
    }

    @Test
    fun `when observing widget loading state_given it was not set to loading beforehand_then non-loading value is emitted`() = runTest(dispatcher) {
        backgroundScope.launch { notifier.setLoadingWidgetForAction(randomWidgetId()) { suspendCoroutine { } } }

        val isWidgetLoadingValues = collectToListInBackground(notifier.observeIsWidgetLoading(randomWidgetId()))

        assertFalse(isWidgetLoadingValues.single())
    }

    @Test
    fun `when observing widget loading state_given it was set to loading beforehand_then loading value is emitted`() = runTest(dispatcher) {
        backgroundScope.launch { notifier.setLoadingWidgetForAction(widgetIdFixture) { suspendCoroutine { } } }

        val isWidgetLoadingValues = collectToListInBackground(notifier.observeIsWidgetLoading(widgetIdFixture))

        assertTrue(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_then loading value is emitted`() = runTest(dispatcher) {
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeIsWidgetLoading(widgetIdFixture).drop(1))

        backgroundScope.launch { notifier.setLoadingWidgetForAction(widgetIdFixture) { suspendCoroutine { } } }

        assertTrue(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_given action has not finished_action is invoked after emitting loading`() = runTest(dispatcher) {
        var actionCalledAfterEmission = false
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeIsWidgetLoading(widgetIdFixture).drop(1))

        backgroundScope.launch {
            notifier.setLoadingWidgetForAction(widgetIdFixture) {
                yield()
                actionCalledAfterEmission = isWidgetLoadingValues.isNotEmpty()
                suspendCoroutine { }
            }
        }

        assertTrue(actionCalledAfterEmission)
    }

    @Test
    fun `when widget is set to loading_given action has finished_then non-loading value is emitted`() = runTest(dispatcher) {
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeIsWidgetLoading(widgetIdFixture).drop(2))

        notifier.setLoadingWidgetForAction(widgetIdFixture) { }

        assertFalse(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_given action has finished_then non-loading value is emitted after action finished`() = runTest(dispatcher) {
        var actionCalledBeforeEmission = false
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeIsWidgetLoading(widgetIdFixture).drop(2))

        notifier.setLoadingWidgetForAction(widgetIdFixture) {
            yield()
            actionCalledBeforeEmission = isWidgetLoadingValues.isEmpty()
        }

        assertTrue(actionCalledBeforeEmission)
    }
}
