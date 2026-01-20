package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestSkjermetBarnSøker
import no.nav.familie.kontrakter.felles.Fødselsnummer

data class FagsakRequest(
    val personIdent: String,
    val fagsakType: FagsakType = FagsakType.NORMAL,
    val institusjon: InstitusjonDto? = null,
    val skjermetBarnSøker: RestSkjermetBarnSøker? = null,
) {
    // Bruker init til å validere personidenten
    init {
        Fødselsnummer(personIdent)
    }

    fun valider() {
        institusjon?.valider()
        skjermetBarnSøker?.valider()
        if (fagsakType == FagsakType.INSTITUSJON && institusjon == null) {
            throw FunksjonellFeil("Institusjon mangler for fagsaktype institusjon.")
        }
        if (fagsakType != FagsakType.INSTITUSJON && institusjon != null) {
            throw FunksjonellFeil("Forventer ikke at institusjon er satt for en annen fagsaktype enn institusjon.")
        }
        if (fagsakType == FagsakType.SKJERMET_BARN && skjermetBarnSøker == null) {
            throw FunksjonellFeil("Mangler informasjon om skjermet barn søker.")
        }
        if (fagsakType != FagsakType.SKJERMET_BARN && skjermetBarnSøker != null) {
            throw FunksjonellFeil("Forventer ikke at skjermet barn søker er satt for en annen fagsaktype enn skjermet.")
        }
    }
}
