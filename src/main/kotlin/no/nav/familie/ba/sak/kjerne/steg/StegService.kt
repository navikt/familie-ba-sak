package no.nav.familie.ba.sak.kjerne.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.prosessering.error.RekjørSenereException
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Properties

@Service
class StegService(
    private val steg: List<BehandlingSteg<*>>,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val skyggesakService: SkyggesakService,
    private val tilgangService: TilgangService,
    val infotrygdFeedService: InfotrygdFeedService,
) {

    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")
    private val stegFunksjonellFeilMetrics: Map<StegType, Counter> = initStegMetrikker("funksjonell-feil")

    @Transactional
    fun håndterNyBehandlingOgSendInfotrygdFeed(nyBehandling: NyBehandling): Behandling {
        val behandling = håndterNyBehandling(nyBehandling)
        if (behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING) {
            infotrygdFeedService.sendStartBehandlingTilInfotrygdFeed(
                behandling.fagsak.aktør,
            )
        }
        return behandling
    }

    @Transactional
    fun håndterNyBehandling(nyBehandling: NyBehandling): Behandling {
        val behandling = behandlingService.opprettBehandling(nyBehandling)
        val barnasIdenter: List<String> = when {
            nyBehandling.behandlingÅrsak in listOf(BehandlingÅrsak.MIGRERING, BehandlingÅrsak.FØDSELSHENDELSE) -> {
                nyBehandling.barnasIdenter
            }
            nyBehandling.behandlingÅrsak == BehandlingÅrsak.HELMANUELL_MIGRERING -> {
                if (behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) != null) {
                    throw FunksjonellFeil(
                        melding = "Det finnes allerede en vedtatt behandling på fagsak ${behandling.fagsak.id}." +
                            "Behandling kan ikke opprettes med årsak " +
                            BehandlingÅrsak.HELMANUELL_MIGRERING.visningsnavn,
                        frontendFeilmelding = "Det finnes allerede en vedtatt behandling på fagsak." +
                            "Behandling kan ikke opprettes med årsak " +
                            BehandlingÅrsak.HELMANUELL_MIGRERING.visningsnavn
                    )
                }
                nyBehandling.barnasIdenter
            }
            nyBehandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING -> {
                emptyList()
            }
            nyBehandling.behandlingType in listOf(
                BehandlingType.REVURDERING,
                BehandlingType.TEKNISK_ENDRING
            ) || nyBehandling.behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD &&
                nyBehandling.behandlingÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
                val sisteBehandling = hentSisteAvsluttetBehandling(behandling)
                behandlingService.finnBarnFraBehandlingMedTilkjentYtsele(sisteBehandling.id)
                    .map { it.aktivFødselsnummer() }
            }
            else -> throw Feil("Ukjent oppførsel ved opprettelse av behandling.")
        }

        return håndterPersongrunnlag(
            behandling,
            RegistrerPersongrunnlagDTO(
                ident = nyBehandling.søkersIdent,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = nyBehandling.nyMigreringsdato
            )
        )
    }

    @Transactional
    fun opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandlingHendelse: NyBehandlingHendelse): Behandling {
        val fagsak = try {
            fagsakService.hentEllerOpprettFagsakForPersonIdent(nyBehandlingHendelse.morsIdent, true)
        } catch (exception: Exception) {
            if (exception is ConstraintViolationException) {
                throw RekjørSenereException(
                    triggerTid = LocalDateTime.now().plusMinutes(15),
                    årsak = "Klarte ikke å opprette fagsak på grunn av krasj i databasen, prøver igjen om 15 minutter. Feilmelding: ${exception.message}."
                )
            }

            throw exception
        }

        // Denne vil sende selv om det allerede eksisterer en fagsak. Vi tenker det er greit. Ellers så blir det vanskelig å
        // filtere bort for fødselshendelser. Når vi slutter å filtere bort fødselshendelser, så kan vi flytte den tilbake til
        // hentEllerOpprettFagsak
        skyggesakService.opprettSkyggesak(fagsak.aktør, fagsak.id)

        return håndterNyBehandlingOgSendInfotrygdFeed(
            NyBehandling(
                søkersIdent = nyBehandlingHendelse.morsIdent,
                behandlingType = if (fagsak.status == FagsakStatus.LØPENDE)
                    BehandlingType.REVURDERING
                else BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                skalBehandlesAutomatisk = true,
                barnasIdenter = nyBehandlingHendelse.barnasIdenter
            )
        )
    }

    @Transactional
    fun håndterSøknad(
        behandling: Behandling,
        restRegistrerSøknad: RestRegistrerSøknad
    ): Behandling =
        fullførSøknadsHåndtering(behandling = behandling, restRegistrerSøknad = restRegistrerSøknad)

    private fun fullførSøknadsHåndtering(behandling: Behandling, restRegistrerSøknad: RestRegistrerSøknad): Behandling {
        val behandlingSteg: RegistrereSøknad = hentBehandlingSteg(StegType.REGISTRERE_SØKNAD) as RegistrereSøknad
        val søknadDTO = restRegistrerSøknad.søknad

        val aktivSøknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        val innsendtSøknad = søknadDTO.writeValueAsString()

        if (aktivSøknadGrunnlag != null && innsendtSøknad == aktivSøknadGrunnlag.søknad) {
            return behandling
        }

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, restRegistrerSøknad)
        }
    }

    @Transactional
    fun håndterPersongrunnlag(
        behandling: Behandling,
        registrerPersongrunnlagDTO: RegistrerPersongrunnlagDTO
    ): Behandling {
        val behandlingSteg: RegistrerPersongrunnlag =
            hentBehandlingSteg(StegType.REGISTRERE_PERSONGRUNNLAG) as RegistrerPersongrunnlag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, registrerPersongrunnlagDTO)
        }
    }

    @Transactional
    fun håndterFiltreringsreglerForFødselshendelser(
        behandling: Behandling,
        nyBehandling: NyBehandlingHendelse
    ): Behandling {
        val behandlingSteg: FiltreringFødselshendelserSteg =
            hentBehandlingSteg(StegType.FILTRERING_FØDSELSHENDELSER) as FiltreringFødselshendelserSteg

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, nyBehandling)
        }
    }

    @Transactional
    fun håndterVilkårsvurdering(behandling: Behandling): Behandling {
        val behandlingSteg: VilkårsvurderingSteg =
            hentBehandlingSteg(StegType.VILKÅRSVURDERING) as VilkårsvurderingSteg

        val behandlingEtterVilkårsvurdering = håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, "")
        }

        return if (behandlingEtterVilkårsvurdering.skalBehandlesAutomatisk) {
            håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        } else behandlingEtterVilkårsvurdering
    }

    @Transactional
    fun håndterBehandlingsresultat(behandling: Behandling): Behandling {
        val behandlingSteg: BehandlingsresultatSteg =
            hentBehandlingSteg(StegType.BEHANDLINGSRESULTAT) as BehandlingsresultatSteg

        val behandlingEtterBehandlingsresultatSteg = håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, "")
        }

        return if (behandlingEtterBehandlingsresultatSteg.resultat == BehandlingResultat.AVSLÅTT &&
            !behandlingEtterBehandlingsresultatSteg.skalBehandlesAutomatisk
        ) {
            håndterVurderTilbakekreving(
                behandling = behandlingEtterBehandlingsresultatSteg
            )
        } else behandlingEtterBehandlingsresultatSteg
    }

    @Transactional
    fun håndterVurderTilbakekreving(
        behandling: Behandling,
        restTilbakekreving: RestTilbakekreving? = null
    ): Behandling {
        val behandlingSteg: VurderTilbakekrevingSteg =
            hentBehandlingSteg(StegType.VURDER_TILBAKEKREVING) as VurderTilbakekrevingSteg

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, restTilbakekreving)
        }
    }

    @Transactional
    fun håndterSendTilBeslutter(behandling: Behandling, behandlendeEnhet: String): Behandling {
        val behandlingSteg: SendTilBeslutter = hentBehandlingSteg(StegType.SEND_TIL_BESLUTTER) as SendTilBeslutter

        val behandlingEtterBeslutterSteg = håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, behandlendeEnhet)
        }
        if (behandlingEtterBeslutterSteg.erManuellMigrering()) {
            return håndterBeslutningForVedtak(behandlingEtterBeslutterSteg, RestBeslutningPåVedtak(Beslutning.GODKJENT))
        }
        return behandlingEtterBeslutterSteg
    }

    @Transactional
    fun håndterBeslutningForVedtak(behandling: Behandling, restBeslutningPåVedtak: RestBeslutningPåVedtak): Behandling {
        val behandlingSteg: BeslutteVedtak =
            hentBehandlingSteg(StegType.BESLUTTE_VEDTAK) as BeslutteVedtak

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak)
        }
    }

    @Transactional
    fun håndterHenleggBehandling(behandling: Behandling, henleggBehandlingInfo: RestHenleggBehandlingInfo): Behandling {
        val behandlingSteg: HenleggBehandling =
            hentBehandlingSteg(StegType.HENLEGG_BEHANDLING) as HenleggBehandling

        val behandlingEtterHenleggeSteg = håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, henleggBehandlingInfo)
        }

        return håndterFerdigstillBehandling(
            behandling = behandlingEtterHenleggeSteg
        )
    }

    @Transactional
    fun håndterIverksettMotØkonomi(behandling: Behandling, iverksettingTaskDTO: IverksettingTaskDTO): Behandling {
        val behandlingSteg: IverksettMotOppdrag =
            hentBehandlingSteg(StegType.IVERKSETT_MOT_OPPDRAG) as IverksettMotOppdrag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, iverksettingTaskDTO)
        }
    }

    @Transactional
    fun håndterStatusFraØkonomi(behandling: Behandling, statusFraOppdragMedTask: StatusFraOppdragMedTask): Behandling {
        val behandlingSteg: StatusFraOppdrag =
            hentBehandlingSteg(StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) as StatusFraOppdrag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, statusFraOppdragMedTask)
        }
    }

    @Transactional
    fun håndterIverksettMotFamilieTilbake(behandling: Behandling, metadata: Properties): Behandling {
        val behandlingSteg: IverksettMotFamilieTilbake =
            hentBehandlingSteg(StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) as IverksettMotFamilieTilbake

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, IverksettMotFamilieTilbakeData(metadata))
        }
    }

    @Transactional
    fun håndterJournalførVedtaksbrev(
        behandling: Behandling,
        journalførVedtaksbrevDTO: JournalførVedtaksbrevDTO
    ): Behandling {
        val behandlingSteg: JournalførVedtaksbrev =
            hentBehandlingSteg(StegType.JOURNALFØR_VEDTAKSBREV) as JournalførVedtaksbrev

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, journalførVedtaksbrevDTO)
        }
    }

    @Transactional
    fun håndterDistribuerVedtaksbrev(behandling: Behandling, distribuerDokumentDTO: DistribuerDokumentDTO): Behandling {
        val behandlingSteg: DistribuerVedtaksbrev =
            hentBehandlingSteg(StegType.DISTRIBUER_VEDTAKSBREV) as DistribuerVedtaksbrev

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, distribuerDokumentDTO)
        }
    }

    @Transactional
    fun håndterFerdigstillBehandling(behandling: Behandling): Behandling {
        val behandlingSteg: FerdigstillBehandling =
            hentBehandlingSteg(StegType.FERDIGSTILLE_BEHANDLING) as FerdigstillBehandling

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, "")
        }
    }

    // Generelle stegmetoder
    private fun håndterSteg(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg<*>,
        utførendeSteg: () -> StegType
    ): Behandling {
        try {
            logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} håndterer ${behandlingSteg.stegType()} på behandling ${behandling.id}")
            tilgangService.validerTilgangTilBehandling(behandlingId = behandling.id)
            tilgangService.verifiserHarTilgangTilHandling(
                minimumBehandlerRolle = behandlingSteg.stegType().tillattFor.minByOrNull { it.nivå }
                    ?: throw Feil("${SikkerhetContext.hentSaksbehandlerNavn()} prøver å utføre steg ${behandlingSteg.stegType()} som ikke er tillatt av noen."),
                handling = "utføre steg ${behandlingSteg.stegType().displayName()}"
            )

            if (behandling.steg == SISTE_STEG) {
                error("Behandling med id ${behandling.id} er avsluttet og stegprosessen kan ikke gjenåpnes")
            }

            if (behandlingSteg.stegType().erSaksbehandlerSteg() && behandlingSteg.stegType()
                .kommerEtter(behandling.steg)
            ) {
                error(
                    "${SikkerhetContext.hentSaksbehandlerNavn()} prøver å utføre steg '${
                    behandlingSteg.stegType()
                        .displayName()
                    }', men behandlingen er på steg '${behandling.steg.displayName()}'"
                )
            }

            // TODO: Det bør sees på en ytterligere robustgjøring for alle steg som SB kan utføre.
            if (behandling.steg == StegType.BESLUTTE_VEDTAK && behandlingSteg.stegType() != StegType.BESLUTTE_VEDTAK) {
                error("Behandlingen er på steg '${behandling.steg.displayName()}', og er da låst for alle andre type endringer.")
            }

            behandlingSteg.preValiderSteg(behandling, this)
            val nesteSteg = utførendeSteg()
            behandlingSteg.postValiderSteg(behandling)
            val behandlingEtterUtførtSteg = behandlingService.hent(behandling.id)

            stegSuksessMetrics[behandlingSteg.stegType()]?.increment()

            if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingEtterUtførtSteg.status)) {
                error("Steg '${nesteSteg.displayName()}' kan ikke settes på behandling i kombinasjon med status ${behandlingEtterUtførtSteg.status}")
            }

            val returBehandling =
                behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
                    behandlingId = behandling.id,
                    steg = nesteSteg
                )

            if (nesteSteg == SISTE_STEG) {
                logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} er ferdig med stegprosess på behandling $behandling")
            } else {
                logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} har håndtert ${behandlingSteg.stegType()} på behandling $behandling. Neste steg er $nesteSteg")
            }
            return returBehandling
        } catch (exception: Exception) {
            when (exception) {
                is RekjørSenereException -> {
                    stegFunksjonellFeilMetrics[behandlingSteg.stegType()]?.increment()
                    logger.info("Steg '${behandlingSteg.stegType()}' har trigget rekjøring senere på behandling $behandling. Årsak: ${exception.årsak}")
                }
                is FunksjonellFeil -> {
                    stegFunksjonellFeilMetrics[behandlingSteg.stegType()]?.increment()
                    logger.info("Håndtering av stegtype '${behandlingSteg.stegType()}' feilet på grunn av funksjonell feil på behandling $behandling. Melding: ${exception.melding}")
                }
                else -> {
                    stegFeiletMetrics[behandlingSteg.stegType()]?.increment()
                    logger.error("Håndtering av stegtype '${behandlingSteg.stegType()}' feilet på behandling $behandling.")
                    secureLogger.error(
                        "Håndtering av stegtype '${behandlingSteg.stegType()}' feilet på behandling $behandling.",
                        exception
                    )
                }
            }

            throw exception
        }
    }

    fun hentBehandlingSteg(stegType: StegType): BehandlingSteg<*>? {
        return steg.firstOrNull { it.stegType() == stegType }
    }

    private fun hentSisteAvsluttetBehandling(behandling: Behandling): Behandling {
        return behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
            ?: behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
            ?: throw Feil(
                "Forsøker å opprette en ${behandling.type.visningsnavn} " +
                    "med årsak ${behandling.opprettetÅrsak.visningsnavn}, " +
                    "men kan ikke finne tidligere behandling på fagsak ${behandling.fagsak.id}"
            )
    }

    private fun initStegMetrikker(type: String): Map<StegType, Counter> {
        return steg.associate {
            it.stegType() to Metrics.counter(
                "behandling.steg.$type",
                "steg",
                it.stegType().name,
                "beskrivelse",
                it.stegType().rekkefølge.toString() + " " + it.stegType().displayName()
            )
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StegService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
