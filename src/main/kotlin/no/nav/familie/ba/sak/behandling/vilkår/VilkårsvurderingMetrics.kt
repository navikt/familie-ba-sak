package no.nav.familie.ba.sak.behandling.vilkår

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vilkår.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.behandling.vilkår.utfall.VilkårKanskjeOppfyltÅrsak
import no.nav.familie.ba.sak.behandling.vilkår.utfall.VilkårOppfyltÅrsak
import no.nav.familie.ba.sak.nare.Resultat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class VilkårsvurderingMetrics(
        private val persongrunnlagService: PersongrunnlagService
) {

    val vilkårsvurderingUtfall = mutableMapOf<PersonType, Map<String, Counter>>()
    val vilkårsvurderingFørsteUtfall = mutableMapOf<PersonType, Map<String, Counter>>()

    val personTypeToDisplayedType = mapOf(
            PersonType.SØKER to "Mor",
            PersonType.BARN to "Barn",
            PersonType.ANNENPART to "Medforelder"
    )

    init {
        PersonType.values().forEach { personType ->
            val vilkårUtfallMap = mutableMapOf<String, Counter>()
            listOf(Pair(Resultat.NEI, VilkårIkkeOppfyltÅrsak.values()),
                   Pair(Resultat.KANSKJE, VilkårKanskjeOppfyltÅrsak.values()),
                   Pair(Resultat.JA, VilkårOppfyltÅrsak.values()))
                    .forEach { (resultat, årsaker) ->
                        årsaker
                                .forEach { årsak ->
                                    if (vilkårUtfallMap[årsak.toString()] != null)
                                        error("Årsak $årsak deler navn med minst en annen årsak")

                                    val vilkår = Vilkår.valueOf(årsak.hentIdentifikator())

                                    if (vilkår.parterDetteGjelderFor.contains(personType)) {
                                        vilkårUtfallMap[årsak.toString()] =
                                                Metrics.counter("familie.ba.behandling.vilkaarsvurdering",
                                                                "vilkaar",
                                                                årsak.hentIdentifikator(),
                                                                "resultat",
                                                                resultat.name,
                                                                "personType",
                                                                personTypeToDisplayedType[personType],
                                                                "beskrivelse",
                                                                årsak.hentMetrikkBeskrivelse())
                                    }
                                }
                    }

            vilkårsvurderingUtfall[personType] = vilkårUtfallMap
            vilkårsvurderingFørsteUtfall[personType] = vilkårUtfallMap
        }
    }

    fun økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(vilkårResultat: VilkårResultat) {
        val behandlingId = vilkårResultat.personResultat?.behandlingResultat?.behandling?.id!!
        val personer = persongrunnlagService.hentAktiv(behandlingId)?.personer
                       ?: error("Finner ikke aktivt persongrunnlag ved telling av metrikker")

        val person = personer.firstOrNull { it.personIdent.ident == vilkårResultat.personResultat?.personIdent }
                     ?: error("Finner ikke person")

        logger.info("Første vilkår med feil=$vilkårResultat, på behandling $behandlingId")
        vilkårResultat.evalueringÅrsaker.forEach { årsak ->
            vilkårsvurderingFørsteUtfall[person.type]?.get(årsak)?.increment()
        }
    }

    fun tellMetrikker(behandlingResultat: BehandlingResultat) {

        val personer = persongrunnlagService.hentAktiv(behandlingResultat.behandling.id)?.personer
                       ?: error("Finner ikke aktivt persongrunnlag ved telling av metrikker")

        behandlingResultat.personResultater.forEach { personResultat ->
            val person = personer.firstOrNull { it.personIdent.ident == personResultat.personIdent }
                         ?: error("Finner ikke person")

            val neiÅrsaker = personResultat.vilkårResultater.filter { vilkårResultat ->
                vilkårResultat.resultat == Resultat.NEI
            }.map { it.evalueringÅrsaker }.flatten()

            logger.info("Årsaker til NEI for ${person.type}=$neiÅrsaker på behandling ${behandlingResultat.behandling.id}")
            secureLogger.info("Årsaker til NEI for ${person.personIdent.ident}=$neiÅrsaker på behandling ${behandlingResultat.behandling.id}")

            personResultat.vilkårResultater.forEach { vilkårResultat ->
                vilkårResultat.evalueringÅrsaker.forEach { årsak ->
                    vilkårsvurderingUtfall[person.type]?.get(årsak)?.increment()
                }
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}