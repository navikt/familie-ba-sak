package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
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
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val velgFagSystemService: VelgFagSystemService,
        private val infotrygdFeedService: InfotrygdFeedService,
) {
    

    fun hentFagsystemForFødselshendelse(nyBehandling: NyBehandlingHendelse): VelgFagSystemService.FagsystemRegelVurdering {
        return velgFagSystemService.velgFagsystem(nyBehandlingHendelse = nyBehandling)
    }

    fun sendNyBehandlingHendelseTilFagsystem(nyBehandling: NyBehandlingHendelse): VelgFagSystemService.FagsystemRegelVurdering {
        return (hentFagsystemForFødselshendelse(nyBehandling))
    }

    fun sjekkOmMorHarÅpentBehandlingIBASak(nyBehandling: NyBehandlingHendelse): Boolean {
        val morsfagsak = fagsakService.hent(PersonIdent(nyBehandling.morsIdent))

        return morsfagsak != null && harSøkerÅpneBehandlinger(behandlingService.hentBehandlinger(morsfagsak.id))
    }

    fun sendTilBehandling(nyBehandling: NyBehandlingHendelse) {


        if (sjekkOmMorHarÅpentBehandlingIBASak(nyBehandling)) {
            val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
            opprettOppgaveForManuellBehandling(behandlingId = behandling.id,
                                               beskrivelse = "Fødselshendelse: Bruker har åpen behandling")
        } else {
            val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
            kjørFiltreringsreglerOgOpprettBehandling(behandling = behandling, nyBehandling = nyBehandling)
        }
    }

    fun kjørFiltreringsreglerOgOpprettBehandling(behandling: Behandling, nyBehandling: NyBehandlingHendelse) {
        val filtreringsResultat =
                filtreringsreglerService.hentDataOgKjørFiltreringsregler(nyBehandling.morsIdent,
                                                                         nyBehandling.barnasIdenter.toSet(),
                                                                         behandling)
        if (filtreringsResultat == FiltreringsreglerResultat.GODKJENT) {
            kjørVilkårvurdering(behandling, nyBehandling)
        } else {
            opprettOppgaveForManuellBehandling(behandlingId = behandling.id, beskrivelse = filtreringsResultat.beskrivelse)
        }
    }

    fun kjørVilkårvurdering(behandling: Behandling, nyBehandling: NyBehandlingHendelse) {
        val vilkårsVurderingsResultater = initierVilkårAutomatisk(behandling, nyBehandling.barnasIdenter)
        if (vilkårsVurderingsResultater != null && erVilkårOppfylt(vilkårsVurderingsResultater)) {
            //godkjent
        } else {
            //opprett manuell behandling med vilkårsvurderinger ved siden
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