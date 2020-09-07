package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.Fakta
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.FAMILIERELASJONSROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EvaluerFiltreringsreglerForFødselshendelse(private val personopplysningerService: PersonopplysningerService,
                                                 private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository) {

    val filtreringsreglerMetrics = mutableMapOf<String, Counter>()

    init {
        Filtreringsregler.values().map {
            Resultat.values().forEach { resultat ->
                filtreringsreglerMetrics[it.spesifikasjon.identifikator + resultat.name] =
                        Metrics.counter("familie.ba.sak.filtreringsregler.utfall",
                                        "resultat",
                                        resultat.name,
                                        "beskrivelse",
                                        it.spesifikasjon.beskrivelse)
            }
        }
    }

    fun evaluerFiltreringsregler(behandling: Behandling, barnsIdenter: Set<String>): Evaluering {
        val evaluering = Filtreringsregler.hentSamletSpesifikasjon().evaluer(lagFaktaObjekt(behandling, barnsIdenter))
        oppdaterMetrikker(evaluering)
        return evaluering
    }

    private fun lagFaktaObjekt(behandling: Behandling, barnsIdenter: Set<String>): Fakta {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val mor = personopplysningGrunnlag.søker[0]
        val barn = personopplysningGrunnlag.barna.filter { barnsIdenter.contains(it.personIdent.ident) }
                ?: throw java.lang.IllegalStateException("Barnets ident er ikke tilstede i personopplysningsgrunnlaget.")

        val restenAvBarna =
                personopplysningerService.hentPersoninfoFor(personopplysningGrunnlag.søker[0].personIdent.ident).familierelasjoner.filter {
                    it.relasjonsrolle == FAMILIERELASJONSROLLE.BARN && !barn.map { m -> m.personIdent.ident }.toList().contains(it.personIdent.id)
                }.map {
                    personopplysningerService.hentPersoninfoFor(it.personIdent.id)
                }

        val morLever = !personopplysningerService.hentDødsfall(Ident(mor.personIdent.ident)).erDød
        val barnLever = !barn.any { personopplysningerService.hentDødsfall(Ident(it.personIdent.ident)).erDød }
        val morHarVerge = personopplysningerService.hentVergeData(Ident(mor.personIdent.ident)).harVerge

        return Fakta(mor, barn, restenAvBarna, morLever, barnLever, morHarVerge)
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