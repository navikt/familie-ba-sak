package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

data class SøkParamDto(
    val personIdent: String,
    val barnasIdenter: List<String> = emptyList(),
) {
    fun valider() {
        try {
            Fødselsnummer(personIdent)
            barnasIdenter.forEach { Fødselsnummer(it) }
        } catch (e: Exception) {
            throw FunksjonellFeil("Ugyldig fødsels- eller d-nummer")
        }
    }
}

enum class FagsakDeltagerRolle {
    BARN,
    FORELDER,
    UKJENT,
}

data class FagsakDeltagerDto(
    val navn: String? = null,
    val ident: String = "",
    val rolle: FagsakDeltagerRolle,
    val fagsakType: FagsakType? = null,
    val kjønn: Kjønn = Kjønn.UKJENT,
    val fagsakId: Long? = null,
    val fagsakStatus: FagsakStatus? = null,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val harTilgang: Boolean = true,
    val erEgenAnsatt: Boolean? = null,
) {
    override fun toString(): String = "FagsakDeltagerDto(rolle=$rolle, kjønn=$kjønn, fagsakId=$fagsakId)"
}
