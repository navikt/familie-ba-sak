package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.regex.Pattern

class FooCodeServiceITest : AbstractSpringIntegrationTest() {
    private val fooCodeService = FooCodeService()

    @Test
    fun `generateRandomCode should return code with correct prefix and length`() {
        val prefix = "TEST"
        val length = 12
        val code = fooCodeService.generateRandomCode(prefix, length)

        Assertions.assertEquals(length, code.length)
        Assertions.assertTrue(code.startsWith(prefix))

        // Verify the random part contains only allowed characters
        val randomPart = code.substring(prefix.length)
        val allowedPattern = Pattern.compile("[A-Z0-9]+")
        Assertions.assertTrue(allowedPattern.matcher(randomPart).matches())
    }

    @Test
    fun `generateRandomCode should use default values when not specified`() {
        val code = fooCodeService.generateRandomCode()

        Assertions.assertEquals(10, code.length)
        Assertions.assertTrue(code.startsWith("DEMO"))
    }

    @Test
    fun `generateRandomCode should throw exception when length is less than or equal to prefix length`() {
        val prefix = "TEST"

        val exception =
            assertThrows<IllegalArgumentException> {
                fooCodeService.generateRandomCode(prefix, prefix.length)
            }

        Assertions.assertEquals("Total length must be greater than prefix length", exception.message)
    }

    @Test
    fun `generateUuidCode should return valid UUID with prefix`() {
        val prefix = "PRE-"
        val code = fooCodeService.generateUuidCode(prefix)

        Assertions.assertTrue(code.startsWith(prefix))

        // Extract the UUID part and verify it's a valid UUID
        val uuidPart = code.substring(prefix.length)
        Assertions.assertDoesNotThrow { UUID.fromString(uuidPart) }
    }

    @Test
    fun `generateUuidCode should return valid UUID without prefix when not specified`() {
        val code = fooCodeService.generateUuidCode()

        // Verify it's a valid UUID
        Assertions.assertDoesNotThrow { UUID.fromString(code) }
    }

    @Test
    fun `generateNumericCode should return numeric code with correct length`() {
        val length = 8
        val code = fooCodeService.generateNumericCode(length)

        Assertions.assertEquals(length, code.length)

        // Verify the code contains only digits
        val numericPattern = Pattern.compile("\\d+")
        Assertions.assertTrue(numericPattern.matcher(code).matches())
    }

    @Test
    fun `generateNumericCode should use default length when not specified`() {
        val code = fooCodeService.generateNumericCode()

        Assertions.assertEquals(6, code.length)

        // Verify the code contains only digits
        val numericPattern = Pattern.compile("\\d+")
        Assertions.assertTrue(numericPattern.matcher(code).matches())
    }

    @Test
    fun `generateNumericCode should throw exception when length is not positive`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                fooCodeService.generateNumericCode(0)
            }

        Assertions.assertEquals("Length must be positive", exception.message)
    }
}
