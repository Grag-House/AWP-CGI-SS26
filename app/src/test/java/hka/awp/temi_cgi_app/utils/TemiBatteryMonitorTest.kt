package hka.awp.temi_cgi_app.utils

import com.robotemi.sdk.BatteryData
import com.robotemi.sdk.Robot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class TemiBatteryMonitorTest {
    @MockK(relaxed = true)
    lateinit var robot: Robot

    @MockK(relaxed = true)
    lateinit var batteryData: BatteryData

    lateinit var temiBatteryMonitorTest: TemiBatteryMonitor

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `Ensure that the battery state is read correctly from the Temi SDK`() {
        val randomChargePercentage = (0..100).random()
        val randomChargeState = Random.nextBoolean()

        every { robot.batteryData } returns batteryData
        every { batteryData.level } returns randomChargePercentage
        every { batteryData.isCharging } returns randomChargeState

        temiBatteryMonitorTest = TemiBatteryMonitor(robot)

        assertEquals(randomChargePercentage, temiBatteryMonitorTest.batteryLevel.value)
        assertEquals(randomChargeState, temiBatteryMonitorTest.isCharging.value)
    }

    @Test
    fun `Ensure battery state updates when listener is triggered`() {
        val randomChargePercentage = (0..100).random()
        val randomChargeState = Random.nextBoolean()

        every { robot.batteryData } returns null
        temiBatteryMonitorTest = TemiBatteryMonitor(robot)

        val incomingEventData = mockk<BatteryData>(relaxed = true)
        every { incomingEventData.level } returns randomChargePercentage
        every { incomingEventData.isCharging } returns randomChargeState

        temiBatteryMonitorTest.onBatteryStatusChanged(incomingEventData)

        assertEquals(randomChargePercentage, temiBatteryMonitorTest.batteryLevel.value)
        assertEquals(randomChargeState, temiBatteryMonitorTest.isCharging.value)
    }
}