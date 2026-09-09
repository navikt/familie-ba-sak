package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.FiltreringResultat

data class FiltreringResultatDto(
    val filtreringsregel: Filtreringsregel.Identifikator,
    val resultat: Resultat,
    val begrunnelse: String,
)

fun FiltreringResultat.tilFiltreringResultatDto() =
    FiltreringResultatDto(
        filtreringsregel = this.filtreringsregel,
        resultat = this.resultat,
        begrunnelse = this.begrunnelse,
    )
