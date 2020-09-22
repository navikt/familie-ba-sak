package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.Fakta
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.FAMILIERELASJONSROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EvaluerFiltreringsreglerForFødselshendelse(
        private val personopplysningerService: PersonopplysningerService,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val localDateService: LocalDateService) {

    val filtreringsreglerMetrics = mutableMapOf<String, Counter>()

    init {
        Filtreringsregler.values().map {
            Resultat.values().forEach { resultat ->
                filtreringsreglerMetrics[it.spesifikasjon.identifikator + resultat.name] =
                        Metrics.counter("familie.ba.sak.filtreringsregler.utfall",
                                        "beskrivelse",
                                        it.spesifikasjon.beskrivelse,
                                        "resultat",
                                        resultat.name)
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

        val mor = personopplysningGrunnlag.søker.firstOrNull()
                  ?: error("Fant ingen personer i grunnlaget med type=søker.")

        val barnaFraHendelse = personopplysningGrunnlag.barna.filter { barnasIdenter.contains(it.personIdent.ident) }

        val restenAvBarna =
                personopplysningerService.hentPersoninfoMedRelasjoner(personopplysningGrunnlag.søker[0].personIdent.ident).familierelasjoner.filter {
                    it.relasjonsrolle == FAMILIERELASJONSROLLE.BARN && barnaFraHendelse.none { barn -> barn.personIdent.ident == it.personIdent.id }
                }.map {
                    personopplysningerService.hentPersoninfoMedRelasjoner(it.personIdent.id)
                }

        val morLever = !personopplysningerService.hentDødsfall(Ident(mor.personIdent.ident)).erDød
        val barnLever = !barnaFraHendelse.any { personopplysningerService.hentDødsfall(Ident(it.personIdent.ident)).erDød }
        val morHarVerge = personopplysningerService.hentVergeData(Ident(mor.personIdent.ident)).harVerge

        return Fakta(mor, barnaFraHendelse, restenAvBarna, morLever, barnLever, morHarVerge, localDateService.now())
    }

    private fun oppdaterMetrikker(evaluering: Evaluering) {
        if (evaluering.children.isEmpty()) {
            filtreringsreglerMetrics[evaluering.identifikator + evaluering.resultat.name]?.increment()
        } else {
            evaluering.children.forEach {
                filtreringsreglerMetrics[it.identifikator + it.resultat.name]?.increment()
            }
        }
    }

    companion object {

        val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
