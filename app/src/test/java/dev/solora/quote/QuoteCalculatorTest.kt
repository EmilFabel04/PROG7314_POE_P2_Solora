package dev.solora.quote

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for QuoteCalculator
 * Tests the solar quote calculation logic
 */
class QuoteCalculatorTest {

    private lateinit var calculator: QuoteCalculator

    @Before
    fun setup() {
        calculator = QuoteCalculator
    }

    @Test
    fun `test calculateBasic with valid usage input`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = 500.0,
            monthlyBillRands = null,
            tariffRPerKwh = 2.5,
            panelWatt = 550,
            sunHoursPerDay = 6.0,
            location = LocationInputs(
                latitude = -33.9249,
                longitude = 18.4241,
                address = "123 Test Street, Cape Town"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        assertNotNull(result)
        assertTrue(result.panels > 0)
        assertTrue(result.systemKw > 0.0)
        assertTrue(result.inverterKw > 0.0)
        assertTrue(result.estimatedMonthlySavingsR >= 0.0)
        assertTrue(result.estimatedMonthlyGeneration > 0.0)
        assertTrue(result.paybackMonths > 0)
    }

    @Test
    fun `test calculateBasic with valid bill input`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = null,
            monthlyBillRands = 1250.0,
            tariffRPerKwh = 2.5,
            panelWatt = 550,
            sunHoursPerDay = 6.5,
            location = LocationInputs(
                latitude = -26.2041,
                longitude = 28.0473,
                address = "456 Solar Ave, Johannesburg"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        assertNotNull(result)
        assertTrue(result.panels > 0)
        assertTrue(result.systemKw > 0.0)
        assertEquals(500.0, result.monthlyUsageKwh, 0.1) // 1250 / 2.5 = 500 kWh
    }

    @Test
    fun `test calculateBasic system sizing logic`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = 300.0,
            monthlyBillRands = null,
            tariffRPerKwh = 2.0,
            panelWatt = 400,
            sunHoursPerDay = 5.8,
            location = LocationInputs(
                latitude = -29.8587,
                longitude = 31.0218,
                address = "789 Energy Road, Durban"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        // Verify system size is appropriate for usage
        val dailyUsage = 300.0 / 30.0 // 10 kWh per day
        val expectedSystemKw = dailyUsage / 5.8 // ~1.72 kW
        assertTrue(result.systemKw >= expectedSystemKw * 0.8) // Allow 20% tolerance
        assertTrue(result.systemKw <= expectedSystemKw * 1.3)
    }

    @Test
    fun `test calculateBasic panel count calculation`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = 600.0,
            monthlyBillRands = null,
            tariffRPerKwh = 2.5,
            panelWatt = 550,
            sunHoursPerDay = 6.0,
            location = LocationInputs(
                latitude = -33.9249,
                longitude = 18.4241,
                address = "Test Location"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        // Verify panel count matches system size
        val expectedPanels = (result.systemKw * 1000) / 550
        assertEquals(expectedPanels.toInt(), result.panels, 1) // Allow 1 panel tolerance
    }

    @Test
    fun `test calculateBasic inverter sizing`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = 400.0,
            monthlyBillRands = null,
            tariffRPerKwh = 2.5,
            panelWatt = 550,
            sunHoursPerDay = 6.0,
            location = LocationInputs(
                latitude = -33.9249,
                longitude = 18.4241,
                address = "Test Location"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        // Inverter should be 80% of system capacity
        val expectedInverter = result.systemKw * 0.8
        assertEquals(expectedInverter, result.inverterKw, 0.1)
    }

    @Test
    fun `test calculateBasic monthly savings calculation`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = 500.0,
            monthlyBillRands = null,
            tariffRPerKwh = 3.0,
            panelWatt = 550,
            sunHoursPerDay = 6.0,
            location = LocationInputs(
                latitude = -33.9249,
                longitude = 18.4241,
                address = "Test Location"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        // Monthly savings should be reasonable
        val monthlyBill = 500.0 * 3.0 // R1500
        assertTrue(result.estimatedMonthlySavingsR > 0)
        assertTrue(result.estimatedMonthlySavingsR <= monthlyBill * 1.2) // Can't save more than you pay + margin
    }

    @Test
    fun `test calculateBasic payback period is reasonable`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = 500.0,
            monthlyBillRands = null,
            tariffRPerKwh = 2.5,
            panelWatt = 550,
            sunHoursPerDay = 6.0,
            location = LocationInputs(
                latitude = -33.9249,
                longitude = 18.4241,
                address = "Test Location"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        // Payback period should be between 3-15 years (36-180 months)
        assertTrue(result.paybackMonths >= 36)
        assertTrue(result.paybackMonths <= 180)
    }

    @Test
    fun `test calculateBasic with minimum values`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = 100.0,
            monthlyBillRands = null,
            tariffRPerKwh = 1.5,
            panelWatt = 300,
            sunHoursPerDay = 4.5,
            location = LocationInputs(
                latitude = -33.9249,
                longitude = 18.4241,
                address = "Test Location"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        assertNotNull(result)
        assertTrue(result.panels > 0)
        assertTrue(result.systemKw > 0.0)
    }

    @Test
    fun `test calculateBasic with maximum values`() {
        // Given
        val inputs = QuoteInputs(
            monthlyUsageKwh = 2000.0,
            monthlyBillRands = null,
            tariffRPerKwh = 4.0,
            panelWatt = 600,
            sunHoursPerDay = 8.0,
            location = LocationInputs(
                latitude = -33.9249,
                longitude = 18.4241,
                address = "Test Location"
            )
        )

        // When
        val result = calculator.calculateBasic(inputs)

        // Then
        assertNotNull(result)
        assertTrue(result.panels > 0)
        assertTrue(result.systemKw > 0.0)
        assertTrue(result.estimatedMonthlyGeneration > 0.0)
    }

    @Test
    fun `test calculateBasic consistency`() {
        // Given - same inputs
        val inputs = QuoteInputs(
            monthlyUsageKwh = 500.0,
            monthlyBillRands = null,
            tariffRPerKwh = 2.5,
            panelWatt = 550,
            sunHoursPerDay = 6.0,
            location = LocationInputs(
                latitude = -33.9249,
                longitude = 18.4241,
                address = "Test Location"
            )
        )

        // When - calculate twice
        val result1 = calculator.calculateBasic(inputs)
        val result2 = calculator.calculateBasic(inputs)

        // Then - results should be identical
        assertEquals(result1.panels, result2.panels)
        assertEquals(result1.systemKw, result2.systemKw, 0.001)
        assertEquals(result1.inverterKw, result2.inverterKw, 0.001)
        assertEquals(result1.estimatedMonthlySavingsR, result2.estimatedMonthlySavingsR, 0.001)
        assertEquals(result1.estimatedMonthlyGeneration, result2.estimatedMonthlyGeneration, 0.001)
        assertEquals(result1.paybackMonths, result2.paybackMonths)
    }
}