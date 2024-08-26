package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AutobrevUtilsTest {
    @Test
    fun `Skal sjekke at historiske og gjeldene begrunnelser blir hentet for 18 år`() {
        val begrunnelser = AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(alder = 18)

        assertEquals(listOf("REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK", "REDUKSJON_UNDER_18_ÅR"), begrunnelser.map { it.name })
    }

    @Test
    fun `Skal sjekke at gjeldende begrunnelse for autobrev er i listen over alle`() {
        assertThrows<Feil> {
            AutobrevUtils
                .hentStandardbegrunnelserReduksjonForAlder(6)
                .contains(AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(6))
        }.apply { assertEquals("Alder må være oppgitt til 18 år.", message) }

        assertTrue(
            AutobrevUtils
                .hentStandardbegrunnelserReduksjonForAlder(18)
                .contains(AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(18)),
        )
    }
}
