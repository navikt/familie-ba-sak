package no.nav.familie.ba.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagKlagebehandlingDto
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.EnhetConfig
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.domene.NyEksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.enhet.Enhet
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlageServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val stegService = mockk<StegService>()
    private val klagebehandlingHenter = mockk<KlagebehandlingHenter>()
    private val klageClient = mockk<KlageClient>()
    private val mocketEnhetConfig = mockk<EnhetConfig>()
    private val klageService =
        KlageService(
            fagsakService = fagsakService,
            klageClient = klageClient,
            enhetConfig = mocketEnhetConfig,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            stegService = stegService,
            vedtakService = mockk(),
            tilbakekrevingKlient = mockk(),
            klagebehandlingHenter = klagebehandlingHenter,
        )

    @Nested
    inner class KanOppretteRevurdering {
        @Test
        fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            // Arrange
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns
                lagBehandling(
                    status = BehandlingStatus.AVSLUTTET,
                )

            // Act
            val result = klageService.kanOppretteRevurdering(0L)

            // Assert
            assertThat(result.kanOpprettes).isTrue()
            assertThat(result.årsak).isNull()
        }

        @Test
        fun `kan ikke opprette revurdering hvis det finnes åpen behandling`() {
            // Arrange
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns true
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns
                lagBehandling(
                    status = BehandlingStatus.UTREDES,
                )

            // Act
            val result = klageService.kanOppretteRevurdering(0L)

            // Assert
            assertThat(result.kanOpprettes).isFalse()
            assertThat(result.årsak).isEqualTo(KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING)
        }

        @Test
        fun `kan ikke opprette revurdering hvis det ikke finnes noen behandlinger`() {
            // Arrange
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            // Act
            val result = klageService.kanOppretteRevurdering(0L)

            // Assert
            assertThat(result.kanOpprettes).isFalse()
            assertThat(result.årsak).isEqualTo(KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING)
        }
    }

    @Nested
    inner class OpprettRevurderingKlage {
        @Test
        fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            // Arrange
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
                    nyEksternBehandlingRelasjon = null,
                )

            // Act
            klageService.validerOgOpprettRevurderingKlage(fagsakId = fagsak.id, klagebehandlingId = null)

            // Assert
            verify { stegService.håndterNyBehandling(nyBehandling) }
        }

        @Test
        fun `kan opprette revurdering med ekstern behandling relasjon hvis det finnes en ferdigstilt behandling`() {
            // Arrange
            val klagebehandlingId = UUID.randomUUID()
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

            val nyEksternBehandlingRelasjon =
                NyEksternBehandlingRelasjon(
                    eksternBehandlingId = klagebehandlingId.toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

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
                    nyEksternBehandlingRelasjon = nyEksternBehandlingRelasjon,
                )

            every { fagsakService.hentPåFagsakId(any()) } returns fagsak
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns forrigeBehandling

            // Act
            klageService.validerOgOpprettRevurderingKlage(fagsakId = fagsak.id, klagebehandlingId = klagebehandlingId)

            // Assert
            verify { stegService.håndterNyBehandling(nyBehandling) }
        }

        @Test
        fun `kan ikke opprette revurdering hvis det finnes åpen behandling`() {
            // Arrange
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns true
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling(status = BehandlingStatus.UTREDES)

            // Act
            val result = klageService.validerOgOpprettRevurderingKlage(fagsakId = 0L, klagebehandlingId = null)

            // Assert
            assertThat(result.opprettetBehandling).isFalse()
        }

        @Test
        fun `kan ikke opprette revurdering hvis det ikke finnes noen behandlinger`() {
            // Arrange
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            // Act
            val result = klageService.validerOgOpprettRevurderingKlage(fagsakId = 0L, klagebehandlingId = null)

            // Assert
            assertThat(result.opprettetBehandling).isFalse()
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

    @Nested
    inner class SettRiktigEnhetVedOpprettelseAvKlage {
        @Test
        fun `skal sette enheten til saksbehandlers enhet ved opprettelse av klage`() {
            // Arrange
            val fagsak = lagFagsak()
            val forventetEnhet = Enhet("1234", "en")

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns UUID.randomUUID()

            // Act
            klageService.opprettKlage(fagsak, LocalDate.now())

            // Assert
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(forventetEnhet.enhetsnummer)
        }
    }
}
