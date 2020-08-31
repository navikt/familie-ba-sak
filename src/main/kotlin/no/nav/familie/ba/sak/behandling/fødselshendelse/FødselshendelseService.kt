package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
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
import no.nav.nare.core.evaluations.Evaluering
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
                             private val personopplysningerService: PersonopplysningerService,
                             private val behandlingResultatRepository: BehandlingResultatRepository,
                             private val persongrunnlagService: PersongrunnlagService,
                             private val behandlingRepository: BehandlingRepository) {

    val finnesHosInfotrygdCounter: Counter = Metrics.counter("fødselshendelse.mor.eller.barn.finnes.i.infotrygd")
    val finnesIkkeHosInfotrygdCounter: Counter = Metrics.counter("fødselshendelse.mor.eller.barn.finnes.ikke.i.infotrygd")
    val stansetIAutomatiskFiltreringCounter = Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "filtrering")
    val stansetIAutomatiskVilkårsvurderingCounter =
            Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "vilkaarsvurdering")
    val passertFiltreringOgVilkårsvurderingCounter = Metrics.counter("familie.ba.sak.henvendelse.passert")

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

        val evalueringAvFiltrering =
                evaluerFiltreringsreglerForFødselshendelse.evaluerFiltreringsregler(behandling, nyBehandling.barnasIdenter[0])
        var resultatAvVilkårsvurdering: BehandlingResultatType? = null

        if (evalueringAvFiltrering.resultat == Resultat.JA) {
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
            if (evalueringAvFiltrering.resultat !== Resultat.JA || resultatAvVilkårsvurdering !== BehandlingResultatType.INNVILGET) {
                val beskrivelse = when (resultatAvVilkårsvurdering) {
                    null -> hentBeskrivelseFraEvaluering(evalueringAvFiltrering)
                    else -> hentBeskrivelseFraBehandlingResultat(behandling.id)
                }

                opprettOppgaveForManuellBehandling(behandlingId = behandling.id, beskrivelse = beskrivelse)
            } else {
                val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                             ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
                IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
            }

        }
    }

    private fun hentBeskrivelseFraBehandlingResultat(behandlingId: Long): String? {
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandlingId)
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val søker = persongrunnlagService.hentSøker(behandling)
        val søkerResultat = behandlingResultat?.personResultater?.find { it.personIdent == søker?.personIdent?.ident }

        val bosattIRiketResultat = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        if (bosattIRiketResultat?.resultat == Resultat.NEI) {
            return "Mor er ikke bosatt i riket"
        }

        val harLovligOpphold = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
        if (harLovligOpphold?.resultat == Resultat.NEI) {
            return harLovligOpphold.begrunnelse
        }

        val barna = persongrunnlagService.hentBarna(behandling)

        barna.forEach { barnet ->
            val barnetResultat = behandlingResultat?.personResultater?.find { it.personIdent == barnet.personIdent.ident }

            val under18År = barnetResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

            if (under18År?.resultat == Resultat.NEI) {
                return under18År?.begrunnelse
            }

            val børMedSøker = barnetResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BOR_MED_SØKER }
            if (børMedSøker?.resultat == Resultat.NEI) {
                return børMedSøker?.begrunnelse
            }
        }
    }

    private fun hentBeskrivelseFraEvaluering(evaluering: Evaluering): String? =
            evaluering.children.find { it.resultat == Resultat.NEI }?.begrunnelse

    private fun opprettOppgaveForManuellBehandling(behandlingId: Long, beskrivelse: String?) {

        val nyTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now(),
                beskrivelse = beskrivelse
        )
        taskRepository.save(nyTask)
    }

    private fun fødselshendelseSkalRullesTilbake(): Boolean =
            featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")
}