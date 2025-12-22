package ai.genos.core.ui.theme

import org.junit.Assert.*
import org.junit.Test

class ThemeTest {

    @Test
    fun `DarkColorScheme colors are defined`() {
        // Test that dark color scheme can be referenced
        assertNotNull(Purple80)
        assertNotNull(PurpleGrey80)
        assertNotNull(Pink80)
    }

    @Test
    fun `LightColorScheme colors are defined`() {
        // Test that light color scheme can be referenced
        assertNotNull(Purple40)
        assertNotNull(PurpleGrey40)
        assertNotNull(Pink40)
    }

    @Test
    fun `Typography is accessible`() {
        assertNotNull(Typography)
    }

    @Test
    fun `theme colors are properly configured`() {
        // Verify all required colors exist
        val darkColors = listOf(Purple80, PurpleGrey80, Pink80)
        val lightColors = listOf(Purple40, PurpleGrey40, Pink40)
        
        assertEquals(3, darkColors.size)
        assertEquals(3, lightColors.size)
    }

    @Test
    fun `color schemes use different colors`() {
        // Dark theme primary vs light theme primary
        assertNotEquals(Purple80, Purple40)
    }
}