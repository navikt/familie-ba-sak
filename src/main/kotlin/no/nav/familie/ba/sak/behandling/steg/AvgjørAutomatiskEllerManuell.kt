package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
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
        // TODO: Kjør filtreringsregler som avgjør om fødselshendelsen skal behandles automatisk eller manuelt
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)
                                       ?: throw IllegalStateException("Fant ikke personopplysninggrunnlag for behandling ${behandling.id}")

        val søker = personopplysningGrunnlag.søker[0]
        val barnet = personopplysningGrunnlag.barna[0]

        barnet.fødselsdato

        if (erDnummer(søker.personIdent.ident) ) {
            opprettOppgave(behandling)
        }

        if (erDnummer(barnet.personIdent.ident)) {
            opprettOppgave(behandling)
        }

        if (erOver6mnd(barnet)) {
            opprettOppgave(behandling)
        }

        if (erUnder18år(søker)) {
            opprettOppgave(behandling)
        }



        return hentNesteStegForNormalFlyt(behandling)
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


    private fun erDnummer(personIdent: String): Boolean {
        return personIdent.substring(0, 1).toInt() > 3
    }

    private fun erOver6mnd(person: Person): Boolean {
        return LocalDate.now().minusMonths(6).isAfter(person.fødselsdato)
    }

    private fun erUnder18år(person: Person): Boolean {
        return LocalDate.now().isBefore(person.fødselsdato.plusYears(18))
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    }
}