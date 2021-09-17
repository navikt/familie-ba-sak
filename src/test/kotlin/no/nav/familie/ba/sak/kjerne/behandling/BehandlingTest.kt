package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.*
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.nyRevurdering
import no.nav.familie.ba.sak.common.nyUtvidetBehandling
import no.nav.familie.ba.sak.common.randomFnr
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
        val søkerFnr = randomFnr()
        assertEquals(BehandlingUnderkategori.ORDINÆR, Behandlingutils.bestemUnderkategori(
                nyBehandling = nyOrdinærBehandling(søkerFnr),
                aktivBehandlingUnderkategori = null
        ))
    }

    @Test
    fun `Skal velge utvidet ved FGB`() {
        val søkerFnr = randomFnr()
        assertEquals(BehandlingUnderkategori.UTVIDET, Behandlingutils.bestemUnderkategori(
                nyBehandling = nyUtvidetBehandling(søkerFnr),
                aktivBehandlingUnderkategori = null
        ))
    }

    @Test
    fun `Skal velge utvidet ved RV når FGB er utvidet`() {
        val søkerFnr = randomFnr()
        assertEquals(BehandlingUnderkategori.UTVIDET, Behandlingutils.bestemUnderkategori(
                nyBehandling = nyRevurdering(søkerFnr).copy(
                        behandlingÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL
                ),
                aktivBehandlingUnderkategori = BehandlingUnderkategori.UTVIDET
        ))
    }

    @Test
    fun `Skal velge ordinær ved RV når FGB er utvidet`() {
        val søkerFnr = randomFnr()
        assertEquals(BehandlingUnderkategori.ORDINÆR, Behandlingutils.bestemUnderkategori(
                nyBehandling = nyRevurdering(søkerFnr).copy(
                        behandlingÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL
                ),
                aktivBehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR
        ))
    }
}