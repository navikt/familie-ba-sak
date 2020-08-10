package no.nav.familie.ba.sak.behandling.vilkår

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.springframework.stereotype.Component

@Component
class VilkårsvurderingMetrics {

    val vilkårsvurderingUtfall = mutableMapOf<String, Counter>()

    init {
        Vilkår.values().map {
            Resultat.values().forEach { resultat ->
                BehandlingOpprinnelse.values().forEach { behandlingOpprinnelse ->
                    PersonType.values().forEach { personType ->
                        if (it.parterDetteGjelderFor.contains(personType)) {
                            genererMetrikkMap(it.spesifikasjon, personType, resultat, behandlingOpprinnelse)
                        }
                    }
                }
            }
        }

        LovligOppholdAvslagÅrsaker.values().map {årsak ->
            PersonType.values().filter { it !== PersonType.ANNENPART }.forEach { personType ->
                        lovligOppholdAvslagÅrsaker[personType.name + årsak.name] = Metrics.counter("familie.ba.behandling.lovligopphold",
                                                                              "aarsak",
                                                                              årsak.name,
                                                                              "personType",
                                                                              personType.name)
                    }
        }
    }

    private fun genererMetrikkMap(spesifikasjon: Spesifikasjon<Fakta>,
                                  personType: PersonType,
                                  resultat: Resultat,
                                  behandlingOpprinnelse: BehandlingOpprinnelse) {
        if (spesifikasjon.children.isEmpty()) {
            val counter = Metrics.counter("familie.ba.behandling.vilkaarsvurdering",
                                          "vilkaar",
                                          spesifikasjon.identifikator,
                                          "personType",
                                          personType.name,
                                          "opprinnelse",
                                          behandlingOpprinnelse.name,
                                          "resultat",
                                          resultat.name,
                                          "beskrivelse",
                                          spesifikasjon.beskrivelse)

            vilkårsvurderingUtfall[vilkårNøkkel(spesifikasjon.identifikator, personType, resultat, behandlingOpprinnelse)] =
                    counter
        } else {
            spesifikasjon.children.forEach { genererMetrikkMap(it, personType, resultat, behandlingOpprinnelse) }
        }
    }

    fun vilkårNøkkel(vilkår: String,
                     personType: PersonType,
                     resultat: Resultat,
                     behandlingOpprinnelse: BehandlingOpprinnelse) = "${vilkår}-${personType.name}_${resultat.name}_${behandlingOpprinnelse.name}"

    fun økTellereForEvaluering(evaluering: Evaluering, personType: PersonType, behandlingOpprinnelse: BehandlingOpprinnelse) {
        if (evaluering.children.isEmpty()) {
            vilkårsvurderingUtfall[vilkårNøkkel(evaluering.identifikator,
                                                personType,
                                                evaluering.resultat,
                                                behandlingOpprinnelse)]?.increment()
        } else {
            evaluering.children.forEach { økTellereForEvaluering(it, personType, behandlingOpprinnelse) }
        }
    }

    companion object {
        val lovligOppholdAvslagÅrsaker = mutableMapOf<String, Counter>()

        fun økTellerForLovligOpphold(årsak: LovligOppholdAvslagÅrsaker, personType: PersonType) {
            lovligOppholdAvslagÅrsaker[personType.name + årsak.name]?.increment()
        }
    }
}

enum class LovligOppholdAvslagÅrsaker {
    TREDJELANDSBORGER,
    EØS,
    STATSLØS,
    BOSTEDSADRESSE
}