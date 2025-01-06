package com.example.words.synchronization

import com.example.words.coroutines.collectToListInBackground
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
class DefaultWordsSynchronizationStateNotifierTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var notifier: DefaultWordsSynchronizationStateNotifier

    @Before
    fun setUp() {
        notifier = DefaultWordsSynchronizationStateNotifier()
    }

    @Test
    fun `when observing widget loading state_given it was not set to loading beforehand_then non-loading value is emitted`() = runTest(dispatcher) {
        backgroundScope.launch { notifier.notifyWordsSynchronizationForAction(randomWidgetId()) { suspendCoroutine { } } }

        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(randomWidgetId()))

        assertFalse(isWidgetLoadingValues.single())
    }

    @Test
    fun `when observing widget loading state_given it was set to loading beforehand_then loading value is emitted`() = runTest(dispatcher) {
        backgroundScope.launch { notifier.notifyWordsSynchronizationForAction(widgetIdFixture) { suspendCoroutine { } } }

        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(widgetIdFixture))

        assertTrue(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_then loading value is emitted`() = runTest(dispatcher) {
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(widgetIdFixture).drop(1))

        backgroundScope.launch { notifier.notifyWordsSynchronizationForAction(widgetIdFixture) { suspendCoroutine { } } }

        assertTrue(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_given action has not finished_action is invoked after emitting loading`() = runTest(dispatcher) {
        var actionCalledAfterEmission = false
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(widgetIdFixture).drop(1))

        backgroundScope.launch {
            notifier.notifyWordsSynchronizationForAction(widgetIdFixture) {
                yield()
                actionCalledAfterEmission = isWidgetLoadingValues.isNotEmpty()
                suspendCoroutine { }
            }
        }

        assertTrue(actionCalledAfterEmission)
    }

    @Test
    fun `when widget is set to loading_given action has finished_then non-loading value is emitted`() = runTest(dispatcher) {
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(widgetIdFixture).drop(2))

        notifier.notifyWordsSynchronizationForAction(widgetIdFixture) { }

        assertFalse(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_given action has finished_then non-loading value is emitted after action finished`() = runTest(dispatcher) {
        var actionCalledBeforeEmission = false
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(widgetIdFixture).drop(2))

        notifier.notifyWordsSynchronizationForAction(widgetIdFixture) {
            yield()
            actionCalledBeforeEmission = isWidgetLoadingValues.isEmpty()
        }

        assertTrue(actionCalledBeforeEmission)
    }
}
