package no.nav.familie.ba.sak.behandling.fødselshendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.fødselshendelse.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.*
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
                             private val behandlingRepository: BehandlingRepository,
                             private val vilkårsvurderingMetrics: VilkårsvurderingMetrics) {

    val finnesLøpendeSakIInfotrygd: Counter = Metrics.counter("foedselshendelse.mor.eller.barn.finnes.loepende.i.infotrygd")
    val finnesIkkeLøpendeSakIInfotrygd: Counter =
            Metrics.counter("foedselshendelse.mor.eller.barn.finnes.ikke.loepende.i.infotrygd")
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
            true -> finnesLøpendeSakIInfotrygd.increment()
            false -> finnesIkkeLøpendeSakIInfotrygd.increment()
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
                evaluerFiltreringsreglerForFødselshendelse.evaluerFiltreringsregler(behandling,
                                                                                    nyBehandling.barnasIdenter.toSet())
        var resultatAvVilkårsvurdering: BehandlingResultatType? = null

        if (evalueringAvFiltrering.resultat == Resultat.JA) {
            resultatAvVilkårsvurdering = stegService.evaluerVilkårForFødselshendelse(behandling, nyBehandling.søkersIdent)
        } 

        when (resultatAvVilkårsvurdering) {
            null -> stansetIAutomatiskFiltreringCounter.increment()
            BehandlingResultatType.INNVILGET -> passertFiltreringOgVilkårsvurderingCounter.increment()
            else -> økTellereForStansetIAutomatiskVilkårsvurdering(behandling)
        }

        if (fødselshendelseSkalRullesTilbake()) {
            throw KontrollertRollbackException()
        } else {
            if (evalueringAvFiltrering.resultat !== Resultat.JA || resultatAvVilkårsvurdering !== BehandlingResultatType.INNVILGET) {
                val beskrivelse = when (resultatAvVilkårsvurdering) {
                    null -> hentBegrunnelseFraFiltreringsregler(evalueringAvFiltrering)
                    else -> hentBegrunnelseFraVilkårsvurdering(behandling.id)
                }

                opprettOppgaveForManuellBehandling(behandlingId = behandling.id, beskrivelse = beskrivelse)
            } else {
                val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                             ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")
                vedtakService.oppdaterVedtakMedStønadsbrev(vedtak)
                IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
            }
        }
    }

    private fun erVilkårUtfall(behandlingResultat: BehandlingResultat, personType: PersonType, vilkår: Vilkår): Boolean {
        val personer = if (personType == PersonType.SØKER) {
            listOf(persongrunnlagService.hentSøker(behandlingResultat.behandling))
        } else {
            persongrunnlagService.hentBarna(behandlingResultat.behandling)
        }
        return behandlingResultat?.personResultater?.find {personResultat ->
            personer.map { it?.personIdent?.ident }.contains(personResultat.personIdent) && personResultat.vilkårResultater.find { vilkårResusltat ->
                vilkårResusltat.vilkårType == vilkår && vilkårResusltat.resultat == Resultat.NEI
            } != null
        } != null
    }

    internal fun hentBegrunnelseFraVilkårsvurdering(behandlingId: Long): String? {
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandlingId)
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val søker = persongrunnlagService.hentSøker(behandling)
        val søkerResultat = behandlingResultat?.personResultater?.find { it.personIdent == søker?.personIdent?.ident }

        val bosattIRiketResultat = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        if (bosattIRiketResultat?.resultat == Resultat.NEI) {
            return "Mor er ikke bosatt i riket."
        }

        val harLovligOpphold = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
        if (harLovligOpphold?.resultat == Resultat.NEI) {
            return harLovligOpphold.begrunnelse
        }

        persongrunnlagService.hentBarna(behandling).forEach { barn ->
            val vilkårsresultat =
                    behandlingResultat?.personResultater?.find { it.personIdent == barn.personIdent.ident }?.vilkårResultater

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }?.resultat == Resultat.NEI) {
                return "Barnet (fødselsdato: ${barn.fødselsdato}) er over 18 år."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.BOR_MED_SØKER }?.resultat == Resultat.NEI) {
                return "Barnet (fødselsdato: ${barn.fødselsdato}) er ikke bosatt med mor."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }?.resultat == Resultat.NEI) {
                return "Barnet (fødselsdato: ${barn.fødselsdato}) er gift."
            }

            if (vilkårsresultat?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat == Resultat.NEI) {
                return "Barnet (fødselsdato: ${barn.fødselsdato}) er ikke bosatt i riket."
            }
        }

        return null
    }

    internal fun hentBegrunnelseFraFiltreringsregler(evaluering: Evaluering): String? {

        Filtreringsregler.values().forEach { filteringRegel ->

            val regelEvaluering = evaluering.children.find {
                it.identifikator == filteringRegel.spesifikasjon.identifikator
            }

            if (regelEvaluering?.resultat == Resultat.NEI) {
                return regelEvaluering.begrunnelse
            }
        }

        return null
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

    private fun økTellereForStansetIAutomatiskVilkårsvurdering(behandling: Behandling) {
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandling.id)!!

        val orderedVilkårList = listOf(
                //Mor bosatt i riket
                Pair(PersonType.SØKER, Vilkår.BOSATT_I_RIKET),
                //Mor har lovlig opphold
                Pair(PersonType.SØKER, Vilkår.LOVLIG_OPPHOLD),
                //Barnet er under 18 år
                Pair(PersonType.BARN, Vilkår.UNDER_18_ÅR),
                //Barnet bor med søker
                Pair(PersonType.BARN, Vilkår.BOR_MED_SØKER),
                //Barnet er ugift og har ikke inngått partnerskap
                Pair(PersonType.BARN, Vilkår.GIFT_PARTNERSKAP),
                //Barnet er bosatt i riket
                Pair(PersonType.BARN, Vilkår.BOSATT_I_RIKET),
                //Barnet har lovlig opphold
                Pair(PersonType.BARN, Vilkår.LOVLIG_OPPHOLD),
        )

        orderedVilkårList.find { erVilkårUtfall(behandlingResultat, it.first, it.second) }?.let {
            vilkårsvurderingMetrics.økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(it.second, it.first)
        }

        stansetIAutomatiskVilkårsvurderingCounter.increment()
    }

    private fun fødselshendelseSkalRullesTilbake(): Boolean =
            featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")
}