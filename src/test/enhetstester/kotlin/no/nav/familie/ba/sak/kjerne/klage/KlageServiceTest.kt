package no.nav.familie.ba.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagKlagebehandlingDto
import no.nav.familie.ba.sak.datagenerator.randomAktør
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
import no.nav.familie.ba.sak.kjerne.klage.dto.OpprettKlageDto
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
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
    private val klagebehandlingOppretter = mockk<KlagebehandlingOppretter>()
    private val klageService =
        KlageService(
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            stegService = stegService,
            vedtakService = mockk(),
            tilbakekrevingKlient = mockk(),
            klagebehandlingHenter = klagebehandlingHenter,
            klagebehandlingOppretter = klagebehandlingOppretter,
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
            val result = klageService.validerOgOpprettRevurderingKlage(fagsakId = 0L, klagebehandlingId = UUID.randomUUID())

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
            val result = klageService.validerOgOpprettRevurderingKlage(fagsakId = 0L, klagebehandlingId = UUID.randomUUID())

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
    inner class OpprettKlage {
        @Test
        fun `skal opprette klagebehandling for fagsak`() {
            // Arrange
            val fagsak = lagFagsak()
            val dagensDato = LocalDate.now()
            val klagebehandlingId = UUID.randomUUID()

            every { klagebehandlingOppretter.opprettKlage(fagsak, dagensDato) } returns klagebehandlingId

            // Act
            val id = klageService.opprettKlage(fagsak, LocalDate.now())

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
        }

        @Test
        fun `skal opprette klagebehandling for fagsakId`() {
            // Arrange
            val fagsakId = 1L
            val klagebehandlingId = UUID.randomUUID()
            val opprettKlageDto = OpprettKlageDto(LocalDate.now())

            every { klagebehandlingOppretter.opprettKlage(fagsakId, opprettKlageDto) } returns klagebehandlingId

            // Act
            val id = klageService.opprettKlage(fagsakId, opprettKlageDto)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
        }
    }
}
