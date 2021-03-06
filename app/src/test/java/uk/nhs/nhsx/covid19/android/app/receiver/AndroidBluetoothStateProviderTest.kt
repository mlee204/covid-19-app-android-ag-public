package uk.nhs.nhsx.covid19.android.app.receiver

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED

class AndroidBluetoothStateProviderTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testSubject = AndroidBluetoothStateProvider()

    private val context = mockk<Context>(relaxed = true)
    private val intent = mockk<Intent>(relaxed = true)

    private val availabilityState = mockk<Observer<AvailabilityState>>(relaxed = true)

    @Before
    fun setUp() {
        testSubject.availabilityState.observeForever(availabilityState)
    }

    @Test
    fun `start broadcasts state and registers receiver`() = runBlocking {
        testSubject.start(context)

        verify(exactly = 1) { availabilityState.onChanged(any()) }
        verify { context.registerReceiver(testSubject, any()) }
    }

    @Test
    fun `stop unregisters receiver with no side effects`() = runBlocking {
        testSubject.stop(context)

        verify(exactly = 0) { availabilityState.onChanged(any()) }
        verify { context.unregisterReceiver(testSubject) }
    }

    @Test
    fun `no value publishing if intent action is not ACTION_STATE_CHANGED`() = runBlocking {
        every { intent.action } returns BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { availabilityState.onChanged(any()) }
    }

    @Test
    fun `receiving broadcast triggers post value on observer with state on`() = runBlocking {
        every { intent.action } returns BluetoothAdapter.ACTION_STATE_CHANGED
        every { intent.getIntExtra(any(), any()) } returns BluetoothAdapter.STATE_ON

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { availabilityState.onChanged(ENABLED) }
    }

    @Test
    fun `receiving broadcast triggers post value on observer with state off`() = runBlocking {
        every { intent.action } returns BluetoothAdapter.ACTION_STATE_CHANGED
        every { intent.getIntExtra(any(), any()) } returns BluetoothAdapter.STATE_OFF

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { availabilityState.onChanged(DISABLED) }
    }
}
