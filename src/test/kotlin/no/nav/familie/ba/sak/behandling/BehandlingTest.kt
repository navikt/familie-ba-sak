package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.common.lagBehandling
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
}