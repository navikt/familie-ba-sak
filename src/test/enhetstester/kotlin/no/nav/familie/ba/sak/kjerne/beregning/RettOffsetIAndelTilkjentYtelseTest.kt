package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RettOffsetIAndelTilkjentYtelseTest {

    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()

    private val beregningService: BeregningService = mockk()

    @Test
    fun `Skal filtrere bort behandling hvis det eksisterer nyere behandling som er avsluttet`() {
        val fagsak1 = Fagsak(aktør = randomAktør(), id = 1)
        val fagsak2 = Fagsak(aktør = randomAktør(), id = 2)
        val fagsak3 = Fagsak(aktør = randomAktør(), id = 3)

        val behandling1fagsak1 = lagBehandling(fagsak = fagsak1, status = BehandlingStatus.AVSLUTTET)

        val behandling1fagsak2 = lagBehandling(fagsak = fagsak2, status = BehandlingStatus.AVSLUTTET)
        val behandling2fagsak2 = lagBehandling(fagsak = fagsak2, status = BehandlingStatus.AVSLUTTET)

        val behandling1fagsak3 = lagBehandling(fagsak = fagsak3, status = BehandlingStatus.AVSLUTTET)
        val behandling2fagsak3 = lagBehandling(fagsak = fagsak3, status = BehandlingStatus.UTREDES)

        val behandlingerMedFeilOffset = listOf(behandling1fagsak1, behandling1fagsak2, behandling1fagsak3)

        every { behandlingHentOgPersisterService.hentBehandlinger(fagsak1.id)} returns listOf(behandling1fagsak1)
        every { behandlingHentOgPersisterService.hentBehandlinger(fagsak2.id)} returns listOf(behandling1fagsak2, behandling2fagsak2)
        every { behandlingHentOgPersisterService.hentBehandlinger(fagsak3.id)} returns listOf(behandling1fagsak3, behandling2fagsak3)

        val relevanteBehandlinger = RettOffsetIAndelTilkjentYtelseTask(behandlingHentOgPersisterService, beregningService).finnRelevanteBehandlingerForOppdateringAvOffset(behandlingerMedFeilOffset)

        Assertions.assertEquals(2, relevanteBehandlinger.size)
        Assertions.assertEquals(setOf(behandling1fagsak1, behandling1fagsak3), relevanteBehandlinger.toSet())
    }
}