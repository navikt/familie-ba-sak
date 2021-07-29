package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.filtreringsregler.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.erOppfylt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FødselshendelseService(
        private val filtreringsreglerService: FiltreringsreglerService,
        private val taskRepository: TaskRepository,
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,
        private val persongrunnlagService: PersongrunnlagService,
        private val stegService: StegService,
        private val vedtakService: VedtakService,
        private val vedtaksperiodeService: VedtaksperiodeService
) {

    val stansetIAutomatiskFiltreringCounter =
            Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "filtrering")
    val stansetIAutomatiskVilkårsvurderingCounter =
            Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "vilkaarsvurdering")
    val passertFiltreringOgVilkårsvurderingCounter = Metrics.counter("familie.ba.sak.henvendelse.passert")


    fun behandleFødselshendelse(nyBehandling: NyBehandlingHendelse) {
        val morsÅpneBehandling = hentÅpenBehandling(ident = nyBehandling.morsIdent)
        if (morsÅpneBehandling != null) {
            opprettOppgaveForManuellBehandling(
                    morsÅpneBehandling.id,
                    "Fødselshendelse: Bruker har åpen behandling"
            )
            return
        }

        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val evalueringer = filtreringsreglerService.kjørFiltreringsregler(nyBehandling.morsIdent,
                                                                          nyBehandling.barnasIdenter.toSet(),
                                                                          behandling)

        if (!evalueringer.erOppfylt()) {
            henleggBehandlingOgOpprettManuellOppgave(
                    behandling = behandling,
                    begrunnelse = evalueringer.first { it.resultat == Resultat.IKKE_OPPFYLT }.begrunnelse,
            )

            stansetIAutomatiskFiltreringCounter.increment()
        } else vurderVilkår(behandling = behandling, barnaSomVurderes = nyBehandling.barnasIdenter)
    }

    private fun vurderVilkår(behandling: Behandling, barnaSomVurderes: List<String>) {
        val behandlingEtterVilkårsVurdering = stegService.håndterVilkårsvurdering(behandling = behandling)
        if (behandlingEtterVilkårsVurdering.resultat == BehandlingResultat.INNVILGET) {
            val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)
            vedtaksperiodeService.oppdaterVedtaksperioderForBarnVurdertIFødselshendelse(vedtak, barnaSomVurderes)

            val vedtakEtterToTrinn =
                    vedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling = behandlingEtterVilkårsVurdering)

            val task = IverksettMotOppdragTask.opprettTask(behandling, vedtakEtterToTrinn, SikkerhetContext.hentSaksbehandler())
            taskRepository.save(task)

            passertFiltreringOgVilkårsvurderingCounter.increment()
        } else {
            henleggBehandlingOgOpprettManuellOppgave(behandling = behandlingEtterVilkårsVurdering)

            stansetIAutomatiskVilkårsvurderingCounter
        }
    }

    private fun hentÅpenBehandling(ident: String): Behandling? {
        return fagsakService.hent(PersonIdent(ident)).let {
            if (it == null) null else behandlingService.hentAktivOgÅpenForFagsak(it.id)
        }
    }

    private fun henleggBehandlingOgOpprettManuellOppgave(
            behandling: Behandling,
            begrunnelse: String = "",
    ) {
        val begrunnelseForManuellOppgave = if (begrunnelse == "") {
            hentBegrunnelseFraVilkårsvurdering(behandlingId = behandling.id)
        } else {
            begrunnelse
        }

        logger.info("Henlegger behandling ${behandling.id} automatisk på grunn av ugyldig resultat")
        secureLogger.info("Henlegger behandling ${behandling.id} automatisk på grunn av ugyldig resultat. Begrunnelse: $begrunnelse")

        stegService.håndterHenleggBehandling(behandling = behandling, henleggBehandlingInfo = RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.FØDSELSHENDELSE_UGYLDIG_UTFALL,
                begrunnelse = "Automatisk henlagt: $begrunnelseForManuellOppgave" // TODO: avklar denne meldingen med fag
        ))

        opprettOppgaveForManuellBehandling(
                behandlingId = behandling.id,
                begrunnelse = begrunnelseForManuellOppgave
        )
    }

    private fun opprettOppgaveForManuellBehandling(behandlingId: Long, begrunnelse: String?) {
        val nyTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now(),
                beskrivelse = begrunnelse
        )
        taskRepository.save(nyTask)
    }

    private fun hentBegrunnelseFraVilkårsvurdering(behandlingId: Long): String? {
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
                return "Barnet (fødselsdato: ${barn.fødselsdato.tilKortString()}) er over 18 år."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.BOR_MED_SØKER }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato.tilKortString()}) er ikke bosatt med mor."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato.tilKortString()}) er gift."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat == Resultat.IKKE_OPPFYLT) {
                return "Barnet (fødselsdato: ${barn.fødselsdato.tilKortString()}) er ikke bosatt i riket."
            }
        }

        return null
    }


    companion object {

        private val logger = LoggerFactory.getLogger(BehandleFødselshendelseTask::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}