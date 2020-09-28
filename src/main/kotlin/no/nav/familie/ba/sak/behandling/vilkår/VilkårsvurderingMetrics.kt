package no.nav.familie.ba.sak.behandling.vilkår

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class VilkårsvurderingMetrics {

    val vilkårsvurderingUtfall = mutableMapOf<String, Counter>()

    val personTypeToDisplayedType = mapOf(
            PersonType.SØKER to "Mor",
            PersonType.BARN to "Barn",
            PersonType.ANNENPART to "Medforelder"
    )

    enum class VilkårstellerType(val navn: String) {
        UTFALL("familie.ba.behandling.vilkaarsvurdering"),
        FØRSTEUTFALL("familie.ba.behandling.vilkaarsvurdering.foerstutfall")
    }

    init {
        VilkårstellerType.values().forEach { tellerType ->
            Vilkår.values().forEach {
                Resultat.values().forEach { resultat ->
                    BehandlingOpprinnelse.values().forEach { behandlingOpprinnelse ->
                        PersonType.values().forEach { personType ->
                            if (it.parterDetteGjelderFor.contains(personType)) {
                                leggTilEntryTilMetrikkMap(tellerType,
                                                          it.spesifikasjon,
                                                          personType,
                                                          resultat,
                                                          behandlingOpprinnelse)
                            }
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

    private fun leggTilEntryTilMetrikkMap(tellerType: VilkårstellerType,
                                          spesifikasjon: Spesifikasjon<FaktaTilVilkårsvurdering>,
                                          personType: PersonType,
                                          resultat: Resultat,
                                          behandlingOpprinnelse: BehandlingOpprinnelse){
        if (spesifikasjon.children.isEmpty()) {
            val counter = Metrics.counter(tellerType.navn,
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
            vilkårsvurderingUtfall[vilkårNøkkel(tellerType,
                                                spesifikasjon.identifikator,
                                                personType,
                                                resultat,
                                                behandlingOpprinnelse)] =
                    counter
        } else {
            spesifikasjon.children.forEach {
                leggTilEntryTilMetrikkMap(tellerType,
                                          it,
                                          personType,
                                          resultat,
                                          behandlingOpprinnelse)
            }
        }
    }

    fun vilkårNøkkel(
            tellerType: VilkårstellerType,
            vilkår: String,
            personType: PersonType,
            resultat: Resultat,
            behandlingOpprinnelse: BehandlingOpprinnelse) = "${tellerType}-${vilkår}-${personType.name}_${resultat.name}_${behandlingOpprinnelse.name}"

    fun økTellereForEvaluering(evaluering: Evaluering, personType: PersonType, behandlingOpprinnelse: BehandlingOpprinnelse) {
        økTellereForEvaluering(VilkårstellerType.UTFALL, evaluering, personType, behandlingOpprinnelse)
    }

    fun økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(evaluering: Evaluering, personType: PersonType) {
        LOG.info("øk teller for førsteutfallvilkår")
        økTellereForEvaluering(VilkårstellerType.FØRSTEUTFALL, evaluering, personType, BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)
    }

    fun økTellereForEvaluering(tellerType: VilkårstellerType, evaluering: Evaluering, personType: PersonType, behandlingOpprinnelse: BehandlingOpprinnelse) {
        if (evaluering.children.isEmpty()) {
            LOG.info("øk teller ${vilkårNøkkel(tellerType, evaluering.identifikator,
                                               personType,
                                               evaluering.resultat,
                                               behandlingOpprinnelse)} ${vilkårsvurderingUtfall[vilkårNøkkel(tellerType, evaluering.identifikator,
                                                                                                             personType,
                                                                                                             evaluering.resultat,
                                                                                                             behandlingOpprinnelse)]}")

            vilkårsvurderingUtfall[vilkårNøkkel(tellerType, evaluering.identifikator,
                                                personType,
                                                evaluering.resultat,
                                                behandlingOpprinnelse)]!!.increment()
        } else {
            evaluering.children.forEach { økTellereForEvaluering(it, personType, behandlingOpprinnelse) }
        }
    }

    companion object {
        val lovligOppholdUtfall = mutableMapOf<String, Counter>()

        fun økTellerForLovligOpphold(utfall: LovligOppholdUtfall) {
            lovligOppholdUtfall[utfall.name]?.increment()
        }

        val LOG = LoggerFactory.getLogger(VilkårsvurderingMetrics::class.java)
    }
}