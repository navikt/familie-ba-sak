package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.harSøkerÅpneBehandlinger
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class UtfiltrereÅpnebehandlingerUtilTest {


    val opprettetBehandling = lagBehandling().copy(status = BehandlingStatus.OPPRETTET)
    val avsluttetBehandling = lagBehandling().copy(status = BehandlingStatus.AVSLUTTET)


    @Test
    fun `Mor har en åpen og en avsluttet behandling`() {
        Assertions.assertTrue(harSøkerÅpneBehandlinger(listOf(opprettetBehandling, avsluttetBehandling)))
    }

    @Test
    fun `Mor har en avsluttet og ingen åpne behandling`() {
        Assertions.assertFalse(harSøkerÅpneBehandlinger(listOf(avsluttetBehandling)))
    }

    @Test
    fun `Mor har to åpne behandlinger`() {
        Assertions.assertTrue(harSøkerÅpneBehandlinger(listOf(opprettetBehandling, opprettetBehandling)))
    }

    @Test
    fun `Mor har to avsluttede og ingen åpne behandlinger`() {
        Assertions.assertFalse(harSøkerÅpneBehandlinger(listOf(avsluttetBehandling, avsluttetBehandling)))
    }
}