package no.nav.familie.ba.sak.behandling.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.*
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatRepository
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StegService(
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val steg: List<BehandlingSteg<*>>,
        private val loggService: LoggService,
        private val rolleConfig: RolleConfig,
        private val behandlingResultatRepository: BehandlingResultatRepository,
        private val søknadGrunnlagService: SøknadGrunnlagService
) {

    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")

    @Transactional
    fun håndterNyBehandling(nyBehandling: NyBehandling): Behandling {
        val behandling = behandlingService.opprettBehandling(nyBehandling)

        return when (nyBehandling.behandlingType) {
            BehandlingType.MIGRERING_FRA_INFOTRYGD ->
                håndterPersongrunnlag(behandling,
                                      RegistrerPersongrunnlagDTO(ident = nyBehandling.søkersIdent,
                                                                 barnasIdenter = nyBehandling.barnasIdenter,
                                                                 bekreftEndringerViaFrontend = true))
            else -> behandling
        }
    }

    @Transactional
    fun opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling: NyBehandlingHendelse): Behandling {
        val ident = nyBehandling.morsIdent
                    ?: (nyBehandling.søkersIdent ?: error("Fant ingen gyldig ident på mor/søker"))
        fagsakService.hentEllerOpprettFagsakForPersonIdent(ident)

        val behandling = behandlingService.opprettBehandling(NyBehandling(
                søkersIdent = ident,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingOpprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE
        ))

        loggService.opprettFødselshendelseLogg(behandling)

        return håndterPersongrunnlag(behandling,
                                     RegistrerPersongrunnlagDTO(ident = ident,
                                                                barnasIdenter = nyBehandling.barnasIdenter,
                                                                bekreftEndringerViaFrontend = true))
    }

    fun evaluerVilkårForFødselshendelse(behandling: Behandling, søkersIdent: String?): BehandlingResultatType? {
        håndterVilkårsvurdering(behandling)
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandling.id)
        return behandlingResultat?.hentSamletResultat()
                .also { secureLogger.info("Behandling med søkerident $søkersIdent fullført med resultat: $it") }
    }

    @Transactional
    fun håndterSøknad(behandling: Behandling,
                      restRegistrerSøknad: RestRegistrerSøknad): Behandling =
            fullførSøknadsHåndtering(behandling = behandling, registrerSøknad = restRegistrerSøknad)

    @Transactional
    fun håndterSøknad(behandling: Behandling,
                      restRegistrerSøknadGammel: RestRegistrerSøknadGammel): Behandling {

        val registrerSøknad = RestRegistrerSøknad(søknad = restRegistrerSøknadGammel.søknad.toSøknadDTO(),
                                bekreftEndringerViaFrontend = restRegistrerSøknadGammel.bekreftEndringerViaFrontend)

        return fullførSøknadsHåndtering(behandling = behandling, registrerSøknad = registrerSøknad)
    }

    private fun fullførSøknadsHåndtering(behandling: Behandling, registrerSøknad: RestRegistrerSøknad): Behandling {
        val behandlingSteg: RegistrereSøknad = hentBehandlingSteg(StegType.REGISTRERE_SØKNAD) as RegistrereSøknad
        val søknadDTO = registrerSøknad.søknad

        val aktivSøknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)
        val innsendtSøknad = søknadDTO.writeValueAsString()

        if (aktivSøknadGrunnlag != null && innsendtSøknad == aktivSøknadGrunnlag.søknad) {
            return behandling
        }

        val behandlingEtterSøknadshåndtering = håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, søknadDTO)
        }

        return håndterPersongrunnlag(
                behandlingEtterSøknadshåndtering,
                RegistrerPersongrunnlagDTO(ident = søknadDTO.søkerMedOpplysninger.ident,
                                           barnasIdenter = søknadDTO.barnaMedOpplysninger.filter { it.inkludertISøknaden }
                                                   .map { barn -> barn.ident },
                                           bekreftEndringerViaFrontend = registrerSøknad.bekreftEndringerViaFrontend,
                                           målform = søknadDTO.søkerMedOpplysninger.målform))
    }

    @Transactional
    fun håndterPersongrunnlag(behandling: Behandling, registrerPersongrunnlagDTO: RegistrerPersongrunnlagDTO): Behandling {
        val behandlingSteg: RegistrerPersongrunnlag =
                hentBehandlingSteg(StegType.REGISTRERE_PERSONGRUNNLAG) as RegistrerPersongrunnlag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, registrerPersongrunnlagDTO)
        }
    }

    @Transactional
    fun håndterVilkårsvurdering(behandling: Behandling): Behandling {
        val behandlingSteg: Vilkårsvurdering =
                hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, "")
        }
    }

    @Transactional
    fun håndterSendTilBeslutter(behandling: Behandling, behandlendeEnhet: String): Behandling {
        val behandlingSteg: SendTilBeslutter = hentBehandlingSteg(StegType.SEND_TIL_BESLUTTER) as SendTilBeslutter

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, behandlendeEnhet)
        }
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
    fun håndterJournalførVedtaksbrev(behandling: Behandling, journalførVedtaksbrevDTO: JournalførVedtaksbrevDTO): Behandling {
        val behandlingSteg: JournalførVedtaksbrev =
                hentBehandlingSteg(StegType.JOURNALFØR_VEDTAKSBREV) as JournalførVedtaksbrev

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, journalførVedtaksbrevDTO)
        }
    }

    @Transactional
    fun håndterDistribuerVedtaksbrev(behandling: Behandling, distribuerVedtaksbrevDTO: DistribuerVedtaksbrevDTO): Behandling {
        val behandlingSteg: DistribuerVedtaksbrev =
                hentBehandlingSteg(StegType.DISTRIBUER_VEDTAKSBREV) as DistribuerVedtaksbrev

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, distribuerVedtaksbrevDTO)
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
    private fun håndterSteg(behandling: Behandling,
                            behandlingSteg: BehandlingSteg<*>,
                            utførendeSteg: () -> StegType): Behandling {
        try {
            if (behandling.steg == sisteSteg) {
                error("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes")
            }

            if (behandlingSteg.stegType().kommerEtter(behandling.steg)) {
                error("${SikkerhetContext.hentSaksbehandlerNavn()} prøver å utføre steg '${behandlingSteg.stegType()
                        .displayName()}'," +
                      " men behandlingen er på steg '${behandling.steg.displayName()}'")
            }

            if (behandling.steg == StegType.BESLUTTE_VEDTAK && behandlingSteg.stegType() != StegType.BESLUTTE_VEDTAK) {
                error("Behandlingen er på steg '${behandling.steg.displayName()}', og er da låst for alle andre type endringer.")
            }

            val behandlerRolle =
                    SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, behandling.steg.tillattFor.minBy { it.nivå })

            LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} håndterer ${behandlingSteg.stegType()} på behandling ${behandling.id}")
            if (!behandling.steg.tillattFor.contains(behandlerRolle)) {
                error("${SikkerhetContext.hentSaksbehandlerNavn()} kan ikke utføre steg '${behandlingSteg.stegType()
                        .displayName()} pga manglende rolle.")
            }

            behandlingSteg.preValiderSteg(behandling, this)
            val nesteSteg = utførendeSteg()
            behandlingSteg.postValiderSteg(behandling)

            stegSuksessMetrics[behandlingSteg.stegType()]?.increment()

            if (nesteSteg == sisteSteg) {
                LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} er ferdig med stegprosess på behandling ${behandling.id}")
            }

            if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingService.hent(behandling.id).status)) {
                error("Steg '${nesteSteg.displayName()}' kan ikke settes på behandling i kombinasjon med status ${behandling.status}")
            }

            val returBehandling = behandlingService.oppdaterStegPåBehandling(behandlingId = behandling.id, steg = nesteSteg)

            LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} har håndtert ${behandlingSteg.stegType()} på behandling ${behandling.id}")
            return returBehandling
        } catch (exception: Exception) {
            stegFeiletMetrics[behandlingSteg.stegType()]?.increment()
            LOG.error("Håndtering av stegtype '${behandlingSteg.stegType()}' feilet på behandling ${behandling.id}.")
            secureLogger.info("Håndtering av stegtype '${behandlingSteg.stegType()}' feilet.",
                              exception)
            throw exception
        }
    }

    fun hentBehandlingSteg(stegType: StegType): BehandlingSteg<*>? {
        return steg.firstOrNull { it.stegType() == stegType }
    }

    private fun initStegMetrikker(type: String): Map<StegType, Counter> {
        return steg.map {
            it.stegType() to Metrics.counter("behandling.steg.$type",
                                             "steg",
                                             it.stegType().name,
                                             "beskrivelse",
                                             it.stegType().displayName())
        }.toMap()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}