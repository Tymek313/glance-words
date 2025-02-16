package com.pt.domain.words.synchronization

import com.pt.domain.words.fixture.randomWidgetId
import com.pt.glancewords.domain.synchronization.DefaultWordsSynchronizationStateNotifier
import com.pt.testcommon.coroutines.collectToListInBackground
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
        backgroundScope.launch { notifier.notifyWordsSynchronizationForAction(WIDGET_ID) { suspendCoroutine { } } }

        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(WIDGET_ID))

        assertTrue(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_then loading value is emitted`() = runTest(dispatcher) {
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(WIDGET_ID).drop(1))

        backgroundScope.launch { notifier.notifyWordsSynchronizationForAction(WIDGET_ID) { suspendCoroutine { } } }

        assertTrue(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_given action has not finished_action is invoked after emitting loading`() = runTest(dispatcher) {
        var actionCalledAfterEmission = false
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(WIDGET_ID).drop(1))

        backgroundScope.launch {
            notifier.notifyWordsSynchronizationForAction(WIDGET_ID) {
                yield()
                actionCalledAfterEmission = isWidgetLoadingValues.isNotEmpty()
                suspendCoroutine { }
            }
        }

        assertTrue(actionCalledAfterEmission)
    }

    @Test
    fun `when widget is set to loading_given action has finished_then non-loading value is emitted`() = runTest(dispatcher) {
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(WIDGET_ID).drop(2))

        notifier.notifyWordsSynchronizationForAction(WIDGET_ID) { }

        assertFalse(isWidgetLoadingValues.single())
    }

    @Test
    fun `when widget is set to loading_given action has finished_then non-loading value is emitted after action finished`() = runTest(dispatcher) {
        var actionCalledBeforeEmission = false
        val isWidgetLoadingValues = collectToListInBackground(notifier.observeAreWordsSynchronized(WIDGET_ID).drop(2))

        notifier.notifyWordsSynchronizationForAction(WIDGET_ID) {
            yield()
            actionCalledBeforeEmission = isWidgetLoadingValues.isEmpty()
        }

        assertTrue(actionCalledBeforeEmission)
    }

    private companion object {
        val WIDGET_ID = randomWidgetId()
    }
}
