package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FødselshendelseServiceNy(
        private val stegService: StegService,
        private val filtreringsreglerService: FiltreringsreglerService,
        private val taskRepository: TaskRepository,
        private val persongrunnlagService: PersongrunnlagService,
) {

    fun kjørFiltreringsreglerOgOpprettBehandling(nyBehandling: NyBehandlingHendelse) {
        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val filtreringsResultat =
                filtreringsreglerService.hentDataOgKjørFiltreringsregler(nyBehandling.morsIdent,
                                                                         nyBehandling.barnasIdenter.toSet(),
                                                                         behandling)
        if (filtreringsResultat == FiltreringsreglerResultat.GODKJENT) {
            //videre til vilkår
        } else {
            opprettOppgaveForManuellBehandling(behandlingId = behandling.id, beskrivelse = filtreringsResultat.beskrivelse)
        }
    }


    //sommmerteam har laget for å vurdere saken automatisk basert på vilkår.
    fun initierVilkårAutomatisk(behandling: Behandling, nyeBarnsIdenter: List<String>): List<PersonResultat>? {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return null
        return initierVilkårsvurdering(personopplysningGrunnlag, nyeBarnsIdenter)
    }

    private fun opprettOppgaveForManuellBehandling(behandlingId: Long, beskrivelse: String?) {

        val nyTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now(),
                beskrivelse = beskrivelse
        )
        taskRepository.save(nyTask)
    }

}