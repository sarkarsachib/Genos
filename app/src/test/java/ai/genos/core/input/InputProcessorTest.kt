package ai.genos.core.input

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class InputProcessorTest {

    private lateinit var inputProcessor: InputProcessor

    @Before
    fun setUp() {
        inputProcessor = InputProcessor()
    }

    @Test
    fun `constructor creates instance successfully`() {
        assertNotNull(inputProcessor)
    }

    @Test
    fun `processInput returns input text unchanged`() {
        val input = "test input"
        val result = inputProcessor.processInput(input)
        assertEquals(input, result)
    }

    @Test
    fun `processInput handles empty string`() {
        val result = inputProcessor.processInput("")
        assertEquals("", result)
    }

    @Test
    fun `processInput handles whitespace`() {
        val input = "   "
        val result = inputProcessor.processInput(input)
        assertEquals(input, result)
    }

    @Test
    fun `processInput handles long text`() {
        val input = "a".repeat(10000)
        val result = inputProcessor.processInput(input)
        assertEquals(input, result)
    }

    @Test
    fun `processInput handles special characters`() {
        val input = "!@#$%^&*()_+-=[]{}|;':,.<>?/~`"
        val result = inputProcessor.processInput(input)
        assertEquals(input, result)
    }

    @Test
    fun `processInput handles unicode characters`() {
        val input = "Hello 世界 🌍 مرحبا"
        val result = inputProcessor.processInput(input)
        assertEquals(input, result)
    }

    @Test
    fun `processInput handles newlines and tabs`() {
        val input = "line1\nline2\tindented"
        val result = inputProcessor.processInput(input)
        assertEquals(input, result)
    }

    @Test
    fun `processInput handles null-like strings`() {
        val input = "null"
        val result = inputProcessor.processInput(input)
        assertEquals("null", result)
    }

    @Test
    fun `handleVoiceInput does not throw exception with empty array`() {
        try {
            inputProcessor.handleVoiceInput(ByteArray(0))
            assertTrue(true)
        } catch (e: Exception) {
            fail("handleVoiceInput should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `handleVoiceInput does not throw exception with small array`() {
        try {
            val audioData = ByteArray(100) { it.toByte() }
            inputProcessor.handleVoiceInput(audioData)
            assertTrue(true)
        } catch (e: Exception) {
            fail("handleVoiceInput should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `handleVoiceInput does not throw exception with large array`() {
        try {
            val audioData = ByteArray(100000) { (it % 256).toByte() }
            inputProcessor.handleVoiceInput(audioData)
            assertTrue(true)
        } catch (e: Exception) {
            fail("handleVoiceInput should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `handleVoiceInput can be called multiple times`() {
        try {
            val audioData = ByteArray(10) { it.toByte() }
            inputProcessor.handleVoiceInput(audioData)
            inputProcessor.handleVoiceInput(audioData)
            inputProcessor.handleVoiceInput(audioData)
            assertTrue(true)
        } catch (e: Exception) {
            fail("Multiple handleVoiceInput calls should not throw: ${e.message}")
        }
    }

    @Test
    fun `processInput and handleVoiceInput can be used together`() {
        val text = inputProcessor.processInput("test")
        assertEquals("test", text)
        
        val audioData = ByteArray(10)
        inputProcessor.handleVoiceInput(audioData)
        
        val text2 = inputProcessor.processInput("test2")
        assertEquals("test2", text2)
    }

    @Test
    fun `multiple processInput calls are independent`() {
        val result1 = inputProcessor.processInput("first")
        val result2 = inputProcessor.processInput("second")
        val result3 = inputProcessor.processInput("third")
        
        assertEquals("first", result1)
        assertEquals("second", result2)
        assertEquals("third", result3)
    }

    @Test
    fun `processInput handles numeric strings`() {
        val input = "1234567890"
        val result = inputProcessor.processInput(input)
        assertEquals(input, result)
    }

    @Test
    fun `processInput handles mixed content`() {
        val input = "Mixed 123 !@# content"
        val result = inputProcessor.processInput(input)
        assertEquals(input, result)
    }
}