package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.springframework.stereotype.Component

@Component
class FiltreringsregelEvaluator(
    private val featureToggleService: FeatureToggleService,
) {
    fun <T : FiltreringsreglerFakta> evaluerFiltreringsregler(
        filtreringsregler: List<Filtreringsregel<T>>,
        fakta: T,
    ): List<Evaluering> {
        val skalVurdereAlleFiltreringsregler = featureToggleService.isEnabled(FeatureToggle.VURDER_ALLE_FILTRERINGSREGLER)

        return filtreringsregler.fold(mutableListOf()) { acc, filtreringsregel ->
            val finnesResultatSomIkkeErOppfylt = acc.any { it.resultat == Resultat.IKKE_OPPFYLT }
            if (finnesResultatSomIkkeErOppfylt && !skalVurdereAlleFiltreringsregler) {
                acc.add(
                    Evaluering(
                        resultat = Resultat.IKKE_VURDERT,
                        identifikator = filtreringsregel.identifikator.name,
                        begrunnelse = "Ikke vurdert",
                        evalueringÅrsaker = emptyList(),
                    ),
                )
            } else {
                acc.add(filtreringsregel.evaluer(fakta))
            }
            acc
        }
    }
}
