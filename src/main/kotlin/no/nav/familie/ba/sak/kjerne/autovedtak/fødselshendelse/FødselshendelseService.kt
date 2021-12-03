package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val autovedtakService: AutovedtakService,
    private val personopplysningerService: PersonopplysningerService
) {

    val stansetIAutomatiskFiltreringCounter =
        Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "filtrering")
    val stansetIAutomatiskVilkårsvurderingCounter =
        Metrics.counter("familie.ba.sak.henvendelse.stanset", "steg", "vilkaarsvurdering")
    val passertFiltreringOgVilkårsvurderingCounter = Metrics.counter("familie.ba.sak.henvendelse.passert")

    fun behandleFødselshendelse(nyBehandling: NyBehandlingHendelse) {
        val morsÅpneBehandling = hentÅpenBehandling(ident = nyBehandling.morsIdent)
        if (morsÅpneBehandling != null) {
            val barnaPåÅpenBehandling =
                persongrunnlagService.hentBarna(behandling = morsÅpneBehandling).map { it.personIdent.ident }

            if (barnPåHendelseBlirAlleredeBehandletIÅpenBehandling(
                    barnaPåHendelse = nyBehandling.barnasIdenter,
                    barnaPåÅpenBehandling = barnaPåÅpenBehandling
                )
            ) {
                logger.info("Ignorerer fødselshendelse fordi åpen behandling inneholder alle barna i hendelsen.")
                secureLogger.info(
                    "Ignorerer fødselshendelse fordi åpen behandling inneholder alle barna i hendelsen." +
                        "Barn på hendelse=${nyBehandling.barnasIdenter}, barn på åpen behandling=$barnaPåÅpenBehandling"
                )
                return
            }

            autovedtakService.opprettOppgaveForManuellBehandling(
                behandling = morsÅpneBehandling,
                begrunnelse = "Fødselshendelse: Bruker har åpen behandling",
                oppgavetype = Oppgavetype.VurderLivshendelse
            )
            return
        }

        val (barnSomSkalBehandlesForMor, alleBarnSomKanBehandles) = finnBarnSomSkalBehandlesForMor(
            fagsak = fagsakService.hent(personIdent = PersonIdent(nyBehandling.morsIdent)),
            nyBehandlingHendelse = nyBehandling
        )

        if (barnSomSkalBehandlesForMor.isEmpty()) {
            logger.info("Ignorere fødselshendelse, alle barna fra hendelse er allerede behandlet")
            secureLogger.info(
                "Ignorere fødselshendelse, alle barna fra hendelse er allerede behandlet. " +
                    "Alle barna som kan behandles=$alleBarnSomKanBehandles, "
            )
            return
        }

        val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(
            nyBehandling.copy(
                barnasIdenter = barnSomSkalBehandlesForMor
            )
        )

        logger.info("Behandler fødselshendelse på behandling $behandling")

        val behandlingEtterFiltrering =
            stegService.håndterFiltreringsreglerForFødselshendelser(behandling, nyBehandling)

        if (behandlingEtterFiltrering.steg == StegType.HENLEGG_BEHANDLING) {
            henleggBehandlingOgOpprettManuellOppgave(
                behandling = behandlingEtterFiltrering,
                begrunnelse = filtreringsreglerService.hentFødselshendelsefiltreringResultater(behandlingId = behandling.id)
                    .first { it.resultat == Resultat.IKKE_OPPFYLT }.begrunnelse,
            )

            stansetIAutomatiskFiltreringCounter.increment()
        } else vurderVilkår(behandling = behandlingEtterFiltrering, barnaSomVurderes = barnSomSkalBehandlesForMor)
    }

    private fun vurderVilkår(behandling: Behandling, barnaSomVurderes: List<String>) {
        val behandlingEtterVilkårsvurdering = stegService.håndterVilkårsvurdering(behandling = behandling)

        if (behandlingEtterVilkårsvurdering.resultat == BehandlingResultat.INNVILGET) {
            val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)
            vedtaksperiodeService.oppdaterVedtaksperioderForBarnVurdertIFødselshendelse(vedtak, barnaSomVurderes)

            val vedtakEtterToTrinn =
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling = behandlingEtterVilkårsvurdering)

            val task = IverksettMotOppdragTask.opprettTask(
                behandling,
                vedtakEtterToTrinn,
                SikkerhetContext.hentSaksbehandler()
            )
            taskRepository.save(task)

            passertFiltreringOgVilkårsvurderingCounter.increment()
        } else {
            henleggBehandlingOgOpprettManuellOppgave(behandling = behandlingEtterVilkårsvurdering)

            stansetIAutomatiskVilkårsvurderingCounter
        }
    }

    private fun hentÅpenBehandling(ident: String): Behandling? {
        return fagsakService.hent(PersonIdent(ident))?.let {
            behandlingService.hentAktivOgÅpenForFagsak(it.id)
        }
    }

    private fun finnBarnSomSkalBehandlesForMor(
        fagsak: Fagsak?,
        nyBehandlingHendelse: NyBehandlingHendelse
    ): Pair<List<String>, List<String>> {
        val barnaTilMor = personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(
            personIdent = nyBehandlingHendelse.morsIdent
        ).forelderBarnRelasjon.filter { it.relasjonsrolle == FORELDERBARNRELASJONROLLE.BARN }

        val barnaSomHarBlittBehandlet =
            if (fagsak != null) behandlingService.hentBehandlinger(fagsakId = fagsak.id).flatMap {
                persongrunnlagService.hentBarna(behandling = it).map { barn -> barn.personIdent.ident }
            }.distinct() else emptyList()

        return finnBarnSomSkalBehandlesForMor(
            nyBehandlingHendelse = nyBehandlingHendelse,
            barnaTilMor = barnaTilMor,
            barnaSomHarBlittBehandlet = barnaSomHarBlittBehandlet,
            secureLogger = secureLogger
        )
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

        logger.info("Henlegger behandling $behandling automatisk på grunn av ugyldig resultat")

        stegService.håndterHenleggBehandling(
            behandling = behandling,
            henleggBehandlingInfo = RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.FØDSELSHENDELSE_UGYLDIG_UTFALL,
                begrunnelse = begrunnelseForManuellOppgave
            )
        )

        autovedtakService.opprettOppgaveForManuellBehandling(
            behandling = behandling,
            begrunnelse = "Fødselshendelse: $begrunnelseForManuellOppgave",
            oppgavetype = Oppgavetype.VurderLivshendelse
        )
    }

    private fun hentBegrunnelseFraVilkårsvurdering(behandlingId: Long): String {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
        val behandling = behandlingService.hent(behandlingId)
        val søker = persongrunnlagService.hentSøker(behandling.id)
        val søkerResultat = vilkårsvurdering?.personResultater?.find { it.personIdent == søker?.personIdent?.ident }

        val bosattIRiketResultat = søkerResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        if (bosattIRiketResultat?.resultat == Resultat.IKKE_OPPFYLT && bosattIRiketResultat.evalueringÅrsaker.any {
                VilkårIkkeOppfyltÅrsak.valueOf(
                    it
                ) == VilkårIkkeOppfyltÅrsak.BOR_IKKE_I_RIKET_FLERE_ADRESSER_UTEN_FOM
            }
        ) {
            return "Mor har flere bostedsadresser uten fra- og med dato"
        } else if (bosattIRiketResultat?.resultat == Resultat.IKKE_OPPFYLT) {
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

        logger.error("Fant ikke begrunnelse for at fødselshendelse ikke kunne automatisk behandles.")

        return ""
    }

    companion object {

        private val logger = LoggerFactory.getLogger(BehandleFødselshendelseTask::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
