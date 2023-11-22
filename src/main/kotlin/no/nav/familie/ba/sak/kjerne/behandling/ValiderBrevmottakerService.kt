package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import org.springframework.stereotype.Service

@Service
class ValiderBrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val familieIntegrasjonerTilgangskontrollService: FamilieIntegrasjonerTilgangskontrollService,
    private val fagsakRepository: FagsakRepository,
) {
    fun validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
        behandlingId: Long,
        nyBrevmottaker: BrevmottakerDb? = null,
        barnLagtTilIBrev: List<String>,
    ) {
        var brevmottakere = brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId)
        nyBrevmottaker?.let {
            brevmottakere += it
        }
        brevmottakere.takeIf { it.isNotEmpty() } ?: return

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandlingId) ?: return
        val personIdenter =
            personopplysningGrunnlag.søkerOgBarn
                .takeIf { it.isNotEmpty() }
                ?.map { it.aktør.aktivFødselsnummer() }
                ?: return
        val strengtFortroligePersonIdenter =
            familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(
                (personIdenter + barnLagtTilIBrev).toSet().toList(),
            )
        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding = "Behandlingen (id: $behandlingId) inneholder ${strengtFortroligePersonIdenter.size} person(er) med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere (${brevmottakere.size} stk)."
            val frontendFeilmelding =
                "Behandlingen inneholder personer med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            throw FunksjonellFeil(melding, frontendFeilmelding)
        }
    }

    fun validerAtFagsakIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
        fagsakId: Long,
        manuelleBrevmottakere: List<ManuellBrevmottaker>,
        barnLagtTilIBrev: List<String>,
    ) {
        val erManuellBrevmottaker = manuelleBrevmottakere.isNotEmpty()
        if (!erManuellBrevmottaker) return

        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw Feil("Fant ikke fagsak $fagsakId")
        val strengtFortroligePersonIdenter =
            familieIntegrasjonerTilgangskontrollService.hentIdenterMedStrengtFortroligAdressebeskyttelse(listOf(fagsak.aktør.aktivFødselsnummer()) + barnLagtTilIBrev)

        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding =
                "Brev på fagsak $fagsakId inneholder person med strengt fortrolig adressebeskyttelse og kan ikke sendes til manuelle brevmottakere (${manuelleBrevmottakere.size} stk)."
            val frontendFeilmelding =
                "Brevet inneholder personer med strengt fortrolig adressebeskyttelse og kan ikke sendes til manuelle brevmottakere."
            throw FunksjonellFeil(melding, frontendFeilmelding)
        }
    }
}
