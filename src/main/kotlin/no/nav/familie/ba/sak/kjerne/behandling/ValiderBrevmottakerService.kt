package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.springframework.stereotype.Service

@Service
class ValiderBrevmottakerService(
    private val brevmottakerService: BrevmottakerService,
    private val persongrunnlagService: PersongrunnlagService,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
) {
    fun validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(behandlingId: Long) {
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId)
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandlingId)
        val personer = personopplysningGrunnlag?.søkerOgBarn
        val personIdentList = personer?.map { it.aktør.aktivFødselsnummer() } ?: emptyList()
        val strengtFortroligAdresseBeskyttelseIdentList = if (personIdentList.isNotEmpty()) { familieIntegrasjonerTilgangskontrollService.returnerPersonerMedAdressebeskyttelse(personIdentList) } else { emptyList() }
        if (brevmottakere.isNotEmpty() && strengtFortroligAdresseBeskyttelseIdentList.isNotEmpty()) {
            val kommaSeparertListeAvStrengtFortroligIdenter = strengtFortroligAdresseBeskyttelseIdentList.joinToString { it }
            val melding = "Behandlingen (id: $behandlingId) inneholder ${strengtFortroligAdresseBeskyttelseIdentList.size} person(er) med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere (${brevmottakere.size} stk)."
            val frontendFeilmelding = "Behandlingen inneholder personer med strengt fortrolig adressebeskyttelse ($kommaSeparertListeAvStrengtFortroligIdenter) og kan ikke kombineres med manuelle brevmottakere."
            throw FunksjonellFeil(melding, frontendFeilmelding)
        }
    }
}
