package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BrevTypeTest {

    private val førerTilAvventerDokumentasjon = listOf(BrevType.INNHENTE_OPPLYSNINGER, BrevType.VARSEL_OM_REVURDERING)

    @Test
    fun `Skal si om behandling settes på vent`() {
        val setterIkkeBehandlingPåVent = BrevType.values().filter { !førerTilAvventerDokumentasjon.contains(it) }

        setterIkkeBehandlingPåVent.forEach {
            Assertions.assertFalse(it.setterBehandlingPåVent())
        }

        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertTrue(it.setterBehandlingPåVent())
        }
    }

    @Test
    fun `Skal gi riktig ventefrist`() {
        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertEquals(21L, it.ventefristDager())
        }
    }

    @Test
    fun `Skal gi riktig venteårsak`() {
        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertEquals(SettPåVentÅrsak.AVVENTER_DOKUMENTASJON, it.venteårsak())
        }
    }
}
