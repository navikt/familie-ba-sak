package no.nav.familie.ba.sak.kjerne.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Brevmottaker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ValiderBrevmottakerServiceTest {
    private val brevmottakerService = mockk<BrevmottakerService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val familieIntegrasjonerTilgangskontrollService = mockk<FamilieIntegrasjonerTilgangskontrollService>()
    val validerBrevmottakerService = ValiderBrevmottakerService(
        brevmottakerService,
        persongrunnlagService,
        familieIntegrasjonerTilgangskontrollService,
    )

    private val behandlingId = 0L
    val brevmottaker = Brevmottaker(
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
    fun `Skal validere at er ok når ikke inneholder noen brevmottakere`() {
        every { brevmottakerService.hentBrevmottakere(behandlingId) } returns emptyList()

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
        )

        verify(exactly = 1) { brevmottakerService.hentBrevmottakere(behandlingId) }
        verify(exactly = 0) { persongrunnlagService.hentAktivThrows(any()) }
        verify(exactly = 0) {  familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) }
    }

    @Test
    fun `Skal kaste en exception når en behandling inneholder minst en strengt fortrolig person og minst en brevmottaker`() {
        every { brevmottakerService.hentBrevmottakere(behandlingId) } returns listOf(brevmottaker)
        every { persongrunnlagService.hentAktivThrows(behandlingId) } returns lagTestPersonopplysningGrunnlag(behandlingId, søker)
        every { familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns listOf(søker.aktør.aktivFødselsnummer())

        assertThatThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                behandlingId,
            )
        }.hasMessageContaining("strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere")
    }

    @Test
    fun `Skal validere at er ok når ikke inneholder noen strengt fortrolige personer og inneholder en brevmottaker`() {
        every { brevmottakerService.hentBrevmottakere(behandlingId) } returns listOf(brevmottaker)
        every { persongrunnlagService.hentAktivThrows(behandlingId) } returns lagTestPersonopplysningGrunnlag(behandlingId, søker)
        every { familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns emptyList()

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
        )
        verify(exactly = 1) { familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) }
    }

    @Test
    fun `Skal ikke kaste en exception når en behandling inneholder minst en strengt fortrolig person og ingen brevmottakere`() {
        every { brevmottakerService.hentBrevmottakere(behandlingId) } returns emptyList()
        every { persongrunnlagService.hentAktivThrows(behandlingId) } returns lagTestPersonopplysningGrunnlag(
            behandlingId,
            søker
        )
        every { familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any()) } returns listOf(
            søker.aktør.aktivFødselsnummer()
        )

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId,
        )
    }

}
