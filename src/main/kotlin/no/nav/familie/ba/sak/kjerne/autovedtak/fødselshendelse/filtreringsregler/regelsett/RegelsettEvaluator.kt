package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.regelsett

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFakta
import org.springframework.stereotype.Component

@Component
class RegelsettEvaluator {
    fun <T : FiltreringsreglerFakta> evaluerRegelsett(
        regelsett: List<Regelsett<T>>,
        fakta: T,
    ): List<Evaluering> =
        regelsett.fold(mutableListOf()) { acc, regelsett ->
            if (acc.any { it.resultat == Resultat.IKKE_OPPFYLT }) {
                acc.add(
                    Evaluering(
                        resultat = Resultat.IKKE_VURDERT,
                        identifikator = regelsett.regel.name,
                        begrunnelse = "Ikke vurdert",
                        evalueringÅrsaker = emptyList(),
                    ),
                )
            } else {
                acc.add(regelsett.evaluer(fakta).copy(identifikator = regelsett.regel.name))
            }
            acc
        }
}
