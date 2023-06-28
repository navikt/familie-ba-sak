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
        val brevmottakere = brevmottakerService.hentBrevmottakere(behandlingId).takeIf { it.isNotEmpty() } ?: return
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)
        val personIdentList = personopplysningGrunnlag.søkerOgBarn
            .takeIf { it.isNotEmpty() }
            ?.map { it.aktør.aktivFødselsnummer() }
            ?: return
        val strengtFortroligePersoner =
            familieIntegrasjonerTilgangskontrollService.returnerPersonerMedAdressebeskyttelse(personIdentList)
        if (strengtFortroligePersoner.isNotEmpty()) {
            val melding = "Behandlingen (id: $behandlingId) inneholder ${strengtFortroligePersoner.size} person(er) med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere (${brevmottakere.size} stk)."
            val frontendFeilmelding =
                "Behandlingen inneholder personer med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            throw FunksjonellFeil(melding, frontendFeilmelding)
        }
    }
}
