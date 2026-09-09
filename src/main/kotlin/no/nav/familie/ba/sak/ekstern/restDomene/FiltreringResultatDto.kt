package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.domene.FiltreringResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat

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
