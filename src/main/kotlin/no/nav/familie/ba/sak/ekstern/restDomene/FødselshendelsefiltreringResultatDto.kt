package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultat

data class FødselshendelsefiltreringResultatDto(
    val filtreringsregel: Filtreringsregel,
    val resultat: Resultat,
    val begrunnelse: String,
)

fun FødselshendelsefiltreringResultat.tilFødselshendelsefiltreringResultatDto() =
    FødselshendelsefiltreringResultatDto(
        filtreringsregel = this.filtreringsregel,
        resultat = this.resultat,
        begrunnelse = this.begrunnelse,
    )
