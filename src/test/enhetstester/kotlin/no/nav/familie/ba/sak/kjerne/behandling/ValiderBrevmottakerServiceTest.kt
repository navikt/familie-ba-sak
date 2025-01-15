package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ValiderBrevmottakerServiceTest {
    private val brevmottakerRepository = mockk<BrevmottakerRepository>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val familieIntegrasjonerTilgangskontrollService = mockk<FamilieIntegrasjonerTilgangskontrollService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    val validerBrevmottakerService =
        ValiderBrevmottakerService(
            brevmottakerRepository = brevmottakerRepository,
            persongrunnlagService = persongrunnlagService,
            familieIntegrasjonerTilgangskontrollService = familieIntegrasjonerTilgangskontrollService,
            fagsakRepository = fagsakRepository,
        )

    private val behandlingId = 0L
    val brevmottaker =
        BrevmottakerDb(
            behandlingId = behandlingId,
            type = MottakerType.DØDSBO,
            navn = "Donald Duck",
            adresselinje1 = "Andebyveien 1",
            postnummer = "0000",
            poststed = "OSLO",
            landkode = "NO",
        )
    val søker = tilfeldigPerson(personType = PersonType.SØKER)

    @Test
    fun `Skal ikke kaste funksjonell feil når en behandling ikke inneholder noen manuelle brevmottakere`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns emptyList()

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
            ekstraBarnLagtTilIBrev = emptyList(),
        )

        verify(exactly = 1) { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) }
        verify(exactly = 0) { persongrunnlagService.hentAktiv(any()) }
        verify(exactly = 0) {
            familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(
                any(),
            )
        }
    }

    @Test
    fun `Skal kaste en FunksjonellFeil exception når en behandling inneholder minst en strengt fortrolig person og minst en manuell brevmottaker`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns listOf(brevmottaker)
        every { persongrunnlagService.hentAktiv(behandlingId) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId,
                søker,
            )
        every { familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns
            listOf(
                søker.aktør.aktivFødselsnummer(),
            )

        assertThatThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                behandlingId,
                ekstraBarnLagtTilIBrev = emptyList(),
            )
        }.isInstanceOf(FunksjonellFeil::class.java).hasMessageContaining("strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere")
    }

    @Test
    fun `Skal ikke kaste funksjonell feil når behandling ikke inneholder noen strengt fortrolige personer og inneholder minst en manuell brevmottaker`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns listOf(brevmottaker)
        every { persongrunnlagService.hentAktiv(behandlingId) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId,
                søker,
            )
        every { familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns emptyList()

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
            ekstraBarnLagtTilIBrev = emptyList(),
        )
        verify(exactly = 1) {
            familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(
                any(),
            )
        }
    }

    @Test
    fun `Skal ikke kaste en exception når en behandling inneholder minst en strengt fortrolig person og ingen manuelle brevmottakere`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns emptyList()
        every { persongrunnlagService.hentAktiv(behandlingId) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId,
                søker,
            )
        every { familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns
            listOf(
                søker.aktør.aktivFødselsnummer(),
            )

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
            ekstraBarnLagtTilIBrev = emptyList(),
        )
    }

    @Test
    fun `Skal kaste en FunksjonellFeil exception når en behandling inneholder minst en strengt fortrolig person og det blir forsøkt lagt til en ny manuell brevmottaker`() {
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns emptyList()
        every { persongrunnlagService.hentAktiv(behandlingId) } returns
            lagTestPersonopplysningGrunnlag(
                behandlingId,
                søker,
            )
        every { familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns
            listOf(
                søker.aktør.aktivFødselsnummer(),
            )

        assertThatThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                behandlingId,
                brevmottaker,
                ekstraBarnLagtTilIBrev = emptyList(),
            )
        }.isInstanceOf(FunksjonellFeil::class.java).hasMessageContaining("strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere")
    }
}
