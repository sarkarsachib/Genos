package ai.genos.core.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Test

class TypeTest {

    @Test
    fun `Typography object is not null`() {
        assertNotNull(Typography)
    }

    @Test
    fun `bodyLarge has correct font family`() {
        assertEquals(FontFamily.Default, Typography.bodyLarge.fontFamily)
    }

    @Test
    fun `bodyLarge has correct font weight`() {
        assertEquals(FontWeight.Normal, Typography.bodyLarge.fontWeight)
    }

    @Test
    fun `bodyLarge has correct font size`() {
        assertEquals(16.sp, Typography.bodyLarge.fontSize)
    }

    @Test
    fun `bodyLarge has correct line height`() {
        assertEquals(24.sp, Typography.bodyLarge.lineHeight)
    }

    @Test
    fun `bodyLarge has correct letter spacing`() {
        assertEquals(0.5.sp, Typography.bodyLarge.letterSpacing)
    }

    @Test
    fun `Typography has bodyLarge defined`() {
        assertNotNull(Typography.bodyLarge)
    }

    @Test
    fun `bodyLarge TextStyle is properly configured`() {
        val style = Typography.bodyLarge
        assertNotNull(style)
        assertNotNull(style.fontFamily)
        assertNotNull(style.fontWeight)
        assertNotNull(style.fontSize)
        assertNotNull(style.lineHeight)
        assertNotNull(style.letterSpacing)
    }

    @Test
    fun `Typography instance is reusable`() {
        val typography1 = Typography
        val typography2 = Typography
        assertEquals(typography1, typography2)
    }

    @Test
    fun `bodyLarge font size is positive`() {
        assertTrue(Typography.bodyLarge.fontSize.value > 0)
    }

    @Test
    fun `bodyLarge line height is greater than font size`() {
        assertTrue(Typography.bodyLarge.lineHeight > Typography.bodyLarge.fontSize)
    }

    @Test
    fun `bodyLarge letter spacing is non-negative`() {
        assertTrue(Typography.bodyLarge.letterSpacing.value >= 0)
    }
}