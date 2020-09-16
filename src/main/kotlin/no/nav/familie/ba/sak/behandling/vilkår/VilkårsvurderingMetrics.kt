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

    lateinit var vilkårsvurderingUtfall: Map<String, Counter>
    lateinit var vilkårsvurderingFørstUtfall: Map<String, Counter>

    val personTypeToDisplayedType = mapOf(
            PersonType.SØKER to "Mor",
            PersonType.BARN to "Barn",
            PersonType.ANNENPART to "Medforelder"
    )

    init {
        Vilkår.values().forEach {
            Resultat.values().forEach { resultat ->
                BehandlingOpprinnelse.values().forEach { behandlingOpprinnelse ->
                    PersonType.values().forEach { personType ->
                        if (it.parterDetteGjelderFor.contains(personType)) {
                            vilkårsvurderingUtfall = genererMetrikkMap(it.spesifikasjon,
                                                                       personType,
                                                                       resultat,
                                                                       behandlingOpprinnelse,
                                                                       "familie.ba.behandling.vilkaarsvurdering")
                            vilkårsvurderingFørstUtfall = genererMetrikkMap(it.spesifikasjon,
                                                                            personType,
                                                                            resultat,
                                                                            behandlingOpprinnelse,
                                                                            "familie.ba.behandling.vilkaarsvurdering.foerstutfall")
                        }
                    }
                }
            }
        }

        LovligOppholdUtfall.values().forEach { utfall ->
            lovligOppholdUtfall[utfall.name] = Metrics.counter("familie.ba.behandling.lovligopphold",
                    "aarsak",
                    utfall.begrunnelseForMetrikker)
        }
    }

    private fun genererMetrikkMap(spesifikasjon: Spesifikasjon<Fakta>,
                                  personType: PersonType,
                                  resultat: Resultat,
                                  behandlingOpprinnelse: BehandlingOpprinnelse,
                                  navn: String): MutableMap<String, Counter> {
        val counterMap = mutableMapOf<String, Counter>()
        if (spesifikasjon.children.isEmpty()) {
            val counter = Metrics.counter(navn,
                                          "vilkaar",
                                          spesifikasjon.identifikator,
                                          "personType",
                                          personTypeToDisplayedType[personType],
                                          "opprinnelse",
                                          behandlingOpprinnelse.name,
                                          "resultat",
                                          resultat.name,
                                          "beskrivelse",
                                          spesifikasjon.beskrivelse)

            counterMap[vilkårNøkkel(spesifikasjon.identifikator, personType, resultat, behandlingOpprinnelse)] =
                    counter
        } else {
            spesifikasjon.children.forEach {
                counterMap.putAll(genererMetrikkMap(it,
                                                    personType,
                                                    resultat,
                                                    behandlingOpprinnelse, navn))
            }
        }

        return counterMap
    }

    fun vilkårNøkkel(vilkår: String,
                     personType: PersonType,
                     resultat: Resultat,
                     behandlingOpprinnelse: BehandlingOpprinnelse) = "${vilkår}-${personType.name}_${resultat.name}_${behandlingOpprinnelse.name}"

    fun økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(vilkår: Vilkår, personType: PersonType) {
        vilkårsvurderingFørstUtfall[vilkårNøkkel(vilkår.spesifikasjon.identifikator,
                                                 personType, Resultat.NEI,
                                                 BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)]?.increment()
    }

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
        val lovligOppholdUtfall = mutableMapOf<String, Counter>()

        fun økTellerForLovligOpphold(utfall: LovligOppholdUtfall) {
            lovligOppholdUtfall[utfall.name]?.increment()
        }
    }
}