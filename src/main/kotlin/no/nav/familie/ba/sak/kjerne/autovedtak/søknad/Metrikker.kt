package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.FILTRERINGSREGLER_SØKNAD
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.springframework.stereotype.Component
import kotlin.collections.forEach

@Component
class Metrikker {
    private val filtreringsreglerMetrics = mutableMapOf<String, Counter>()
    private val filtreringsreglerFørsteUtfallMetrics = mutableMapOf<String, Counter>()

    init {
        FILTRERINGSREGLER_SØKNAD.forEach { regel ->
            Resultat.entries.forEach { resultat ->
                filtreringsreglerMetrics["${regel.identifikator.name}_${resultat.name}"] =
                    Metrics.counter(
                        "familie.ba.sak.filtreringsregler.soknad.utfall",
                        "beskrivelse",
                        regel.identifikator.name,
                        "resultat",
                        resultat.name,
                    )
            }

            filtreringsreglerFørsteUtfallMetrics[regel.identifikator.name] =
                Metrics.counter(
                    "familie.ba.sak.filtreringsregler.soknad.foersteutfall",
                    "beskrivelse",
                    regel.identifikator.name,
                )
        }
    }

    fun oppdaterMetrikker(evalueringer: List<Evaluering>) {
        var førsteutfall = true
        evalueringer.forEach {
            filtreringsreglerMetrics["${it.identifikator}_${it.resultat.name}"]!!.increment()
            førsteutfall = økTellereForFørsteUtfall(it, førsteutfall)
        }
    }

    private fun økTellereForFørsteUtfall(
        evaluering: Evaluering,
        førsteutfall: Boolean,
    ): Boolean {
        if (evaluering.resultat == Resultat.IKKE_OPPFYLT && førsteutfall) {
            filtreringsreglerFørsteUtfallMetrics[evaluering.identifikator]!!.increment()
            return false
        }
        return førsteutfall
    }
}
