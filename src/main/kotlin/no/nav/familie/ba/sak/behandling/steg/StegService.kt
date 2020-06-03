package no.nav.familie.ba.sak.behandling.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.SimuleringException
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
        private val behandlingResultatRepository: BehandlingResultatRepository
) {

    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")

    @Transactional
    fun håndterNyBehandling(nyBehandling: NyBehandling): Behandling {
        val behandling = behandlingService.opprettBehandling(nyBehandling)
        loggService.opprettBehandlingLogg(behandling)

        return behandling
    }

    @Transactional
    fun håndterNyBehandlingFraHendelse(nyBehandling: NyBehandlingHendelse): Behandling {
        fagsakService.hentEllerOpprettFagsakForPersonIdent(nyBehandling.søkersIdent)

        val behandling = behandlingService.opprettBehandling(NyBehandling(
                søkersIdent = nyBehandling.søkersIdent,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingOpprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE
        ))

        loggService.opprettFødselshendelseLogg(behandling)
        loggService.opprettBehandlingLogg(behandling)

        return håndterPersongrunnlag(behandling,
                RegistrerPersongrunnlagDTO(ident = nyBehandling.søkersIdent,
                        barnasIdenter = nyBehandling.barnasIdenter,
                        bekreftEndringerViaFrontend = true))
    }

    @Transactional
    fun håndterSøknad(behandling: Behandling, restRegistrerSøknad: RestRegistrerSøknad): Behandling {
        val behandlingSteg: RegistrereSøknad = hentBehandlingSteg(StegType.REGISTRERE_SØKNAD) as RegistrereSøknad
        val søknadDTO = restRegistrerSøknad.søknad

        val behandlingEtterSøknadshåndtering = håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, søknadDTO, this)
        }

        return håndterPersongrunnlag(
                behandlingEtterSøknadshåndtering,
                RegistrerPersongrunnlagDTO(ident = søknadDTO.søkerMedOpplysninger.ident,
                        barnasIdenter = søknadDTO.barnaMedOpplysninger.filter { it.inkludertISøknaden }
                                .map { barn -> barn.ident },
                        bekreftEndringerViaFrontend = restRegistrerSøknad.bekreftEndringerViaFrontend))
    }

    @Transactional
    fun håndterPersongrunnlag(behandling: Behandling, registrerPersongrunnlagDTO: RegistrerPersongrunnlagDTO): Behandling {
        val behandlingSteg: RegistrerPersongrunnlag =
                hentBehandlingSteg(StegType.REGISTRERE_PERSONGRUNNLAG) as RegistrerPersongrunnlag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, registrerPersongrunnlagDTO, this)
        }
    }

    @Transactional
    fun håndterVilkårsvurdering(behandling: Behandling, restVilkårsvurdering: RestVilkårsvurdering): Behandling {
        val behandlingSteg: Vilkårsvurdering =
                hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, restVilkårsvurdering, this)
        }
    }

    @Transactional
    fun håndterSendTilBeslutter(behandling: Behandling, behandlendeEnhet: String): Behandling {
        val behandlingSteg: SendTilBeslutter = hentBehandlingSteg(StegType.SEND_TIL_BESLUTTER) as SendTilBeslutter

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, behandlendeEnhet, this)
        }
    }

    @Transactional
    fun håndterBeslutningForVedtak(behandling: Behandling, restBeslutningPåVedtak: RestBeslutningPåVedtak): Behandling {
        val behandlingSteg: BeslutteVedtak =
                hentBehandlingSteg(StegType.BESLUTTE_VEDTAK) as BeslutteVedtak

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, restBeslutningPåVedtak, this)
        }
    }

    @Transactional
    fun håndterIverksettMotØkonomi(behandling: Behandling, iverksettingTaskDTO: IverksettingTaskDTO): Behandling {
        val behandlingSteg: IverksettMotOppdrag =
                hentBehandlingSteg(StegType.IVERKSETT_MOT_OPPDRAG) as IverksettMotOppdrag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, iverksettingTaskDTO, this)
        }
    }

    @Transactional
    fun håndterStatusFraØkonomi(behandling: Behandling, statusFraOppdragMedTask: StatusFraOppdragMedTask): Behandling {
        val behandlingSteg: StatusFraOppdrag =
                hentBehandlingSteg(StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) as StatusFraOppdrag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, statusFraOppdragMedTask, this)
        }
    }

    @Transactional
    fun håndterJournalførVedtaksbrev(behandling: Behandling, journalførVedtaksbrevDTO: JournalførVedtaksbrevDTO): Behandling {
        val behandlingSteg: JournalførVedtaksbrev =
                hentBehandlingSteg(StegType.JOURNALFØR_VEDTAKSBREV) as JournalførVedtaksbrev

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, journalførVedtaksbrevDTO, this)
        }
    }

    @Transactional
    fun håndterDistribuerVedtaksbrev(behandling: Behandling, distribuerVedtaksbrevDTO: DistribuerVedtaksbrevDTO): Behandling {
        val behandlingSteg: DistribuerVedtaksbrev =
                hentBehandlingSteg(StegType.DISTRIBUER_VEDTAKSBREV) as DistribuerVedtaksbrev

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, distribuerVedtaksbrevDTO, this)
        }
    }

    @Transactional
    fun håndterFerdigstillBehandling(behandling: Behandling): Behandling {
        val behandlingSteg: FerdigstillBehandling =
                hentBehandlingSteg(StegType.FERDIGSTILLE_BEHANDLING) as FerdigstillBehandling

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, "", this)
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

            val nesteSteg = utførendeSteg()

            behandlingSteg.validerSteg(behandling)

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

    @Transactional
    fun regelkjørBehandling(nyBehandling: NyBehandlingHendelse) {
        val behandling = håndterNyBehandlingFraHendelse(nyBehandling)
        val behandlingResultat = behandlingResultatRepository.findByBehandlingAndAktiv(behandling.id)
        val samletResultat = behandlingResultat?.hentSamletResultat()
        throw SimuleringException(samletResultat)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}