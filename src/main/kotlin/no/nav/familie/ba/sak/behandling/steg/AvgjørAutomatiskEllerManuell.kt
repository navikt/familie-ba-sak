package no.nav.familie.ba.sak.behandling.steg

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.filtreringsregler.Fakta
import no.nav.familie.ba.sak.behandling.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.FAMILIERELASJONSROLLE
import no.nav.familie.kontrakter.felles.personinfo.Ident
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
class AvgjørAutomatiskEllerManuellBehandlingForFødselshendelser(private val integrasjonClient: IntegrasjonClient,
                                                                private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
                                                                private val taskRepository: TaskRepository,
                                                                private val featureToggleService: FeatureToggleService)
    : BehandlingSteg<String> {

    override fun stegType(): StegType {
        return StegType.AVGJØR_AUTOMATISK_ELLER_MANUELL_BEHANDLING_FOR_FØDSELSHENDELSER
    }

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        val evaluering = Filtreringsregler.hentSamletSpesifikasjon().evaluer(lagFaktaObjekt(behandling))

        oppdaterMetrikker(evaluering)

        if (evaluering.resultat == Resultat.JA) {
            // TODO Fortsett med vilkårsvurdering
        } else {
            opprettOppgave(behandling)
            throw java.lang.IllegalStateException("Behandles manuelt. Går ikke videre til vilkårsvurdering")
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun lagFaktaObjekt(behandling: Behandling): Fakta {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val mor = personopplysningGrunnlag.søker[0]
        val barnet = personopplysningGrunnlag.barna[0]
        val restenAvBarna =
                integrasjonClient.hentPersoninfoFor(personopplysningGrunnlag.søker[0].personIdent.ident).familierelasjoner.filter {
                    it.relasjonsrolle == FAMILIERELASJONSROLLE.BARN && it.personIdent.id != barnet.personIdent.ident
                }.map {
                    integrasjonClient.hentPersoninfoFor(it.personIdent.id)
                }

        val morLever = !integrasjonClient.hentDødsfall(Ident(mor.personIdent.ident)).erDød
        val barnetLever = !integrasjonClient.hentDødsfall(Ident(barnet.personIdent.ident)).erDød
        val morHarVerge = integrasjonClient.hentVergeData(Ident(mor.personIdent.ident)).harVerge

        return Fakta(mor, barnet, restenAvBarna, morLever, barnetLever, morHarVerge)
    }

    private fun oppdaterMetrikker(evaluering: Evaluering) {
        evaluering.children.forEach {
            val counter = Metrics.counter(
                    "barnetrygd.hendelse.filtreringsregler.${it.identifikator.toLowerCase()}.${jaNei(it.resultat)}",
                    "beskrivelse",
                    it.beskrivelse,
                    "begrunnelse",
                    it.begrunnelse)
            counter.increment()
        }

        when (evaluering.resultat) {
            Resultat.JA -> Metrics.counter("barnetrygd.hendelse.filtreringsregler.behandles.automatisk").increment()
            Resultat.NEI -> Metrics.counter("barnetrygd.hendelse.filtreringsregler.manuell.behandling").increment()
            else -> {}
        }
    }

    private fun jaNei(resultat: Resultat): String {
        return when (resultat) {
            Resultat.JA -> "ja"
            Resultat.NEI -> "nei"
            Resultat.KANSKJE -> "kanskje"
        }
    }

    private fun opprettOppgave(behandling: Behandling) {
        if (featureToggleService.isEnabled("familie-ba-sak.lag-oppgave")) {
            val nyTask = OpprettOppgaveTask.opprettTask(
                    behandlingId = behandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    fristForFerdigstillelse = LocalDate.now()
            )
            taskRepository.save(nyTask)
        } else {
            Vilkårsvurdering.LOG.info("Lag opprettOppgaveTask er skrudd av i miljø")
        }
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    }
}