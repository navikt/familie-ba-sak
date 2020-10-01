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

    enum class VilkårTellerType(val navn: String) {
        UTFALL("familie.ba.behandling.vilkaarsvurdering"),
        FØRSTEUTFALL("familie.ba.behandling.vilkaarsvurdering.foerstutfall")
    }

    init {
        initVilkårMetrikker(VilkårTellerType.UTFALL, vilkårsvurderingUtfall)
        initVilkårMetrikker(VilkårTellerType.FØRSTEUTFALL, vilkårsvurderingFørsteUtfall)
    }

    fun initVilkårMetrikker(vilkårTellerType: VilkårTellerType, utfallMap: MutableMap<PersonType, Map<String, Counter>>) {
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
                                                Metrics.counter(vilkårTellerType.navn,
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

            utfallMap[personType] = vilkårUtfallMap
        }
    }

    fun økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(vilkårResultat: VilkårResultat) {
        val behandlingId = vilkårResultat.personResultat?.behandlingResultat?.behandling?.id!!
        val personer = persongrunnlagService.hentAktiv(behandlingId)?.personer
                       ?: error("Finner ikke aktivt persongrunnlag ved telling av metrikker")

        val person = personer.firstOrNull { it.personIdent.ident == vilkårResultat.personResultat?.personIdent }
                     ?: error("Finner ikke person")

        logger.info("Første vilkår med feil=$vilkårResultat, på personType=${person.type}, på behandling $behandlingId")
        secureLogger.info("Første vilkår med feil=$vilkårResultat, på person=${person.personIdent.ident}, på behandling $behandlingId")
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

            val negativeVilkår = personResultat.vilkårResultater.filter { vilkårResultat ->
                vilkårResultat.resultat == Resultat.NEI
            }

            if (negativeVilkår.isNotEmpty()) {
                logger.info("Behandling: ${behandlingResultat.behandling.id}, personType=${person.type}. Vilkår som får negativt resultat og årsakene: ${negativeVilkår.map { "${it.vilkårType}=${it.evalueringÅrsaker}" }}.")
                secureLogger.info("Behandling: ${behandlingResultat.behandling.id}, person=${person.personIdent.ident}. Vilkår som får negativt resultat og årsakene: ${negativeVilkår.map { "${it.vilkårType}=${it.evalueringÅrsaker}" }}.")
            }

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