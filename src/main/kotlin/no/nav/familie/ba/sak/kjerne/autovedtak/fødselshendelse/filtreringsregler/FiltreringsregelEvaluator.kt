package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.springframework.stereotype.Component

@Component
class FiltreringsregelEvaluator {
    fun <T : FiltreringsreglerFakta> evaluerFiltreringsregler(
        filtreringsregler: List<Filtreringsregel<T>>,
        fakta: T,
    ): List<Evaluering> {
        var enRegelErIkkeOppfylt = false
        return filtreringsregler.map { filtreringsregel ->
            if (enRegelErIkkeOppfylt) {
                filtreringsregel.identifikator.tilIkkeVurdertEvaluering()
            } else {
                filtreringsregel.evaluer(fakta).also { enRegelErIkkeOppfylt = it.resultat == Resultat.IKKE_OPPFYLT }
            }
        }
    }
}
