package no.nav.familie.ba.sak.kjerne.fødselshendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.erInnenfor
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.AutomatiskVilkårsVurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.OppfyllerVilkår
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.Filtreringsregler
import no.nav.familie.ba.sak.kjerne.fødselshendelse.gdpr.GDPRService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand.Companion.sisteSivilstand
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.KontrollertRollbackException
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class FødselshendelseService(private val infotrygdFeedService: InfotrygdFeedService,
                             private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                             private val stegService: StegService,
                             private val vedtakService: VedtakService,
                             private val evaluerFiltreringsreglerForFødselshendelse: EvaluerFiltreringsreglerForFødselshendelse,
                             private val taskRepository: TaskRepository,
                             private val personopplysningerService: PersonopplysningerService,
                             private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                             private val persongrunnlagService: PersongrunnlagService,
                             private val behandlingRepository: BehandlingRepository,
                             private val gdprService: GDPRService,
                             private val envService: EnvService) {

    val harLøpendeSakIInfotrygdCounter: Counter = Metrics.counter("foedselshendelse.mor.eller.barn.finnes.loepende.i.infotrygd")
    val harIkkeLøpendeSakIInfotrygdCounter: Counter =
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

        return if (infotrygdBarnetrygdClient.harLøpendeSakIInfotrygd(morsIdenter, alleBarnasIdenter)) {
            harLøpendeSakIInfotrygdCounter.increment()
            true
        } else {
            harIkkeLøpendeSakIInfotrygdCounter.increment()
            false
        }
    }

    fun sendTilInfotrygdFeed(barnIdenter: List<String>) {
        infotrygdFeedService.sendTilInfotrygdFeed(barnIdenter)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettBehandlingOgKjørReglerForFødselshendelse(nyBehandling: NyBehandlingHendelse) {
        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
        val (faktaForFiltreringsregler, evalueringAvFiltrering) =
                evaluerFiltreringsreglerForFødselshendelse.evaluerFiltreringsregler(behandling,
                                                                                    nyBehandling.barnasIdenter.toSet())

        gdprService.lagreResultatAvFiltreringsregler(faktaForFiltreringsregler = faktaForFiltreringsregler,
                                                     evalueringAvFiltrering = evalueringAvFiltrering,
                                                     nyBehandling = nyBehandling,
                                                     behandlingId = behandling.id)

        val resultatAvVilkårsvurdering: BehandlingResultat? =
                if (evalueringAvFiltrering.resultat == Resultat.OPPFYLT)
                    stegService.evaluerVilkårForFødselshendelse(behandling, personopplysningGrunnlag)
                else
                    null

        when (resultatAvVilkårsvurdering) {
            null -> stansetIAutomatiskFiltreringCounter.increment()
            BehandlingResultat.INNVILGET -> passertFiltreringOgVilkårsvurderingCounter.increment()
            else -> stansetIAutomatiskVilkårsvurderingCounter.increment()
        }

        if (envService.skalIverksetteBehandling()) {
            if (evalueringAvFiltrering.resultat !== Resultat.OPPFYLT ||
                resultatAvVilkårsvurdering !== BehandlingResultat.INNVILGET) {
                val beskrivelse = when (resultatAvVilkårsvurdering) {
                    null -> hentBegrunnelseFraFiltreringsregler(evalueringAvFiltrering)
                    else -> hentBegrunnelseFraVilkårsvurdering(behandling.id)
                }

                opprettOppgaveForManuellBehandling(behandlingId = behandling.id, beskrivelse = beskrivelse)

            } else {
                iverksett(behandling)
            }
        } else {
            throw KontrollertRollbackException(gdprService.hentFødselshendelsePreLansering(behandlingId = behandling.id))
        }
    }

    //sommerteam har laget for å vurdere om saken kan behandles automatisk
    fun fødselshendelseBehandlesHosBA(nyBehandling: NyBehandlingHendelse): Boolean {
        return evaluerFiltreringsreglerForFødselshendelse.filtreringsfaktaEvaluering(nyBehandling.morsIdent,
                                                                                     nyBehandling.barnasIdenter.toSet())
    }

    //sommmerteam har laget for å vurdere saken automatisk basert på vilkår.
    fun vilkårsVurdering(behandling: Behandling): AutomatiskVilkårsVurdering {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return AutomatiskVilkårsVurdering(false)

        //mor bosatt i riket
        val mor = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna
        val morsSisteBosted = if (mor.bostedsadresser.isEmpty()) null else mor.bostedsadresser.sisteAdresse()
        if (morsSisteBosted == null || morsSisteBosted.periode?.erInnenfor(LocalDate.now()) == true) {
            println("$morsSisteBosted og  ${morsSisteBosted?.periode}")
            return AutomatiskVilkårsVurdering(false, OppfyllerVilkår.NEI)
        }
        //Sommerteam hopper over sjekk om mor og barn har lovlig opphold

        if (barna.any { it.fødselsdato.plusYears(18).isBefore(LocalDate.now()) }) {
            println(barna.fold("") { acc, personInfo -> acc + personInfo + " og " + personInfo.fødselsdato + ", " })
            return AutomatiskVilkårsVurdering(false, OppfyllerVilkår.JA, OppfyllerVilkår.NEI)
        }
        if (barna.any { !GrBostedsadresse.erSammeAdresse(it.bostedsadresser.sisteAdresse(), morsSisteBosted) }) {
            println("mor adresse type: ${mor.bostedsadresser.sisteAdresse()}")
            barna.forEach {
                println("" + mor.bostedsadresser.sisteAdresse() + " og " + it.bostedsadresser.sisteAdresse())
            }
            return AutomatiskVilkårsVurdering(false, OppfyllerVilkår.JA, OppfyllerVilkår.JA, OppfyllerVilkår.NEI)
        }
        if (barna.any {
                    !(it.sivilstander.sisteSivilstand()?.type != SIVILSTAND.UGIFT ||
                      it.sivilstander.sisteSivilstand()?.type != SIVILSTAND.UOPPGITT)
                }) {
            println(barna.fold("") { acc, personInfo -> acc + personInfo.sivilstander.sisteSivilstand() + ", " })

            return AutomatiskVilkårsVurdering(false,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.NEI)
        }
        if (barna.any {
                    it.bostedsadresser.isEmpty() ||
                    it.bostedsadresser.sisteAdresse()?.periode?.erInnenfor(LocalDate.now()) == true
                }) {
            println(barna.fold("") { acc, personInfo -> acc + personInfo.bostedsadresser.last() + ", " })

            return AutomatiskVilkårsVurdering(false,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.JA,
                                              OppfyllerVilkår.NEI)
        }

        return AutomatiskVilkårsVurdering(true,
                                          OppfyllerVilkår.JA,
                                          OppfyllerVilkår.JA,
                                          OppfyllerVilkår.JA,
                                          OppfyllerVilkår.JA,
                                          OppfyllerVilkår.JA)
    }


    fun opprettBehandlingForAutomatisertVilkårsVurdering(nyBehandling: NyBehandlingHendelse): Behandling {
        return stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
    }

    internal fun hentBegrunnelseFraVilkårsvurdering(behandlingId: Long): String? {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val søker = persongrunnlagService.hentSøker(behandling.id)
        val søkerResultat = vilkårsvurdering?.personResultater?.find { it.personIdent == søker?.personIdent?.ident }

        val bosattIRiketResultat = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        if (bosattIRiketResultat?.resultat == Resultat.IKKE_OPPFYLT) {
            return "Mor er ikke bosatt i riket."
        }

        val harLovligOpphold = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
        if (harLovligOpphold?.resultat == Resultat.IKKE_OPPFYLT) {
            return harLovligOpphold.begrunnelse
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

    internal fun hentBegrunnelseFraFiltreringsregler(evaluering: Evaluering): String? {

        Filtreringsregler.values().forEach { filteringRegel ->

            val regelEvaluering = evaluering.children.find {
                it.identifikator == filteringRegel.spesifikasjon.identifikator
            }

            if (regelEvaluering?.resultat == Resultat.IKKE_OPPFYLT) {
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

    private fun iverksett(behandling: Behandling) {
        val vedtak = vedtakService.opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(behandling)

        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }
}