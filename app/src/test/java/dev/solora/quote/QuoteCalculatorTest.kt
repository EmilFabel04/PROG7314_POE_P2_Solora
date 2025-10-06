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
        calculator = QuoteCalculator()
    }

    @Test
    fun `test calculateQuote with valid usage input`() {
        // Given
        val inputs = QuoteInputs(
            address = "123 Test Street, Cape Town",
            usageKwh = 500.0,
            billRands = null,
            tariff = 2.5,
            panelWatt = 550,
            latitude = -33.9249,
            longitude = 18.4241,
            averageAnnualIrradiance = 5.5,
            averageAnnualSunHours = 6.0
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        assertNotNull(result)
        assertTrue(result.panels > 0)
        assertTrue(result.systemKwp > 0.0)
        assertTrue(result.inverterKw > 0.0)
        assertTrue(result.monthlySavings >= 0.0)
        assertTrue(result.estimatedGeneration > 0.0)
        assertTrue(result.paybackMonths > 0)
    }

    @Test
    fun `test calculateQuote with valid bill input`() {
        // Given
        val inputs = QuoteInputs(
            address = "456 Solar Ave, Johannesburg",
            usageKwh = null,
            billRands = 1250.0,
            tariff = 2.5,
            panelWatt = 550,
            latitude = -26.2041,
            longitude = 28.0473,
            averageAnnualIrradiance = 5.8,
            averageAnnualSunHours = 6.5
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        assertNotNull(result)
        assertTrue(result.panels > 0)
        assertTrue(result.systemKwp > 0.0)
        assertEquals(500.0, result.monthlyUsage, 0.1) // 1250 / 2.5 = 500 kWh
    }

    @Test
    fun `test calculateQuote system sizing logic`() {
        // Given
        val inputs = QuoteInputs(
            address = "789 Energy Road, Durban",
            usageKwh = 300.0,
            billRands = null,
            tariff = 2.0,
            panelWatt = 400,
            latitude = -29.8587,
            longitude = 31.0218,
            averageAnnualIrradiance = 5.3,
            averageAnnualSunHours = 5.8
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        // Verify system size is appropriate for usage
        val dailyUsage = 300.0 / 30.0 // 10 kWh per day
        val expectedSystemKw = dailyUsage / 5.8 // ~1.72 kW
        assertTrue(result.systemKwp >= expectedSystemKw * 0.8) // Allow 20% tolerance
        assertTrue(result.systemKwp <= expectedSystemKw * 1.3)
    }

    @Test
    fun `test calculateQuote panel count calculation`() {
        // Given
        val inputs = QuoteInputs(
            address = "Test Location",
            usageKwh = 600.0,
            billRands = null,
            tariff = 2.5,
            panelWatt = 550,
            latitude = -33.9249,
            longitude = 18.4241,
            averageAnnualIrradiance = 5.5,
            averageAnnualSunHours = 6.0
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        // Verify panel count matches system size
        val expectedPanels = (result.systemKwp * 1000) / 550
        assertEquals(expectedPanels.toInt(), result.panels, 1) // Allow 1 panel tolerance
    }

    @Test
    fun `test calculateQuote inverter sizing`() {
        // Given
        val inputs = QuoteInputs(
            address = "Test Location",
            usageKwh = 400.0,
            billRands = null,
            tariff = 2.5,
            panelWatt = 550,
            latitude = -33.9249,
            longitude = 18.4241,
            averageAnnualIrradiance = 5.5,
            averageAnnualSunHours = 6.0
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        // Inverter should be 80% of system capacity
        val expectedInverter = result.systemKwp * 0.8
        assertEquals(expectedInverter, result.inverterKw, 0.1)
    }

    @Test
    fun `test calculateQuote monthly savings calculation`() {
        // Given
        val inputs = QuoteInputs(
            address = "Test Location",
            usageKwh = 500.0,
            billRands = null,
            tariff = 3.0,
            panelWatt = 550,
            latitude = -33.9249,
            longitude = 18.4241,
            averageAnnualIrradiance = 5.5,
            averageAnnualSunHours = 6.0
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        // Monthly savings should be reasonable
        val monthlyBill = 500.0 * 3.0 // R1500
        assertTrue(result.monthlySavings > 0)
        assertTrue(result.monthlySavings <= monthlyBill * 1.2) // Can't save more than you pay + margin
    }

    @Test
    fun `test calculateQuote payback period is reasonable`() {
        // Given
        val inputs = QuoteInputs(
            address = "Test Location",
            usageKwh = 500.0,
            billRands = null,
            tariff = 2.5,
            panelWatt = 550,
            latitude = -33.9249,
            longitude = 18.4241,
            averageAnnualIrradiance = 5.5,
            averageAnnualSunHours = 6.0
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        // Payback period should be between 3-15 years (36-180 months)
        assertTrue(result.paybackMonths >= 36)
        assertTrue(result.paybackMonths <= 180)
    }

    @Test
    fun `test calculateQuote with minimum values`() {
        // Given
        val inputs = QuoteInputs(
            address = "Test Location",
            usageKwh = 100.0,
            billRands = null,
            tariff = 1.5,
            panelWatt = 300,
            latitude = -33.9249,
            longitude = 18.4241,
            averageAnnualIrradiance = 4.0,
            averageAnnualSunHours = 4.5
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        assertNotNull(result)
        assertTrue(result.panels > 0)
        assertTrue(result.systemKwp > 0.0)
    }

    @Test
    fun `test calculateQuote with maximum values`() {
        // Given
        val inputs = QuoteInputs(
            address = "Test Location",
            usageKwh = 2000.0,
            billRands = null,
            tariff = 4.0,
            panelWatt = 600,
            latitude = -33.9249,
            longitude = 18.4241,
            averageAnnualIrradiance = 7.0,
            averageAnnualSunHours = 8.0
        )

        // When
        val result = calculator.calculateQuote(inputs)

        // Then
        assertNotNull(result)
        assertTrue(result.panels > 0)
        assertTrue(result.systemKwp > 0.0)
        assertTrue(result.estimatedGeneration > 0.0)
    }

    @Test
    fun `test calculateQuote consistency`() {
        // Given - same inputs
        val inputs = QuoteInputs(
            address = "Test Location",
            usageKwh = 500.0,
            billRands = null,
            tariff = 2.5,
            panelWatt = 550,
            latitude = -33.9249,
            longitude = 18.4241,
            averageAnnualIrradiance = 5.5,
            averageAnnualSunHours = 6.0
        )

        // When - calculate twice
        val result1 = calculator.calculateQuote(inputs)
        val result2 = calculator.calculateQuote(inputs)

        // Then - results should be identical
        assertEquals(result1.panels, result2.panels)
        assertEquals(result1.systemKwp, result2.systemKwp, 0.001)
        assertEquals(result1.inverterKw, result2.inverterKw, 0.001)
        assertEquals(result1.monthlySavings, result2.monthlySavings, 0.001)
        assertEquals(result1.estimatedGeneration, result2.estimatedGeneration, 0.001)
        assertEquals(result1.paybackMonths, result2.paybackMonths)
    }
}

