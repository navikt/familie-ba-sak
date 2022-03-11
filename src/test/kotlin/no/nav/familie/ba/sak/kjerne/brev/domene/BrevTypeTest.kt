package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrevTypeTest {

    private val førerTilAvventerDokumentasjon = listOf(
        BrevType.INNHENTE_OPPLYSNINGER,
        BrevType.VARSEL_OM_REVURDERING,
        BrevType.VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14
    )

    private val førerIkkeTilAvventingAvDokumentasjon = BrevType.values().filter { it !in førerTilAvventerDokumentasjon }

    @Test
    fun `Skal si om behandling settes på vent`() {
        val setterIkkeBehandlingPåVent = BrevType.values().filter { !førerTilAvventerDokumentasjon.contains(it) }

        setterIkkeBehandlingPåVent.forEach {
            Assertions.assertFalse(it.setterBehandlingPåVent())
        }

        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertTrue(it.setterBehandlingPåVent())
        }

        førerIkkeTilAvventingAvDokumentasjon.forEach {
            Assertions.assertFalse(it.setterBehandlingPåVent())
        }
    }

    @Test
    fun `Skal gi riktig ventefrist`() {
        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertEquals(21L, it.ventefristDager())
        }

        førerIkkeTilAvventingAvDokumentasjon.forEach {
            assertThrows<Feil> { it.ventefristDager() }
        }
    }

    @Test
    fun `Skal gi riktig venteårsak`() {
        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertEquals(SettPåVentÅrsak.AVVENTER_DOKUMENTASJON, it.venteårsak())
        }

        førerIkkeTilAvventingAvDokumentasjon.forEach {
            Assertions.assertFalse(it.setterBehandlingPåVent())
        }
    }
}
