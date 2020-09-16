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
            PersonType.ANNENPART to "Annen Part"
    )

    init {
        Vilkår.values().map {
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

        LovligOppholdAvslagÅrsaker.values().map { årsak ->
            PersonType.values().filter { it !== PersonType.ANNENPART }.forEach { personType ->
                lovligOppholdAvslagÅrsaker[personType.name + årsak.name] = Metrics.counter("familie.ba.behandling.lovligopphold",
                                                                                           "aarsak",
                                                                                           årsak.besrivelse,
                                                                                           "personType",
                                                                                           personType.name,
                                                                                           "statsborger",
                                                                                           årsak.lovligOppholdStatsborger.beskrivelse)
            }
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

    companion object{
        val lovligOppholdAvslagÅrsaker = mutableMapOf<String, Counter>()

        fun økTellerForLovligOpphold(årsak: LovligOppholdAvslagÅrsaker, personType: PersonType) {
            lovligOppholdAvslagÅrsaker[personType.name + årsak.name]?.increment()
        }
    }
}

enum class LovligOppholdStatsborger(val beskrivelse: String) {
    TREDJELANDSBORGER("Tredjelandsborger"),
    EØS("EØS"),
    STATSLØS("Stateløs"),
}

enum class LovligOppholdAvslagÅrsaker(val lovligOppholdStatsborger: LovligOppholdStatsborger, val besrivelse: String) {
    EØS_IKKE_REGISTRERT_FAR_PÅ_BARNET(LovligOppholdStatsborger.EØS, "Ikke registrert far på barnet"),
    EØS_MOR_OG_FAR_IKKE_SAMME_BOSTEDSADRESSE(LovligOppholdStatsborger.EØS, "Mor og far ikke samme bostedsadresse"),
    EØS_FAR_HAR_IKKE_ET_LØPENDE_ARBEIDSFORHOLD_I_NORGE(LovligOppholdStatsborger.EØS,
                                                       "Far har ikke et løpende arbeidsforhold i Norge"),
    EØS_MOR_HAR_IKKE_HATT_ARBEIDSFORHOLD_I_DE_5_SISTE_ÅRENE(LovligOppholdStatsborger.EØS,
                                                            "Mor har ikke hatt arbeidsforhold i de 5 siste årene"),
    TREDJELANDSBORGER_FALLER_UT(LovligOppholdStatsborger.TREDJELANDSBORGER,
                                "Årsak til at søker faller ut på lovlig opphold vilkår – tredjelandsborger"),
    STATSLØS_FALLER_UT(LovligOppholdStatsborger.STATSLØS, "Årsak til at søker faller ut på lovlig opphold vilkår – statsløs"),
}