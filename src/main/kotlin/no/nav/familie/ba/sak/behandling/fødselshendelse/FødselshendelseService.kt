package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.KontrollertRollbackException
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.nare.core.evaluations.Resultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class FødselshendelseService(private val infotrygdFeedService: InfotrygdFeedService,
                             private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                             private val featureToggleService: FeatureToggleService,
                             private val stegService: StegService,
                             private val vedtakService: VedtakService,
                             private val evaluerFiltreringsreglerForFødselshendelse: EvaluerFiltreringsreglerForFødselshendelse,
                             private val taskRepository: TaskRepository,
                             private val personopplysningerService: PersonopplysningerService) {

    val finnesHosInfotrygdCounter: Counter = Metrics.counter("fødselshendelse.mor.eller.barn.finnes.i.infotrygd")
    val finnesIkkeHosInfotrygdCounter: Counter = Metrics.counter("fødselshendelse.mor.eller.barn.finnes.ikke.i.infotrygd")
    val stansetIAutomatiskFiltreringCounter = Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "filtrering")
    val stansetIAutomatiskVilkårsvurderingCounter = Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "vilkaarsvurdering")
    val passertFiltreringOgVilkårsvurderingCounter =  Metrics.counter("familie.ba.sak.henvendelse.passert")

    fun fødselshendelseSkalBehandlesHosInfotrygd(morsIdent: String, barnasIdenter: List<String>): Boolean {

        val morsIdenter = personopplysningerService.hentIdenter(Ident(morsIdent))
                .filter { it.gruppe == "FOLKEREGISTERIDENT" }
                .map { it.ident }
        val alleBarnasIdenter = barnasIdenter.flatMap {
            personopplysningerService.hentIdenter(Ident(it))
                    .filter { identinfo -> identinfo.gruppe == "FOLKEREGISTERIDENT" }
                    .map { identinfo -> identinfo.ident }
        }

        val finnesHosInfotrygd = !infotrygdBarnetrygdClient.finnesIkkeHosInfotrygd(morsIdenter, alleBarnasIdenter)
        when (finnesHosInfotrygd) {
            true -> finnesHosInfotrygdCounter.increment()
            false -> finnesIkkeHosInfotrygdCounter.increment()
        }

        return finnesHosInfotrygd
    }

    fun sendTilInfotrygdFeed(barnIdenter: List<String>) {
        infotrygdFeedService.sendTilInfotrygdFeed(barnIdenter)
    }

    @Transactional
    fun opprettBehandlingOgKjørReglerForFødselshendelse(nyBehandling: NyBehandlingHendelse) {
        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)

        val resultatAvFiltrering = Resultat.JA
                //evaluerFiltreringsreglerForFødselshendelse.evaluerFiltreringsregler(behandling, nyBehandling.barnasIdenter[0])
                 //       .resultat
        var resultatAvVilkårsvurdering: BehandlingResultatType? = null

        if (resultatAvFiltrering == Resultat.JA) {
            resultatAvVilkårsvurdering = stegService.evaluerVilkårForFødselshendelse(behandling, nyBehandling.søkersIdent)
        }

        when (resultatAvVilkårsvurdering) {
            null -> stansetIAutomatiskFiltreringCounter.increment()
            BehandlingResultatType.INNVILGET -> passertFiltreringOgVilkårsvurderingCounter.increment()
            else -> stansetIAutomatiskVilkårsvurderingCounter.increment()
        }

        if (fødselshendelseSkalRullesTilbake()) {
            throw KontrollertRollbackException()
        } else {
            if (resultatAvFiltrering !== Resultat.JA || resultatAvVilkårsvurdering !== BehandlingResultatType.INNVILGET) {
                opprettOppgaveForManuellBehandling(behandlingId = behandling.id)
            } else {
                val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                             ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
                vedtakService.oppdaterVedtakMedStønadsbrev(vedtak)
                IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
            }

        }
    }

    private fun opprettOppgaveForManuellBehandling(behandlingId: Long) {

        val nyTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now()
        )
        taskRepository.save(nyTask)
    }

    private fun fødselshendelseSkalRullesTilbake(): Boolean =
            featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")
}