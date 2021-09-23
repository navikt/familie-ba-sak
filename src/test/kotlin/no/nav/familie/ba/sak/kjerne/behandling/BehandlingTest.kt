package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingTest {

    @Test
    fun `erRentTekniskOpphør kastet feil hvis behandlingResultat og behandlingÅrsak ikke samsvarer ved teknisk opphør`() {
        val behandling = lagBehandling(
                behandlingType = BehandlingType.TEKNISK_OPPHØR,
                årsak = BehandlingÅrsak.SØKNAD)
        assertThrows<RuntimeException> { behandling.erTekniskOpphør() }
    }

    @Test
    fun `erRentTekniskOpphør gir true når teknisk opphør`() {
        val behandling = lagBehandling(
                behandlingType = BehandlingType.TEKNISK_OPPHØR,
                årsak = BehandlingÅrsak.TEKNISK_OPPHØR)
        assertTrue(behandling.erTekniskOpphør())
    }

    @Test
    fun `erRentTekniskOpphør gir false når ikke teknisk opphør`() {
        val behandling = lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.SØKNAD)
        assertFalse(behandling.erTekniskOpphør())
    }

    @Test
    fun `Skal velge ordinær ved FGB`() {
        assertEquals(BehandlingUnderkategori.ORDINÆR, Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                nyBehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                løpendeUnderkategori = null
        ))
    }

    @Test
    fun `Skal velge utvidet ved FGB`() {
        assertEquals(BehandlingUnderkategori.UTVIDET, Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                nyBehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                løpendeUnderkategori = null
        ))
    }

    @Test
    fun `Skal velge utvidet ved RV når FGB er utvidet`() {
        assertEquals(BehandlingUnderkategori.UTVIDET, Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                nyBehandlingType = BehandlingType.REVURDERING,
                løpendeUnderkategori = BehandlingUnderkategori.UTVIDET
        ))
    }

    @Test
    fun `Skal velge ordinær ved RV når FGB er ordinær`() {
        assertEquals(BehandlingUnderkategori.ORDINÆR, Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                nyBehandlingType = BehandlingType.REVURDERING,
                løpendeUnderkategori = BehandlingUnderkategori.ORDINÆR
        ))
    }

    @Test
    fun `Skal velge utvidet ved RV når FGB er ordinær`() {
        assertEquals(BehandlingUnderkategori.UTVIDET, Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                nyBehandlingType = BehandlingType.REVURDERING,
                løpendeUnderkategori = BehandlingUnderkategori.ORDINÆR
        ))
    }
}