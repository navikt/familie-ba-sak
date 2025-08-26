package no.nav.familie.ba.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.klage.dto.OpprettKlageDto
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

class KlagebehandlingOppretterTest {
    private val dagensDato = LocalDate.of(2025, 8, 25)

    private val fagsakService = mockk<FagsakService>()
    private val klageClient = mockk<KlageClient>()
    private val integrasjonClient = mockk<IntegrasjonClient>()
    private val tilpassArbeidsfordelingService = mockk<TilpassArbeidsfordelingService>()
    private val clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(dagensDato)
    private val unleash = mockk<UnleashNextMedContextService>()

    private val klagebehandlingOppretter =
        KlagebehandlingOppretter(
            fagsakService,
            klageClient,
            integrasjonClient,
            tilpassArbeidsfordelingService,
            clockProvider,
            unleash,
        )

    @BeforeEach
    fun setup() {
        every { unleash.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_AA_FINNE_ENHET_FOR_OPPRETTING_AV_KLAGEBEHANDLING) } returns true
    }

    @Nested
    inner class OpprettKlage {
        @Test
        fun `skal kaste exception hvis klagebehandlingen blir mottatt etter dagens dato`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato.plusDays(1)

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)
                }
            assertThat(exception.message).isEqualTo("Kan ikke opprette klage med krav mottatt frem i tid.")
        }

        @Test
        fun `skal kaste exception om man ikke finner en behandlende enhet for aktør`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato

            every { integrasjonClient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns emptyList()

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)
                }
            assertThat(exception.message).isEqualTo("Fant ingen arbeidsfordelingsenhet for aktør.")
        }

        @Test
        fun `skal kaste exception om man ikke finner flere behandlende enheter for aktør`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato

            every { integrasjonClient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns
                listOf(
                    Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO),
                    Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.VADSØ),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)
                }
            assertThat(exception.message).isEqualTo("Fant flere arbeidsfordelingsenheter for aktør.")
        }

        @Test
        fun `skal opprette klage ved å sende inn kun fagsakId som parameter`() {
            // Arrange
            val fagsak = lagFagsak()
            val opprettKlageDto = OpprettKlageDto(dagensDato)
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { fagsakService.hentPåFagsakId(fagsak.id) } returns fagsak
            every { integrasjonClient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak.id, opprettKlageDto)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.BARNETRYGD)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.BA)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }

        @Test
        fun `skal lage OpprettKlageRequest uten å tilpasse enhetsnummeret fra fagsak aktør`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { integrasjonClient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.BARNETRYGD)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.BA)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }

        @Test
        fun `skal lage OpprettKlageRequest og tilpasse enhetsnummeret fra fagsak aktør basert på tilgangene til saksbehandler`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)
            val tilpassetArbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.VADSØ)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { integrasjonClient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns tilpassetArbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.BARNETRYGD)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.BA)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(tilpassetArbeidsfordelingsenhet.enhetId)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }

        @Test
        fun `skal lage OpprettKlageRequest hvor behandlendeEnhet er satt til saksbehandlers enhet når toggle er av`() {
            // Arrange
            val fagsak = lagFagsak()
            val klageMottattDato = dagensDato

            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { unleash.isEnabled(FeatureToggle.BRUK_NY_LOGIKK_FOR_AA_FINNE_ENHET_FOR_OPPRETTING_AV_KLAGEBEHANDLING) } returns false
            every { integrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(any()) } returns listOf(BarnetrygdEnhet.OSLO, BarnetrygdEnhet.VIKAFOSSEN)
            every { klageClient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.BARNETRYGD)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.BA)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(BarnetrygdEnhet.OSLO.enhetsnummer)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }
    }
}
