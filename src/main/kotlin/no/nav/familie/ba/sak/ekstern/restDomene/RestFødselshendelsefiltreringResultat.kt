package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.domene.FødselshendelsefiltreringResultat

data class RestFødselshendelsefiltreringResultat(
        val filtreringsregel: Filtreringsregel,
        val resultat: Resultat,
        val begrunnelse: String,
)


fun FødselshendelsefiltreringResultat.tilRestFødselshendelsefiltreringResultat() = RestFødselshendelsefiltreringResultat(
        filtreringsregel = this.filtreringsregel,
        resultat = this.resultat,
        begrunnelse = this.begrunnelse
)