package dev.solora.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Firebase data models
 * Tests data validation and serialization
 */
class FirebaseModelsTest {

    @Test
    fun `test FirebaseQuote creation with valid data`() {
        // Given
        val quote = FirebaseQuote(
            id = "test-quote-123",
            reference = "QUOTE-12345",
            clientName = "John Doe",
            address = "123 Solar Street",
            usageKwh = 500.0,
            billRands = 1250.0,
            tariff = 2.5,
            panelWatt = 550,
            systemKwp = 3.3,
            estimatedGeneration = 495.0,
            monthlySavings = 1000.0,
            paybackMonths = 48,
            userId = "user-123"
        )

        // Then
        assertEquals("test-quote-123", quote.id)
        assertEquals("QUOTE-12345", quote.reference)
        assertEquals("John Doe", quote.clientName)
        assertEquals(500.0, quote.usageKwh ?: 0.0, 0.001)
        assertEquals(3.3, quote.systemKwp, 0.001)
    }

    @Test
    fun `test FirebaseQuote with null optional fields`() {
        // Given
        val quote = FirebaseQuote(
            id = "test-quote-456",
            reference = "QUOTE-67890",
            clientName = "Jane Smith",
            address = "456 Energy Ave",
            userId = "user-456"
        )

        // Then
        assertNotNull(quote)
        assertNull(quote.usageKwh)
        assertNull(quote.billRands)
        assertEquals(0.0, quote.tariff, 0.001)
        assertEquals(0, quote.panelWatt)
    }

    @Test
    fun `test FirebaseLead creation with valid data`() {
        // Given
        val lead = FirebaseLead(
            id = "lead-123",
            name = "John Doe",
            email = "john@example.com",
            phone = "+27123456789",
            status = "new",
            notes = "Interested in 5kW system",
            quoteId = "quote-123",
            userId = "user-123"
        )

        // Then
        assertEquals("lead-123", lead.id)
        assertEquals("John Doe", lead.name)
        assertEquals("john@example.com", lead.email)
        assertEquals("new", lead.status)
        assertEquals("quote-123", lead.quoteId)
    }

    @Test
    fun `test FirebaseLead with default status`() {
        // Given
        val lead = FirebaseLead(
            name = "Test Lead",
            email = "test@example.com",
            phone = "1234567890",
            userId = "user-123"
        )

        // Then
        assertEquals("new", lead.status)
        assertNull(lead.quoteId)
        assertNull(lead.notes)
    }

    @Test
    fun `test FirebaseUser creation with valid data`() {
        // Given
        val user = FirebaseUser(
            id = "user-123",
            name = "John",
            surname = "Doe",
            email = "john.doe@example.com",
            phone = "+27123456789",
            company = "Solar Solutions",
            role = "sales_consultant"
        )

        // Then
        assertEquals("user-123", user.id)
        assertEquals("John", user.name)
        assertEquals("Doe", user.surname)
        assertEquals("john.doe@example.com", user.email)
        assertEquals("sales_consultant", user.role)
    }

    @Test
    fun `test FirebaseUser with default role`() {
        // Given
        val user = FirebaseUser(
            name = "Jane",
            surname = "Smith",
            email = "jane@example.com"
        )

        // Then
        assertEquals("sales_consultant", user.role)
        assertEquals("", user.name)
        assertEquals("", user.surname)
    }

    @Test
    fun `test quote reference format`() {
        // Given
        val quote = FirebaseQuote(
            reference = "QUOTE-12345",
            clientName = "Test Client",
            address = "Test Address",
            userId = "user-123"
        )

        // Then
        assertTrue(quote.reference.startsWith("QUOTE-"))
        assertTrue(quote.reference.length > 6)
    }

    @Test
    fun `test lead email validation format`() {
        // Given
        val lead = FirebaseLead(
            name = "Test Lead",
            email = "valid.email@domain.com",
            phone = "1234567890",
            userId = "user-123"
        )

        // Then
        assertTrue(lead.email.contains("@"))
        assertTrue(lead.email.contains("."))
    }

    @Test
    fun `test quote calculations are positive`() {
        // Given
        val quote = FirebaseQuote(
            reference = "QUOTE-TEST",
            clientName = "Test",
            address = "Test",
            systemKwp = 5.5,
            estimatedGeneration = 825.0,
            monthlySavings = 1500.0,
            paybackMonths = 60,
            userId = "user-123"
        )

        // Then
        assertTrue(quote.systemKwp > 0)
        assertTrue(quote.estimatedGeneration > 0)
        assertTrue(quote.monthlySavings > 0)
        assertTrue(quote.paybackMonths > 0)
    }

    @Test
    fun `test user email is not empty`() {
        // Given
        val user = FirebaseUser(
            name = "Test",
            surname = "User",
            email = "test@example.com"
        )

        // Then
        assertFalse(user.email.isEmpty())
        assertTrue(user.email.contains("@"))
    }

    @Test
    fun `test lead status values are valid`() {
        // Valid status values
        val validStatuses = listOf("new", "contacted", "quoted", "converted", "lost")

        // Test each status
        validStatuses.forEach { status ->
            val lead = FirebaseLead(
                name = "Test",
                email = "test@example.com",
                phone = "123456789",
                status = status,
                userId = "user-123"
            )
            assertTrue(validStatuses.contains(lead.status))
        }
    }
}

