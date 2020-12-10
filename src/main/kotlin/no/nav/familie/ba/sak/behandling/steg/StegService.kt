package no.nav.familie.ba.sak.behandling.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.restDomene.writeValueAsString
import no.nav.familie.ba.sak.behandling.vedtak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingRepository
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.skyggesak.SkyggesakService
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StegService(
        private val steg: List<BehandlingSteg<*>>,
        private val loggService: LoggService,
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,
        private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        private val rolleConfig: RolleConfig,
        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
        private val envService: EnvService,
        private val skyggesakService: SkyggesakService,
) {

    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")
    private val stegFunksjonellFeilMetrics: Map<StegType, Counter> = initStegMetrikker("funksjonell-feil")

    @Transactional
    fun håndterNyBehandling(nyBehandling: NyBehandling): Behandling {
        val behandling = behandlingService.opprettBehandling(nyBehandling)

        return if (nyBehandling.behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD || nyBehandling.behandlingÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            håndterPersongrunnlag(behandling,
                                  RegistrerPersongrunnlagDTO(ident = nyBehandling.søkersIdent,
                                                             barnasIdenter = nyBehandling.barnasIdenter))
        } else if (nyBehandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
            håndterPersongrunnlag(behandling,
                                  RegistrerPersongrunnlagDTO(ident = nyBehandling.søkersIdent,
                                                             barnasIdenter = emptyList()))
        } else {
            val sisteBehandling = behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
                                  ?: error("Forsøker å gjøre teknisk opphør, men kan ikke finne tidligere iverksatt behandling på fagsak ${behandling.fagsak.id}")
            val barnFraSisteBehandling =
                    personopplysningGrunnlagRepository.findByBehandlingAndAktiv(sisteBehandling.id)?.barna?.map { it.personIdent.ident }
                    ?: error("Forsøker å gjøre teknisk opphør, men kan ikke finne personopplysningsgrunnlag på siste behandling ${behandling.id}")
            håndterPersongrunnlag(behandling,
                                  RegistrerPersongrunnlagDTO(ident = nyBehandling.søkersIdent,
                                                             barnasIdenter = barnFraSisteBehandling))
        }
    }

    @Transactional
    fun opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandlingHendelse: NyBehandlingHendelse): Behandling {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(nyBehandlingHendelse.morsIdent, true)
        if (envService.skalIverksetteBehandling()) { //Ikke send statistikk for fødselshendelser før man skrur det på.
            //Denne vil sende selv om det allerede eksisterer en fagsak. Vi tenker det er greit. Ellers så blir det vanskelig å
            //filtere bort for fødselshendelser. Når vi slutter å filtere bort fødselshendelser, så kan vi flytte den tilbake til
            //hentEllerOpprettFagsak
            saksstatistikkEventPublisher.publiserSaksstatistikk(fagsak.id)
            skyggesakService.opprettSkyggesak(nyBehandlingHendelse.morsIdent, fagsak.id)
        }

        val behandling = håndterNyBehandling(
                NyBehandling(søkersIdent = nyBehandlingHendelse.morsIdent,
                             behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                             kategori = BehandlingKategori.NASJONAL,
                             underkategori = BehandlingUnderkategori.ORDINÆR,
                             behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                             skalBehandlesAutomatisk = true,
                             barnasIdenter = nyBehandlingHendelse.barnasIdenter
                ))

        loggService.opprettFødselshendelseLogg(behandling)
        return behandling
    }


    fun evaluerVilkårForFødselshendelse(behandling: Behandling,
                                        personopplysningGrunnlag: PersonopplysningGrunnlag?): BehandlingResultat =
            håndterVilkårsvurdering(behandling).resultat
                    .also {
                        LOG.info("Vilkårsvurdering på behandling ${behandling.id} fullført med resultat: $it")
                        secureLogger.info("Vilkårsvurdering på behandling ${behandling.id} med søkerident ${behandling.fagsak.hentAktivIdent().ident} fullført med resultat: $it")
                    }

    @Transactional
    fun håndterSøknad(behandling: Behandling,
                      restRegistrerSøknad: RestRegistrerSøknad): Behandling =
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
    fun håndterPersongrunnlag(behandling: Behandling, registrerPersongrunnlagDTO: RegistrerPersongrunnlagDTO): Behandling {
        val behandlingSteg: RegistrerPersongrunnlag =
                hentBehandlingSteg(StegType.REGISTRERE_PERSONGRUNNLAG) as RegistrerPersongrunnlag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, registrerPersongrunnlagDTO)
        }
    }

    @Transactional
    fun håndterVilkårsvurdering(behandling: Behandling): Behandling {
        val behandlingSteg: VilkårsvurderingSteg =
                hentBehandlingSteg(StegType.VILKÅRSVURDERING) as VilkårsvurderingSteg

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
    fun håndterHenleggBehandling(behandling: Behandling, henleggBehandlingInfo: RestHenleggBehandlingInfo): Behandling {
        val behandlingSteg: HenleggBehandling =
                hentBehandlingSteg(StegType.HENLEGG_SØKNAD) as HenleggBehandling

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførStegOgAngiNeste(behandling, henleggBehandlingInfo)
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
            val behandlerRolle =
                    SikkerhetContext.hentRolletilgangFraSikkerhetscontext(rolleConfig,
                                                                          behandling.steg.tillattFor.minByOrNull { it.nivå })

            LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} håndterer ${behandlingSteg.stegType()} på behandling ${behandling.id}")
            if (!behandling.steg.tillattFor.contains(behandlerRolle)) {
                error("${SikkerhetContext.hentSaksbehandlerNavn()} kan ikke utføre steg '${
                    behandlingSteg.stegType()
                            .displayName()
                }' pga manglende rolle.")
            }

            if (behandling.steg == SISTE_STEG) {
                error("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes")
            }

            if (behandlingSteg.stegType().erSaksbehandlerSteg() && behandlingSteg.stegType().kommerEtter(behandling.steg)) {
                error("${SikkerhetContext.hentSaksbehandlerNavn()} prøver å utføre steg '${
                    behandlingSteg.stegType()
                            .displayName()
                }', men behandlingen er på steg '${behandling.steg.displayName()}'")
            }

            //TODO: Det bør sees på en ytterligere robustgjøring for alle steg som SB kan utføre.
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
                    behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandlingId = behandling.id,
                                                                                          steg = nesteSteg)

            if (nesteSteg == SISTE_STEG) {
                LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} er ferdig med stegprosess på behandling ${behandling.id}")
            } else {
                LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} har håndtert ${behandlingSteg.stegType()} på behandling ${behandling.id}. Neste steg er $nesteSteg")
            }
            return returBehandling
        } catch (exception: Exception) {

            if (exception is FunksjonellFeil) {
                stegFunksjonellFeilMetrics[behandlingSteg.stegType()]?.increment()
                LOG.info("Håndtering av stegtype '${behandlingSteg.stegType()}' feilet på grunn av funksjonell feil på behandling ${behandling.id}. Melding: ${exception.melding}")
            } else {
                stegFeiletMetrics[behandlingSteg.stegType()]?.increment()
                LOG.error("Håndtering av stegtype '${behandlingSteg.stegType()}' feilet på behandling ${behandling.id}.")
                secureLogger.error("Håndtering av stegtype '${behandlingSteg.stegType()}' feilet.", exception)
            }

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
                                             it.stegType().rekkefølge.toString() + " " + it.stegType().displayName())
        }.toMap()
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
