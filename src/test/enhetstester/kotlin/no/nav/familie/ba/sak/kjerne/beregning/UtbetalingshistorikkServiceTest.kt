package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class UtbetalingshistorikkServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val fagsakService = mockk<FagsakService>()
    private val infotrygdService = mockk<InfotrygdService>()
    private val service = UtbetalingshistorikkService(behandlingRepository, andelTilkjentYtelseRepository, fagsakService, infotrygdService)
    private val fagsak = lagFagsak()
    private val søkerIdent = fagsak.aktør.aktivFødselsnummer()

    @Test
    fun `skal returnere true når en ikke henlagt behandling har andeler`() {
        // Arrange
        val henlagtBehandling = lagBehandling(fagsak = fagsak, resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET)
        val behandling = lagBehandling(fagsak = fagsak)
        every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(henlagtBehandling, behandling)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id)) } returns
            listOf(lagAndelTilkjentYtelse(fom = YearMonth.of(2025, 1), tom = YearMonth.of(2025, 12), behandling = behandling))

        // Act
        val resultat = service.harSøkerHattUtbetaling(fagsak.id)

        // Assert
        assertThat(resultat).isTrue()
        verify(exactly = 0) { fagsakService.hentPåFagsakId(any()) }
        verify(exactly = 0) { infotrygdService.hentInfotrygdstønaderForSøker(any(), any()) }
    }

    @Test
    fun `skal bruke Infotrygd når ingen ikke henlagte behandlinger finnes`() {
        // Arrange
        val henlagtBehandling = lagBehandling(fagsak = fagsak, resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET)
        every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(henlagtBehandling)
        every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
        every { infotrygdService.hentInfotrygdstønaderForSøker(søkerIdent, historikk = true) } returns
            InfotrygdSøkResponse(bruker = listOf(Stønad()), barn = emptyList())

        // Act
        val resultat = service.harSøkerHattUtbetaling(fagsak.id)

        // Assert
        assertThat(resultat).isTrue()
        verify(exactly = 0) { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(any()) }
    }

    @Test
    fun `skal returnere false når verken andeler eller Infotrygdstønader finnes`() {
        // Arrange
        val behandling = lagBehandling(fagsak = fagsak)
        every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(behandling)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandling.id)) } returns emptyList()
        every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
        every { infotrygdService.hentInfotrygdstønaderForSøker(søkerIdent, historikk = true) } returns
            InfotrygdSøkResponse(bruker = emptyList(), barn = emptyList())

        // Act
        val resultat = service.harSøkerHattUtbetaling(fagsak.id)

        // Assert
        assertThat(resultat).isFalse()
        verify(exactly = 1) { infotrygdService.hentInfotrygdstønaderForSøker(søkerIdent, historikk = true) }
    }
}
