package no.nav.familie.ba.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagInstitusjon
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.klage.dto.OpprettKlageDto
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøker
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.UUID

class KlagebehandlingOppretterTest {
    private val dagensDato = LocalDate.of(2025, 8, 25)

    private val fagsakService = mockk<FagsakService>()
    private val klageKlient = mockk<KlageKlient>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val tilpassArbeidsfordelingService = mockk<TilpassArbeidsfordelingService>()
    private val clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(dagensDato)
    private val featureToggleService = mockk<FeatureToggleService>()

    private val klagebehandlingOppretter =
        KlagebehandlingOppretter(
            fagsakService,
            klageKlient,
            integrasjonKlient,
            tilpassArbeidsfordelingService,
            clockProvider,
            featureToggleService,
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(FeatureToggle.SKAL_KUNNE_BEHANDLE_BA_INSTITUSJONSFAGSAKER_I_KLAGE) } returns true
    }

    @Nested
    inner class OpprettKlage {
        @Test
        fun `skal kaste exception hvis man prøver å opprette en klagebehandling for en institusjonsfagsak om toggelen er skrudd av`() {
            // Arrange
            val fagsak = lagFagsak(type = FagsakType.INSTITUSJON)
            val klageMottattDato = dagensDato.plusDays(1)

            every { featureToggleService.isEnabled(FeatureToggle.SKAL_KUNNE_BEHANDLE_BA_INSTITUSJONSFAGSAKER_I_KLAGE) } returns false

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)
                }
            assertThat(exception.message).isEqualTo("Oppretting av klagebehandlinger for institusjonsfagsaker er ikke implementert.")
        }

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

            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns emptyList()

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

            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns
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
            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageKlient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak.id, opprettKlageDto)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.orgNummer).isNull()
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

            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageKlient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.orgNummer).isNull()
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

            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageKlient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns tilpassetArbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.orgNummer).isNull()
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.BARNETRYGD)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.BA)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(tilpassetArbeidsfordelingsenhet.enhetId)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }

        @Test
        fun `skal lage OpprettKlageRequest for institusjonsfagsak om toggle er skrudd på`() {
            // Arrange
            val institusjon = lagInstitusjon(orgNummer = "123456789")
            val fagsak = lagFagsak(type = FagsakType.INSTITUSJON, institusjon = institusjon)
            val klageMottattDato = dagensDato
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageKlient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.orgNummer).isEqualTo(institusjon.orgNummer)
            assertThat(opprettKlageRequest.captured.stønadstype).isEqualTo(Stønadstype.BARNETRYGD)
            assertThat(opprettKlageRequest.captured.eksternFagsakId).isEqualTo(fagsak.id.toString())
            assertThat(opprettKlageRequest.captured.fagsystem).isEqualTo(Fagsystem.BA)
            assertThat(opprettKlageRequest.captured.behandlendeEnhet).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(opprettKlageRequest.captured.behandlingsårsak).isEqualTo(Klagebehandlingsårsak.ORDINÆR)
        }

        @Test
        fun `skal kaste feil om man prøver å opprette en klagebehandling for fagsak av type institusjon men som mangler insitutsjon`() {
            // Arrange
            val fagsak = lagFagsak(type = FagsakType.INSTITUSJON, institusjon = null)
            val klageMottattDato = dagensDato
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageKlient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act & assert
            val exception = assertThrows<Feil> { klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato) }
            assertThat(exception.message).isEqualTo("Fant ikke forventet institusjon på fagsak ${fagsak.id}.")
        }

        @ParameterizedTest
        @EnumSource(value = FagsakType::class, names = ["SKJERMET_BARN"], mode = EnumSource.Mode.EXCLUDE)
        fun `skal sende fagsakAktør sin ident som søkerIdent i fagsak uten type SKJERMET_BARN`(
            fagsakType: FagsakType,
        ) {
            // Arrange
            val fagsak = lagFagsak(type = fagsakType, institusjon = lagInstitusjon().takeIf { fagsakType == FagsakType.INSTITUSJON })
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_SKJERMET_BARN_KLAGE) } returns true
            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageKlient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, dagensDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.søkerIdent).isEqualTo(fagsak.aktør.aktivFødselsnummer())
        }

        @Test
        fun `skal sende søkerIdent fra skjermetBarnSøker for SKJERMET_BARN-fagsak`() {
            // Arrange
            val søkerAktør = lagAktør()
            val skjermetBarnSøker = SkjermetBarnSøker(aktør = søkerAktør)
            val fagsak = lagFagsak(type = FagsakType.SKJERMET_BARN, skjermetBarnSøker = skjermetBarnSøker)
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_SKJERMET_BARN_KLAGE) } returns true
            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageKlient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            val id = klagebehandlingOppretter.opprettKlage(fagsak, dagensDato)

            // Assert
            assertThat(id).isEqualTo(klagebehandlingId)
            assertThat(opprettKlageRequest.captured.ident).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            assertThat(opprettKlageRequest.captured.søkerIdent).isEqualTo(søkerAktør.aktivFødselsnummer())
        }

        @Test
        fun `skal kaste feil for SKJERMET_BARN-fagsak uten skjermetBarnSøker`() {
            // Arrange
            val fagsak = lagFagsak(type = FagsakType.SKJERMET_BARN, skjermetBarnSøker = null)
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)

            every { featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_SKJERMET_BARN_KLAGE) } returns true
            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)

            // Act & assert
            val exception = assertThrows<Feil> { klagebehandlingOppretter.opprettKlage(fagsak, dagensDato) }
            assertThat(exception.message).isEqualTo("Fant ikke forventet søker på fagsak ${fagsak.id}.")
        }

        @Test
        fun `skal ikke sende søkerIdent fra skjermetBarnSøker for SKJERMET_BARN-fagsak når toggle er av`() {
            // Arrange
            val søkerAktør = lagAktør()
            val skjermetBarnSøker = SkjermetBarnSøker(aktør = søkerAktør)
            val fagsak = lagFagsak(type = FagsakType.SKJERMET_BARN, skjermetBarnSøker = skjermetBarnSøker)
            val arbeidsfordelingsenhet = Arbeidsfordelingsenhet.opprettFra(BarnetrygdEnhet.OSLO)
            val klagebehandlingId = UUID.randomUUID()

            val opprettKlageRequest = slot<OpprettKlagebehandlingRequest>()

            every { featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_SKJERMET_BARN_KLAGE) } returns false
            every { integrasjonKlient.hentBehandlendeEnhet(fagsak.aktør.aktivFødselsnummer()) } returns listOf(arbeidsfordelingsenhet)
            every { klageKlient.opprettKlage(capture(opprettKlageRequest)) } returns klagebehandlingId
            every { tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(arbeidsfordelingsenhet, any()) } returns arbeidsfordelingsenhet

            // Act
            klagebehandlingOppretter.opprettKlage(fagsak, dagensDato)

            // Assert - søkerIdent skal falle tilbake til barnets ident (fagsak.aktør)
            assertThat(opprettKlageRequest.captured.søkerIdent).isEqualTo(fagsak.aktør.aktivFødselsnummer())
        }
    }
}
