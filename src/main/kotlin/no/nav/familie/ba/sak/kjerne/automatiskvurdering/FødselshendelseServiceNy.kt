package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FødselshendelseServiceNy(
        private val filtreringsreglerService: FiltreringsreglerService,
        private val taskRepository: TaskRepository,
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val velgFagSystemService: VelgFagSystemService,
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,
        private val persongrunnlagService: PersongrunnlagService
) {

    fun hentFagsystemForFødselshendelse(nyBehandling: NyBehandlingHendelse): FagsystemRegelVurdering {
        return velgFagSystemService.velgFagsystem(nyBehandlingHendelse = nyBehandling)
    }

    fun hentÅpenBehandling(ident: String): Behandling? {
        return fagsakService.hent(PersonIdent(ident)).let {
            if (it == null) null else behandlingService.hentAktivOgÅpenForFagsak(it.id)
        }
    }

    fun kjørFiltreringsregler(behandling: Behandling, nyBehandling: NyBehandlingHendelse): List<Evaluering> {
        return filtreringsreglerService.kjørFiltreringsregler(nyBehandling.morsIdent,
                                                              nyBehandling.barnasIdenter.toSet(),
                                                              behandling)
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

    internal fun hentBegrunnelseFraVilkårsvurdering(behandlingId: Long): String? {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
        val behandling = behandlingService.hent(behandlingId)
        val søker = persongrunnlagService.hentSøker(behandling.id)
        val søkerResultat = vilkårsvurdering?.personResultater?.find { it.personIdent == søker?.personIdent?.ident }

        val bosattIRiketResultat = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        if (bosattIRiketResultat?.resultat == Resultat.IKKE_OPPFYLT) {
            return "Mor er ikke bosatt i riket."
        }

        persongrunnlagService.hentBarna(behandling).forEach { barn ->
            val vilkårsresultat =
                    vilkårsvurdering?.personResultater?.find { it.personIdent == barn.personIdent.ident }?.vilkårResultater

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato}) er over 18 år."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.BOR_MED_SØKER }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato}) er ikke bosatt med mor."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato}) er gift."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato}) er ikke bosatt i riket."
            }
        }

        return null
    }

}