package no.nav.familie.ba.sak.behandling.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.filtreringsregler.Fakta
import no.nav.familie.ba.sak.behandling.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.FAMILIERELASJONSROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AvgjørAutomatiskEllerManuellBehandlingForFødselshendelser(private val personopplysningerService: PersonopplysningerService,
                                                                private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                                                                private val taskRepository: TaskRepository,
                                                                private val featureToggleService: FeatureToggleService)
    : BehandlingSteg<String> {

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

    override fun stegType(): StegType {
        return StegType.AVGJØR_AUTOMATISK_ELLER_MANUELL_BEHANDLING_FOR_FØDSELSHENDELSER
    }

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        val evaluering = Filtreringsregler.hentSamletSpesifikasjon().evaluer(lagFaktaObjekt(behandling, data))

        oppdaterMetrikker(evaluering)

        /*
        if (evaluering.resultat == Resultat.JA) {
            // TODO Fortsett med vilkårsvurdering
        } else {
            // TODO Ikke fortsett med vilkårsvurdering, og opprett oppgave
            // opprettOppgave(behandling)
        }
        */

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun lagFaktaObjekt(behandling: Behandling, barnetsIdent: String): Fakta {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val mor = personopplysningGrunnlag.søker[0]
        val barnet = personopplysningGrunnlag.barna.filter { it.personIdent.ident == barnetsIdent }.firstOrNull()
                ?: throw java.lang.IllegalStateException("Barnets ident er ikke tilstede i personopplysningsgrunnlaget.")

        val restenAvBarna =
                personopplysningerService.hentPersoninfoFor(personopplysningGrunnlag.søker[0].personIdent.ident).familierelasjoner.filter {
                    it.relasjonsrolle == FAMILIERELASJONSROLLE.BARN && it.personIdent.id != barnet.personIdent.ident
                }.map {
                    personopplysningerService.hentPersoninfoFor(it.personIdent.id)
                }

        val morLever = !personopplysningerService.hentDødsfall(Ident(mor.personIdent.ident)).erDød
        val barnetLever = !personopplysningerService.hentDødsfall(Ident(barnet.personIdent.ident)).erDød
        val morHarVerge = personopplysningerService.hentVergeData(Ident(mor.personIdent.ident)).harVerge

        return Fakta(mor, barnet, restenAvBarna, morLever, barnetLever, morHarVerge)
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