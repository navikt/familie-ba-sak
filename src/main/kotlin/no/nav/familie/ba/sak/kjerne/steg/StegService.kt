package no.nav.familie.ba.sak.kjerne.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagSystemÅrsak
import no.nav.familie.ba.sak.common.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RegistrerSøknadDto
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.SatsendringService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggBehandlingInfoDto
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.kontrakter.felles.søknad.MissingVersionException
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
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val beregningService: BeregningService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val tilgangService: TilgangService,
    private val infotrygdFeedService: InfotrygdFeedService,
    private val satsendringService: SatsendringService,
    private val personopplysningerService: PersonopplysningerService,
    private val automatiskBeslutningService: AutomatiskBeslutningService,
    private val opprettTaskService: OpprettTaskService,
    private val satskjøringRepository: SatskjøringRepository,
    private val featureToggleService: FeatureToggleService,
    private val automatiskRegistrerSøknadService: AutomatiskRegistrerSøknadService,
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
        when (nyBehandling.behandlingÅrsak) {
            BehandlingÅrsak.HELMANUELL_MIGRERING -> validerHelmanuelMigrering(nyBehandling)
            BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> validerEndreMigreringsdato(nyBehandling)
            BehandlingÅrsak.IVERKSETTE_KA_VEDTAK -> validerIverksettKAVedtak()
            else -> Unit
        }

        val behandling = behandlingService.opprettBehandling(nyBehandling)

        // Aktiverer minside for fagsak-aktør
        opprettTaskService.opprettAktiverMinsideTask(behandling.fagsak.aktør)

        val barnasIdenter =
            when {
                behandling.fagsak.type == FagsakType.SKJERMET_BARN -> {
                    listOf(behandling.fagsak.aktør.aktivFødselsnummer())
                }

                nyBehandling.behandlingÅrsak in listOf(BehandlingÅrsak.FØDSELSHENDELSE, BehandlingÅrsak.HELMANUELL_MIGRERING) -> {
                    nyBehandling.barnasIdenter
                }

                nyBehandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    emptyList()
                }

                nyBehandling.behandlingType in listOf(BehandlingType.REVURDERING, BehandlingType.TEKNISK_ENDRING, BehandlingType.MIGRERING_FRA_INFOTRYGD) -> {
                    if (nyBehandling.behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD) {
                        validerMigreringFraInfotrygd(nyBehandling)
                    }
                    hentBarnFraForrigeAvsluttedeBehandling(behandling)
                }

                else -> {
                    throw Feil(hentUkjentBehandlingTypeOgÅrsakFeilMelding(nyBehandling))
                }
            }

        return håndterPersongrunnlag(
            behandling,
            RegistrerPersongrunnlagDTO(
                ident = nyBehandling.søkersIdent,
                barnasIdenter = barnasIdenter,
                nyMigreringsdato = nyBehandling.nyMigreringsdato,
            ),
        )
    }

    private fun validerMigreringFraInfotrygd(nyBehandling: NyBehandling) {
        if (nyBehandling.behandlingÅrsak != BehandlingÅrsak.ENDRE_MIGRERINGSDATO) {
            throw Feil(hentUkjentBehandlingTypeOgÅrsakFeilMelding(nyBehandling))
        }
    }

    private fun validerEndreMigreringsdato(nyBehandling: NyBehandling) {
        check(nyBehandling.behandlingÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO)

        if (!satsendringService.erFagsakOppdatertMedSisteSatser(fagsakId = nyBehandling.fagsakId)) {
            val satskjøring = satskjøringRepository.findByFagsakIdAndSatsTidspunkt(nyBehandling.fagsakId, StartSatsendring.hentAktivSatsendringstidspunkt())
            if (satskjøring == null) {
                opprettTaskService.opprettSatsendringTask(fagsakId = nyBehandling.fagsakId, satstidspunkt = StartSatsendring.hentAktivSatsendringstidspunkt())
                throw FunksjonellFeil("Fagsaken har ikke siste sats. Det har automatisk blitt opprettet en behandling for satsendring. Vent til den er ferdig behandlet før du endrer migreringsdato.")
            } else if (satskjøring.ferdigTidspunkt == null) {
                throw FunksjonellFeil("Det kjøres satsendring på fagsaken. Vennligst prøv igjen senere")
            }
        }
    }

    private fun validerIverksettKAVedtak() {
        if (!featureToggleService.isEnabled(FeatureToggle.KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK)) {
            throw FunksjonellFeil("Det er ikke mulig å opprette behandling med årsak Iverksette KA-vedtak")
        }
    }

    private fun validerHelmanuelMigrering(nyBehandling: NyBehandling) {
        val sisteBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(nyBehandling.fagsakId)

        if (sisteBehandlingSomErVedtatt != null && behandlingService.erLøpende(sisteBehandlingSomErVedtatt)) {
            throw FunksjonellFeil(
                melding = "Det finnes allerede en vedtatt behandling med løpende utbetalinger på fagsak ${nyBehandling.fagsakId}." + "Behandling kan ikke opprettes med årsak " + BehandlingÅrsak.HELMANUELL_MIGRERING.visningsnavn,
                frontendFeilmelding = "Det finnes allerede en vedtatt behandling med løpende utbetalinger på fagsak." + "Behandling kan ikke opprettes med årsak " + BehandlingÅrsak.HELMANUELL_MIGRERING.visningsnavn,
            )
        }
    }

    private fun hentBarnFraForrigeAvsluttedeBehandling(behandling: Behandling): List<String> {
        val sisteBehandling = hentSisteAvsluttetBehandling(behandling)
        return beregningService.finnBarnFraBehandlingMedTilkjentYtelse(sisteBehandling.id).mapNotNull {
            try {
                personopplysningerService.hentPersoninfoEnkel(it)
                it.aktivFødselsnummer()
            } catch (pdlPersonKanIkkeBehandlesIFagsystem: PdlPersonKanIkkeBehandlesIFagsystem) {
                if (behandling.opprettetÅrsak == BehandlingÅrsak.TEKNISK_ENDRING && pdlPersonKanIkkeBehandlesIFagsystem.årsak == PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO) {
                    logger.warn("Barn fra forrige behandling mangler fødselsdato. Inkluderes alikevel siden det er en teknisk endring")
                    it.aktivFødselsnummer()
                } else {
                    logger.warn("Ignorerer barn fra forrige avsluttede behandling: ${pdlPersonKanIkkeBehandlesIFagsystem.årsak}")
                    secureLogger.warn("Ignorerer barn ${it.aktivFødselsnummer()} fra forrige avsluttede behandling: ${pdlPersonKanIkkeBehandlesIFagsystem.årsak}")
                    null
                }
            }
        }
    }

    @Transactional
    fun opprettNyBehandlingOgRegistrerPersongrunnlagForFødselhendelse(nyBehandlingHendelse: NyBehandlingHendelse): Behandling {
        val fagsak =
            try {
                fagsakService.hentEllerOpprettFagsakForPersonIdent(nyBehandlingHendelse.morsIdent, true, FagsakType.NORMAL)
            } catch (exception: Exception) {
                if (exception is ConstraintViolationException) {
                    throw RekjørSenereException(
                        triggerTid = LocalDateTime.now().plusMinutes(15),
                        årsak = "Klarte ikke å opprette fagsak på grunn av krasj i databasen, prøver igjen om 15 minutter. Feilmelding: ${exception.message}.",
                    )
                }

                throw exception
            }

        return håndterNyBehandlingOgSendInfotrygdFeed(
            NyBehandling(
                søkersIdent = nyBehandlingHendelse.morsIdent,
                behandlingType =
                    if (fagsak.status == FagsakStatus.LØPENDE) {
                        BehandlingType.REVURDERING
                    } else {
                        BehandlingType.FØRSTEGANGSBEHANDLING
                    },
                behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                skalBehandlesAutomatisk = true,
                barnasIdenter = nyBehandlingHendelse.barnasIdenter,
                // alltid NASJONAL for fødselshendelse
                kategori = BehandlingKategori.NASJONAL,
                // alltid ORDINÆR for fødselshendelse
                underkategori = BehandlingUnderkategori.ORDINÆR,
                fagsakId = fagsak.id,
            ),
        )
    }

    @Transactional
    fun håndterSøknad(
        behandling: Behandling,
        registrerSøknadDto: RegistrerSøknadDto,
    ): Behandling = fullførSøknadsHåndtering(behandling = behandling, registrerSøknadDto = registrerSøknadDto)

    @Transactional
    fun håndterAutomatiskRegistrerSøknad(behandling: Behandling): Behandling =
        try {
            val restRegistrerSøknad = automatiskRegistrerSøknadService.lagRestRegistrerSøknad(behandling)
            fullførSøknadsHåndtering(
                behandling = behandling,
                registrerSøknadDto = restRegistrerSøknad,
            )
        } catch (e: Exception) {
            val feilmelding = "Klarte ikke å automatisk registrere søknad for behandling ${behandling.id}. Feil: ${e.message}"
            if (e is MissingVersionException) {
                logger.error(feilmelding, e)
            } else {
                logger.warn(feilmelding, e)
            }
            behandling
        }

    private fun fullførSøknadsHåndtering(
        behandling: Behandling,
        registrerSøknadDto: RegistrerSøknadDto,
    ): Behandling {
        val behandlingSteg: RegistrereSøknad = hentBehandlingSteg(StegType.REGISTRERE_SØKNAD) as RegistrereSøknad
        val søknadDTO = registrerSøknadDto.søknad

        val aktivSøknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        val innsendtSøknad = søknadDTO.writeValueAsString()

        if (aktivSøknadGrunnlag != null && innsendtSøknad == aktivSøknadGrunnlag.søknad) {
            return behandling
        }

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, registrerSøknadDto)
        }
    }

    @Transactional
    fun håndterPersongrunnlag(
        behandling: Behandling,
        registrerPersongrunnlagDTO: RegistrerPersongrunnlagDTO,
    ): Behandling {
        val behandlingSteg: RegistrerPersongrunnlag = hentBehandlingSteg(StegType.REGISTRERE_PERSONGRUNNLAG) as RegistrerPersongrunnlag

        val behandlingEtterRegistrerePersongrunnlag =
            håndterSteg(behandling, behandlingSteg) {
                behandlingSteg.utførStegOgAngiNeste(behandling, registrerPersongrunnlagDTO)
            }

        return if (featureToggleService.isEnabled(FeatureToggle.AUTOMAITSK_REGISTRER_SØKNAD) && behandlingEtterRegistrerePersongrunnlag.steg == StegType.REGISTRERE_SØKNAD) {
            håndterAutomatiskRegistrerSøknad(behandlingEtterRegistrerePersongrunnlag)
        } else {
            behandlingEtterRegistrerePersongrunnlag
        }
    }

    @Transactional
    fun håndterFiltreringsreglerForFødselshendelser(
        behandling: Behandling,
        nyBehandling: NyBehandlingHendelse,
    ): Behandling {
        val behandlingSteg: FiltreringFødselshendelserSteg = hentBehandlingSteg(StegType.FILTRERING_FØDSELSHENDELSER) as FiltreringFødselshendelserSteg

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, nyBehandling)
        }
    }

    @Transactional
    fun håndterVilkårsvurdering(
        behandling: Behandling,
        barnSomSkalVurderesIFødselshendelse: List<String>? = null,
    ): Behandling {
        val behandlingSteg: VilkårsvurderingSteg = hentBehandlingSteg(StegType.VILKÅRSVURDERING) as VilkårsvurderingSteg

        val behandlingEtterVilkårsvurdering =
            håndterSteg(behandling, behandlingSteg) {
                behandlingSteg.utførStegOgAngiNeste(behandling, barnSomSkalVurderesIFødselshendelse)
            }

        return if (behandlingEtterVilkårsvurdering.skalBehandlesAutomatisk) {
            håndterBehandlingsresultat(behandlingEtterVilkårsvurdering)
        } else {
            behandlingEtterVilkårsvurdering
        }
    }

    @Transactional
    fun håndterBehandlingsresultat(behandling: Behandling): Behandling {
        val behandlingSteg: BehandlingsresultatSteg = hentBehandlingSteg(StegType.BEHANDLINGSRESULTAT) as BehandlingsresultatSteg

        val behandlingEtterBehandlingsresultatSteg =
            håndterSteg(behandling, behandlingSteg) {
                behandlingSteg.utførStegOgAngiNeste(behandling, "")
            }

        return if (behandlingEtterBehandlingsresultatSteg.resultat == Behandlingsresultat.AVSLÅTT && !behandlingEtterBehandlingsresultatSteg.skalBehandlesAutomatisk) {
            håndterVurderTilbakekreving(
                behandling = behandlingEtterBehandlingsresultatSteg,
            )
        } else {
            behandlingEtterBehandlingsresultatSteg
        }
    }

    @Transactional
    fun håndterVurderTilbakekreving(
        behandling: Behandling,
        tilbakekrevingDto: TilbakekrevingDto? = null,
    ): Behandling {
        val behandlingSteg: VurderTilbakekrevingSteg = hentBehandlingSteg(StegType.VURDER_TILBAKEKREVING) as VurderTilbakekrevingSteg

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, tilbakekrevingDto)
        }
    }

    @Transactional
    fun håndterSendTilBeslutter(
        behandling: Behandling,
        behandlendeEnhet: String,
    ): Behandling {
        val behandlingSteg: SendTilBeslutter = hentBehandlingSteg(StegType.SEND_TIL_BESLUTTER) as SendTilBeslutter

        val behandlingEtterBeslutterSteg =
            håndterSteg(behandling, behandlingSteg) {
                behandlingSteg.utførStegOgAngiNeste(behandling, behandlendeEnhet)
            }

        if (automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)) {
            return håndterBeslutningForVedtak(
                behandlingEtterBeslutterSteg,
                BeslutningPåVedtakDto(Beslutning.GODKJENT),
            )
        }
        return behandlingEtterBeslutterSteg
    }

    @Transactional
    fun håndterBeslutningForVedtak(
        behandling: Behandling,
        beslutningPåVedtakDto: BeslutningPåVedtakDto,
    ): Behandling {
        val behandlingSteg: BeslutteVedtak = hentBehandlingSteg(StegType.BESLUTTE_VEDTAK) as BeslutteVedtak

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, beslutningPåVedtakDto)
        }
    }

    @Transactional
    fun håndterHenleggBehandling(
        behandling: Behandling,
        henleggBehandlingInfo: HenleggBehandlingInfoDto,
    ): Behandling {
        val behandlingSteg: HenleggBehandling = hentBehandlingSteg(StegType.HENLEGG_BEHANDLING) as HenleggBehandling

        val behandlingEtterHenleggeSteg =
            håndterSteg(
                behandling = behandling,
                behandlingSteg = behandlingSteg,
                henleggÅrsak = henleggBehandlingInfo.årsak,
            ) {
                behandlingSteg.utførStegOgAngiNeste(behandling, henleggBehandlingInfo)
            }

        return håndterFerdigstillBehandling(
            behandling = behandlingEtterHenleggeSteg,
        )
    }

    @Transactional
    fun håndterIverksettMotØkonomi(
        behandling: Behandling,
        iverksettingTaskDTO: IverksettingTaskDTO,
    ): Behandling {
        val behandlingSteg: IverksettMotOppdrag = hentBehandlingSteg(StegType.IVERKSETT_MOT_OPPDRAG) as IverksettMotOppdrag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, iverksettingTaskDTO)
        }
    }

    @Transactional
    fun håndterStatusFraØkonomi(
        behandling: Behandling,
        statusFraOppdragMedTask: StatusFraOppdragMedTask,
    ): Behandling {
        val behandlingSteg: StatusFraOppdrag = hentBehandlingSteg(StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) as StatusFraOppdrag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, statusFraOppdragMedTask)
        }
    }

    @Transactional
    fun håndterIverksettMotFamilieTilbake(
        behandling: Behandling,
        metadata: Properties,
    ): Behandling {
        val behandlingSteg: IverksettMotFamilieTilbake = hentBehandlingSteg(StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) as IverksettMotFamilieTilbake

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, IverksettMotFamilieTilbakeData(metadata))
        }
    }

    @Transactional
    fun håndterJournalførVedtaksbrev(
        behandling: Behandling,
        journalførVedtaksbrevDTO: JournalførVedtaksbrevDTO,
    ): Behandling {
        val behandlingSteg: JournalførVedtaksbrev = hentBehandlingSteg(StegType.JOURNALFØR_VEDTAKSBREV) as JournalførVedtaksbrev

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, journalførVedtaksbrevDTO)
        }
    }

    @Transactional
    fun håndterDistribuerVedtaksbrev(
        behandling: Behandling,
        distribuerDokumentDTO: DistribuerDokumentDTO,
    ): Behandling {
        val behandlingSteg: DistribuerVedtaksbrev = hentBehandlingSteg(StegType.DISTRIBUER_VEDTAKSBREV) as DistribuerVedtaksbrev

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, distribuerDokumentDTO)
        }
    }

    @Transactional
    fun håndterFerdigstillBehandling(behandling: Behandling): Behandling {
        val behandlingSteg: FerdigstillBehandling = hentBehandlingSteg(StegType.FERDIGSTILLE_BEHANDLING) as FerdigstillBehandling

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, "")
        }
    }

    @Transactional
    fun håndterRegistrerInstitusjon(
        behandling: Behandling,
        institusjon: Institusjon,
    ): Behandling {
        val behandlingSteg: RegistrerInstitusjon = hentBehandlingSteg(StegType.REGISTRERE_INSTITUSJON) as RegistrerInstitusjon

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, institusjon)
        }
    }

    // Generelle stegmetoder
    private fun håndterSteg(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg<*>,
        henleggÅrsak: HenleggÅrsak? = null,
        utførendeSteg: () -> StegType,
    ): Behandling {
        try {
            logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} håndterer ${behandlingSteg.stegType()} på behandling ${behandling.id}")
            tilgangService.validerTilgangTilBehandling(
                behandlingId = behandling.id,
                event = AuditLoggerEvent.UPDATE,
            )
            if (behandling.erManuellMigrering() && behandlingSteg.stegType() == StegType.BESLUTTE_VEDTAK) {
                verifiserBeslutteVedtakForManuellMigrering(behandlingSteg)
            } else {
                tilgangService.verifiserHarTilgangTilHandling(
                    minimumBehandlerRolle =
                        behandlingSteg.stegType().tillattFor.minByOrNull { it.nivå } ?: throw Feil(
                            "${SikkerhetContext.hentSaksbehandlerNavn()} prøver " + "å utføre steg ${behandlingSteg.stegType()} som ikke er tillatt av noen.",
                        ),
                    handling = "utføre steg ${behandlingSteg.stegType().displayName()}",
                )
            }

            validerBehandlingIkkeSattPåVent(behandling, behandlingSteg)

            if (behandling.steg == SISTE_STEG) {
                throw FunksjonellFeil("Behandling med id ${behandling.id} er avsluttet og stegprosessen kan ikke gjenåpnes")
            }

            if (behandlingSteg.stegType().erSaksbehandlerSteg() && behandlingSteg.stegType().kommerEtter(behandling.steg)) {
                throw FunksjonellFeil(
                    "${SikkerhetContext.hentSaksbehandlerNavn()} prøver å utføre steg '${
                        behandlingSteg.stegType().displayName()
                    }', men behandlingen er på steg '${behandling.steg.displayName()}'",
                )
            }

            val erTekniskVedlikeholdHenleggelse = behandlingSteg.stegType() == StegType.HENLEGG_BEHANDLING && henleggÅrsak == HenleggÅrsak.TEKNISK_VEDLIKEHOLD
            if (behandling.steg == StegType.BESLUTTE_VEDTAK && behandlingSteg.stegType() != StegType.BESLUTTE_VEDTAK && !erTekniskVedlikeholdHenleggelse) {
                throw FunksjonellFeil(
                    "Behandlingen er på steg '${behandling.steg.displayName()}', " + "og er da låst for alle andre type endringer." + "behandlingSteg er $behandlingSteg",
                )
            }

            behandlingSteg.preValiderSteg(behandling, this)
            val nesteSteg = utførendeSteg()
            val behandlingEtterUtførtSteg = behandlingHentOgPersisterService.hent(behandling.id)
            behandlingSteg.postValiderSteg(behandlingEtterUtførtSteg)

            stegSuksessMetrics[behandlingSteg.stegType()]?.increment()

            if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingEtterUtførtSteg.status)) {
                throw Feil("Steg '${nesteSteg.displayName()}' kan ikke settes på behandling i kombinasjon med status ${behandlingEtterUtførtSteg.status}")
            }

            val returBehandling =
                behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
                    behandlingId = behandling.id,
                    steg = nesteSteg,
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
                    logger.info("Håndtering av stegtype '${behandlingSteg.stegType()}' feilet på behandling $behandling.")
                    secureLogger.info(
                        "Håndtering av stegtype '${behandlingSteg.stegType()}' feilet på behandling $behandling.",
                        exception,
                    )
                }
            }

            throw exception
        }
    }

    private fun validerBehandlingIkkeSattPåVent(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg<*>,
    ) {
        if (behandling.status == BehandlingStatus.SATT_PÅ_VENT) {
            throw FunksjonellFeil(
                "${SikkerhetContext.hentSaksbehandlerNavn()} prøver å utføre steg " + behandlingSteg.stegType() + " på behandling ${behandling.id} som er på vent.",
            )
        }
        if (behandling.status == BehandlingStatus.SATT_PÅ_MASKINELL_VENT) {
            throw FunksjonellFeil(
                "${SikkerhetContext.hentSaksbehandlerNavn()} prøver å utføre steg " + behandlingSteg.stegType() + " på behandling ${behandling.id} som er på maskinell vent.",
            )
        }
    }

    private fun verifiserBeslutteVedtakForManuellMigrering(behandlingSteg: BehandlingSteg<*>) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "utføre steg ${behandlingSteg.stegType().displayName()}",
        )
    }

    fun hentBehandlingSteg(stegType: StegType): BehandlingSteg<*>? = steg.firstOrNull { it.stegType() == stegType }

    private fun hentSisteAvsluttetBehandling(behandling: Behandling): Behandling =
        behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) ?: throw Feil(
            "Forsøker å opprette en ${behandling.type.visningsnavn} " + "med årsak ${behandling.opprettetÅrsak.visningsnavn}, " + "men kan ikke finne tidligere behandling på fagsak ${behandling.fagsak.id}",
        )

    private fun initStegMetrikker(type: String): Map<StegType, Counter> =
        steg.associate {
            it.stegType() to
                Metrics.counter(
                    "behandling.steg.$type",
                    "steg",
                    it.stegType().name,
                    "beskrivelse",
                    it.stegType().rekkefølge.toString() + " " + it.stegType().displayName(),
                )
        }

    companion object {
        private val logger = LoggerFactory.getLogger(StegService::class.java)

        private fun hentUkjentBehandlingTypeOgÅrsakFeilMelding(nyBehandling: NyBehandling) = "Ukjent oppførsel ved opprettelse av ny behandling med årsak " + "${nyBehandling.behandlingÅrsak.visningsnavn} og " + "type ${nyBehandling.behandlingType.visningsnavn}."
    }
}
