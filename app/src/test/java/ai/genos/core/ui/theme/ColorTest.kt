package ai.genos.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class ColorTest {

    @Test
    fun `Purple80 has correct color value`() {
        assertEquals(Color(0xFFD0BCFF), Purple80)
    }

    @Test
    fun `PurpleGrey80 has correct color value`() {
        assertEquals(Color(0xFFCCC2DC), PurpleGrey80)
    }

    @Test
    fun `Pink80 has correct color value`() {
        assertEquals(Color(0xFFEFB8C8), Pink80)
    }

    @Test
    fun `Purple40 has correct color value`() {
        assertEquals(Color(0xFF6650a4), Purple40)
    }

    @Test
    fun `PurpleGrey40 has correct color value`() {
        assertEquals(Color(0xFF625b71), PurpleGrey40)
    }

    @Test
    fun `Pink40 has correct color value`() {
        assertEquals(Color(0xFF7D5260), Pink40)
    }

    @Test
    fun `all dark theme colors are defined`() {
        assertNotNull(Purple80)
        assertNotNull(PurpleGrey80)
        assertNotNull(Pink80)
    }

    @Test
    fun `all light theme colors are defined`() {
        assertNotNull(Purple40)
        assertNotNull(PurpleGrey40)
        assertNotNull(Pink40)
    }

    @Test
    fun `Purple80 is different from Purple40`() {
        assertNotEquals(Purple80, Purple40)
    }

    @Test
    fun `PurpleGrey80 is different from PurpleGrey40`() {
        assertNotEquals(PurpleGrey80, PurpleGrey40)
    }

    @Test
    fun `Pink80 is different from Pink40`() {
        assertNotEquals(Pink80, Pink40)
    }

    @Test
    fun `dark theme colors are distinct`() {
        assertNotEquals(Purple80, PurpleGrey80)
        assertNotEquals(Purple80, Pink80)
        assertNotEquals(PurpleGrey80, Pink80)
    }

    @Test
    fun `light theme colors are distinct`() {
        assertNotEquals(Purple40, PurpleGrey40)
        assertNotEquals(Purple40, Pink40)
        assertNotEquals(PurpleGrey40, Pink40)
    }

    @Test
    fun `Purple80 alpha channel is opaque`() {
        assertEquals(1f, Purple80.alpha, 0.01f)
    }

    @Test
    fun `all colors have opaque alpha`() {
        assertEquals(1f, Purple80.alpha, 0.01f)
        assertEquals(1f, PurpleGrey80.alpha, 0.01f)
        assertEquals(1f, Pink80.alpha, 0.01f)
        assertEquals(1f, Purple40.alpha, 0.01f)
        assertEquals(1f, PurpleGrey40.alpha, 0.01f)
        assertEquals(1f, Pink40.alpha, 0.01f)
    }

    @Test
    fun `color values are immutable`() {
        val color1 = Purple80
        val color2 = Purple80
        assertEquals(color1, color2)
    }
}