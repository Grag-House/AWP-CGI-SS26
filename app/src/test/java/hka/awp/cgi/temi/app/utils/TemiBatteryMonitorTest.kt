package hka.awp.cgi.temi.app.utils

import com.robotemi.sdk.BatteryData
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class TemiBatteryMonitorTest {
    @MockK(relaxed = true)
    lateinit var robot: Robot

    @MockK(relaxed = true)
    lateinit var batteryData: BatteryData

    @MockK(relaxed = true)
    lateinit var mqttManager: MqttManager

    lateinit var temiBatteryMonitor: TemiBatteryMonitor

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

        temiBatteryMonitor = TemiBatteryMonitor(robot, mqttManager)

        assertEquals(randomChargePercentage, temiBatteryMonitor.batteryLevel.value)
        assertEquals(randomChargeState, temiBatteryMonitor.isCharging.value)
    }

    @Test
    fun `Ensure battery state updates when listener is triggered`() {
        val randomChargePercentage = (0..100).random()
        val randomChargeState = Random.nextBoolean()

        every { robot.batteryData } returns null
        temiBatteryMonitor = TemiBatteryMonitor(robot, mqttManager)

        val incomingEventData = mockk<BatteryData>(relaxed = true)
        every { incomingEventData.level } returns randomChargePercentage
        every { incomingEventData.isCharging } returns randomChargeState

        temiBatteryMonitor.onBatteryStatusChanged(incomingEventData)

        assertEquals(randomChargePercentage, temiBatteryMonitor.batteryLevel.value)
        assertEquals(randomChargeState, temiBatteryMonitor.isCharging.value)
    }

    @Test
    fun `Ensure battery level is published to MQTT on battery update`() {
        every { robot.batteryData } returns null
        temiBatteryMonitor = TemiBatteryMonitor(robot, mqttManager)

        val incomingEventData = mockk<BatteryData>(relaxed = true)
        every { incomingEventData.level } returns 73
        every { incomingEventData.isCharging } returns false

        temiBatteryMonitor.onBatteryStatusChanged(incomingEventData)

        assertEquals(73, temiBatteryMonitor.batteryLevel.value)
        assertFalse(temiBatteryMonitor.isCharging.value)

        coVerify(exactly = 1, timeout = 1000L) {
            mqttManager.publishStatus(
                status = "73",
                topic = "innovation_lab/karlsruhe/temi/temi_battery_level"
            )
        }
    }
}
