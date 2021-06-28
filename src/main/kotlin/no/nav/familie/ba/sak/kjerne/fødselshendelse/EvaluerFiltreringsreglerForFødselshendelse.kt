package no.nav.familie.ba.sak.kjerne.fødselshendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.Fakta
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service

@Service
class EvaluerFiltreringsreglerForFødselshendelse(
        private val personopplysningerService: PersonopplysningerService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val localDateService: LocalDateService) {

    val filtreringsreglerMetrics = mutableMapOf<String, Counter>()
    val filtreringsreglerFørsteUtfallMetrics = mutableMapOf<String, Counter>()

    init {
        Filtreringsregler.values().map {
            Resultat.values().forEach { resultat ->
                filtreringsreglerMetrics[it.spesifikasjon.identifikator + resultat.name] =
                        Metrics.counter("familie.ba.sak.filtreringsregler.utfall",
                                        "beskrivelse",
                                        it.spesifikasjon.beskrivelse,
                                        "resultat",
                                        resultat.name)

                filtreringsreglerFørsteUtfallMetrics[it.spesifikasjon.identifikator] =
                    Metrics.counter(
                        "familie.ba.sak.filtreringsregler.foersteutfall",
                        "beskrivelse",
                        it.spesifikasjon.beskrivelse
                    )

            }
        }
    }


    fun evaluerFiltreringsregler(behandling: Behandling, barnasIdenter: Set<String>): Pair<Fakta, Evaluering> {
        val fakta = lagFaktaObjekt(behandling, barnasIdenter.toSet())

        val evaluering = Filtreringsregler.hentSamletSpesifikasjon().evaluer(fakta)
        oppdaterMetrikker(evaluering)
        return Pair(fakta, evaluering)
    }


    private fun lagFaktaObjekt(behandling: Behandling, barnasIdenter: Set<String>): Fakta {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val mor = personopplysningGrunnlag.søker

        val barnaFraHendelse = personopplysningGrunnlag.barna.filter { barnasIdenter.contains(it.personIdent.ident) }

        val restenAvBarna =
                personopplysningerService.hentPersoninfoMedRelasjoner(mor.personIdent.ident).forelderBarnRelasjon.filter {
                    it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN && barnaFraHendelse.none { barn -> barn.personIdent.ident == it.personIdent.id }
                }.map {
                    personopplysningerService.hentPersoninfoMedRelasjoner(it.personIdent.id)
                }

        val morLever = !personopplysningerService.hentDødsfall(Ident(mor.personIdent.ident)).erDød
        val barnLever = !barnaFraHendelse.any { personopplysningerService.hentDødsfall(Ident(it.personIdent.ident)).erDød }
        val morHarVerge = personopplysningerService.hentVergeData(Ident(mor.personIdent.ident)).harVerge

        return Fakta(mor, barnaFraHendelse, restenAvBarna, morLever, barnLever, morHarVerge, localDateService.now())
    }

    private fun økTellereForFørsteUtfall(evaluering: Evaluering, førsteutfall: Boolean): Boolean {
        if (evaluering.resultat == Resultat.IKKE_OPPFYLT && førsteutfall) {
            filtreringsreglerFørsteUtfallMetrics[evaluering.identifikator]!!.increment()
            return false
        }
        return førsteutfall
    }

    private fun oppdaterMetrikker(evaluering: Evaluering) {
        var førsteutfall = true
        if (evaluering.children.isEmpty()) {
            filtreringsreglerMetrics[evaluering.identifikator + evaluering.resultat.name]!!.increment()
            førsteutfall = økTellereForFørsteUtfall(evaluering, førsteutfall)
        } else {
            evaluering.children.forEach {
                filtreringsreglerMetrics[it.identifikator + it.resultat.name]!!.increment()
                førsteutfall = økTellereForFørsteUtfall(it, førsteutfall)
            }
        }
    }
}
