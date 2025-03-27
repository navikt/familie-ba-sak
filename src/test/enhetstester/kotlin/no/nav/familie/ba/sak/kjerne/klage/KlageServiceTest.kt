package no.nav.familie.ba.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagKlagebehandlingDto
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KlageServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val stegService = mockk<StegService>()
    private val klagebehandlingHenter = mockk<KlagebehandlingHenter>()
    private val klageService =
        KlageService(
            fagsakService = fagsakService,
            klageClient = mockk(),
            integrasjonClient = mockk(),
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            stegService = stegService,
            vedtakService = mockk(),
            tilbakekrevingKlient = mockk(),
            klagebehandlingHenter = klagebehandlingHenter,
        )

    @Nested
    inner class KanOppretteRevurdering {
        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns
                lagBehandling(
                    status = BehandlingStatus.AVSLUTTET,
                )

            val result = klageService.kanOppretteRevurdering(0L)

            Assertions.assertTrue(result.kanOpprettes)
            Assertions.assertEquals(result.årsak, null)
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det finnes åpen behandling`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns true
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns
                lagBehandling(
                    status = BehandlingStatus.UTREDES,
                )

            val result = klageService.kanOppretteRevurdering(0L)

            Assertions.assertFalse(result.kanOpprettes)
            Assertions.assertEquals(result.årsak, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING)
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det ikke finnes noen behandlinger`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            val result = klageService.kanOppretteRevurdering(0L)

            Assertions.assertFalse(result.kanOpprettes)
            Assertions.assertEquals(result.årsak, KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING)
        }
    }

    @Nested
    inner class OpprettRevurderingKlage {
        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            val aktør = randomAktør()
            val fagsak = Fagsak(aktør = aktør)
            val forrigeBehandling =
                lagBehandling(
                    behandlingKategori = BehandlingKategori.EØS,
                    underkategori = BehandlingUnderkategori.UTVIDET,
                    fagsak = fagsak,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    årsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG,
                    status = BehandlingStatus.AVSLUTTET,
                )

            every { fagsakService.hentPåFagsakId(any()) } returns fagsak
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns forrigeBehandling

            val nyBehandling =
                NyBehandling(
                    kategori = forrigeBehandling.kategori,
                    underkategori = forrigeBehandling.underkategori,
                    søkersIdent = forrigeBehandling.fagsak.aktør.aktivFødselsnummer(),
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.KLAGE,
                    navIdent = SikkerhetContext.hentSaksbehandler(),
                    barnasIdenter = emptyList(),
                    fagsakId = forrigeBehandling.fagsak.id,
                )

            klageService.validerOgOpprettRevurderingKlage(fagsak.id)

            verify { stegService.håndterNyBehandling(nyBehandling) }
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det finnes åpen behandling`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns true
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns
                lagBehandling(
                    status = BehandlingStatus.UTREDES,
                )

            val result = klageService.validerOgOpprettRevurderingKlage(0L)

            Assertions.assertFalse(result.opprettetBehandling)
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det ikke finnes noen behandlinger`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            val result = klageService.validerOgOpprettRevurderingKlage(0L)

            Assertions.assertFalse(result.opprettetBehandling)
        }
    }

    @Nested
    inner class HentKlagebehandlingerPåFagsak {
        @Test
        fun `skal hente alle klagebehandlinger på fagsak`() {
            // Arrange
            val fagsakId = 1L

            val klagebehandlinger =
                listOf(
                    lagKlagebehandlingDto(),
                    lagKlagebehandlingDto(),
                    lagKlagebehandlingDto(),
                )

            every { klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsakId) } returns klagebehandlinger

            // Act
            val resultat = klageService.hentKlagebehandlingerPåFagsak(fagsakId)

            // Assert
            assertThat(resultat).isEqualTo(klagebehandlinger)
        }
    }

    @Nested
    inner class HentForrigeVedtatteKlagebehandling {
        @Test
        fun `skal hente forrige vedtatte klagebehandling`() {
            // Arrange
            val behandling = lagBehandling()

            val klagebehandlingDto =
                lagKlagebehandlingDto(
                    vedtaksdato = LocalDateTime.now(),
                    status = no.nav.familie.kontrakter.felles.klage.BehandlingStatus.FERDIGSTILT,
                    henlagtÅrsak = null,
                    resultat = BehandlingResultat.MEDHOLD,
                )

            every { klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling) } returns klagebehandlingDto

            // Act
            val forrigeVedtatteKlagebehandling = klageService.hentForrigeVedtatteKlagebehandling(behandling)

            // Assert
            assertThat(forrigeVedtatteKlagebehandling).isEqualTo(klagebehandlingDto)
        }
    }
}
