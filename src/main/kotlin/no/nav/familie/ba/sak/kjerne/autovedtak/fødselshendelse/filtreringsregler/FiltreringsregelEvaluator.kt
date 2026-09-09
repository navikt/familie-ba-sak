package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.springframework.stereotype.Component

@Component
class FiltreringsregelEvaluator {
    fun <T : FiltreringsreglerFakta> evaluerFiltreringsregler(
        filtreringsregler: List<Filtreringsregel<T>>,
        fakta: T,
    ): List<Evaluering> =
        filtreringsregler.fold(mutableListOf()) { acc, filtreringsregel ->
            if (acc.any { it.resultat == Resultat.IKKE_OPPFYLT }) {
                acc.add(
                    Evaluering(
                        resultat = Resultat.IKKE_VURDERT,
                        identifikator = filtreringsregel.identifikator.name,
                        begrunnelse = "Ikke vurdert",
                        evalueringÅrsaker = emptyList(),
                    ),
                )
            } else {
                acc.add(filtreringsregel.evaluer(fakta).copy(identifikator = filtreringsregel.identifikator.name))
            }
            acc
        }
}
