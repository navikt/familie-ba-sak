package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.GDPRService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.FaktaTilVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
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
        private val gdprService: GDPRService,
        private val vilkårService: VilkårService
) {


    fun sendTilInfotrygdFeed(barnIdenter: List<String>) {
        infotrygdFeedService.sendTilInfotrygdFeed(barnIdenter)
    }

    fun hentFagsystemForFødselshendelse(nyBehandling: NyBehandlingHendelse): VelgFagSystemService.FagsystemRegelVurdering {
        return velgFagSystemService.velgFagsystem(nyBehandlingHendelse = nyBehandling)
    }

    fun sendNyBehandlingHendelseTilFagsystem(nyBehandling: NyBehandlingHendelse): VelgFagSystemService.FagsystemRegelVurdering {
        return (hentFagsystemForFødselshendelse(nyBehandling))
    }

    fun harMorÅpenBehandlingIBASAK(nyBehandling: NyBehandlingHendelse): Boolean {
        val morsfagsak = fagsakService.hent(PersonIdent(nyBehandling.morsIdent))

        return morsfagsak != null && harSøkerÅpneBehandlinger(behandlingService.hentBehandlinger(morsfagsak.id))
    }

    fun sendTilBehandling(nyBehandling: NyBehandlingHendelse) {


        if (harMorÅpenBehandlingIBASAK(nyBehandling)) {
            val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
            opprettOppgaveForManuellBehandling(behandlingId = behandling.id,
                                               beskrivelse = "Fødselshendelse: Bruker har åpen behandling")
        } else {
            val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
            kjørFiltreringsregler(behandling = behandling, nyBehandling = nyBehandling)
        }
    }

    fun kjørFiltreringsregler(behandling: Behandling, nyBehandling: NyBehandlingHendelse): FiltreringsreglerResultat {
        return filtreringsreglerService.hentDataOgKjørFiltreringsregler(nyBehandling.morsIdent,
                                                                        nyBehandling.barnasIdenter.toSet(),
                                                                        behandling)
    }

    fun kjørVilkårvurdering(behandling: Behandling, nyBehandling: NyBehandlingHendelse) {
        val vilkårsVurderingsResultater = initierVilkårAutomatisk(behandling, nyBehandling.barnasIdenter)
        /* if (vilkårsVurderingsResultater != null && erVilkårOppfylt(vilkårsVurderingsResultater)) {
            TODO()
        } else {
            //opprett manuell behandling med vilkårsvurderinger ved siden
        }*/
    }

    //sommmerteam har laget for å vurdere saken automatisk basert på vilkår.
    fun initierVilkårAutomatisk(behandling: Behandling, nyeBarnsIdenter: List<String>): Vilkårsvurdering? {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return null
        return Vilkårsvurdering(behandling = behandling).apply {
            personResultater = initierVilkårsvurdering(personopplysningGrunnlag, nyeBarnsIdenter, this).toSet()
        }
    }

    //sommmerteam har laget for å vurdere saken automatisk basert på vilkår.
    fun initierVilkårsvurdering(personopplysningGrunnlag: PersonopplysningGrunnlag,
                                nyeBarnsIdenter: List<String>, vilkårsvurdering: Vilkårsvurdering): List<PersonResultat> {
        //sommerteam antar at hvis mor har en registrert nåværende adresse er hun bosatt i riket
        /*val mor = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna.filter { nyeBarnsIdenter.contains(it.personIdent.ident) }
        val morsSisteBosted = if (mor.bostedsadresser.isEmpty()) null else mor.bostedsadresser.sisteAdresse()*/
        //Sommerteam hopper over sjekk om mor og barn har lovlig opphold

        /*val morsResultat = vurderMor(morsSisteBosted, vilkårsvurdering)
        val resultatListe = mutableListOf(morsResultat)
        barna.forEach { resultatListe.add(vurderBarn(it, morsSisteBosted, vilkårsvurdering)) }*/
        val fødselsdatoEldsteBarn = personopplysningGrunnlag.personer
                                            .filter { it.type == PersonType.BARN }
                                            .maxByOrNull { it.fødselsdato }?.fødselsdato
                                    ?: error("Fant ikke barn i personopplysninger")

        return personopplysningGrunnlag.personer.filter { it.type != PersonType.ANNENPART }.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering,
                                                personIdent = person.personIdent.ident)

            val samletSpesifikasjonForPerson = Vilkår.hentSamletSpesifikasjonForPerson(person.type)
            val faktaTilVilkårsvurdering = FaktaTilVilkårsvurdering(personForVurdering = person)
            val evalueringForVilkårsvurdering = samletSpesifikasjonForPerson.evaluer(faktaTilVilkårsvurdering)

            gdprService.oppdaterFødselshendelsePreLanseringMedVilkårsvurderingForPerson(behandlingId = vilkårsvurdering.behandling.id,
                                                                                        faktaTilVilkårsvurdering = faktaTilVilkårsvurdering,
                                                                                        evaluering = evalueringForVilkårsvurdering)

            personResultat.setSortedVilkårResultater(
                    vilkårService.vilkårResultater(personResultat,
                                                   person,
                                                   faktaTilVilkårsvurdering,
                                                   evalueringForVilkårsvurdering,
                                                   fødselsdatoEldsteBarn)
            )
            personResultat
        }
    }

    fun opprettOppgaveForManuellBehandling(behandlingId: Long, beskrivelse: String?) {

        val nyTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now(),
                beskrivelse = beskrivelse
        )
        taskRepository.save(nyTask)
    }

}