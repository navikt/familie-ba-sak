package no.nav.familie.ba.sak.behandling.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StegService(
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val steg: List<BehandlingSteg<*>>,
        private val loggService: LoggService,
        private val rolleConfig: RolleConfig
) {

    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")

    @Transactional
    fun håndterNyBehandling(nyBehandling: NyBehandling): Behandling {
        val behandling = behandlingService.opprettBehandling(nyBehandling)
        loggService.opprettBehandlingLogg(behandling)

        return håndterPersongrunnlag(behandling,
                                     Registreringsdata(ident = nyBehandling.søkersIdent,
                                                       barnasIdenter = nyBehandling.barnasIdenter))
    }

    @Transactional
    fun håndterNyBehandlingFraHendelse(nyBehandling: NyBehandlingHendelse): Behandling {
        fagsakService.hentEllerOpprettFagsakForPersonIdent(nyBehandling.søkersIdent)

        val behandling = behandlingService.opprettBehandling(NyBehandling(
                søkersIdent = nyBehandling.søkersIdent,
                barnasIdenter = nyBehandling.barnasIdenter,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR
        ))

        loggService.opprettFødselshendelseLogg(behandling)
        loggService.opprettBehandlingLogg(behandling)

        return håndterPersongrunnlag(behandling,
                                     Registreringsdata(ident = nyBehandling.søkersIdent,
                                                       barnasIdenter = nyBehandling.barnasIdenter))
    }

    fun håndterPersongrunnlag(behandling: Behandling, registreringsdata: Registreringsdata): Behandling {
        val behandlingSteg: RegistrerPersongrunnlag =
                hentBehandlingSteg(StegType.REGISTRERE_PERSONGRUNNLAG) as RegistrerPersongrunnlag

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførSteg(behandling, registreringsdata)
        }
    }

    fun håndterVilkårsvurdering(behandling: Behandling, restVilkårsvurdering: RestVilkårsvurdering): Behandling {
        val behandlingSteg: Vilkårsvurdering =
                hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførSteg(behandling, restVilkårsvurdering)
        }
    }

    fun håndterSendTilBeslutter(behandling: Behandling): Behandling {
        val behandlingSteg: SendTilBeslutter = hentBehandlingSteg(StegType.SEND_TIL_BESLUTTER) as SendTilBeslutter

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførSteg(behandling, "")
        }
    }

    fun håndterGodkjenneVedtak(behandling: Behandling): Behandling {
        val behandlingSteg: GodkjenneVedtakOgStartIverksetting =
                hentBehandlingSteg(StegType.GODKJENNE_VEDTAK) as GodkjenneVedtakOgStartIverksetting

        return håndterSteg(behandling, behandlingSteg) {
            behandlingSteg.utførSteg(behandling, "")
        }
    }

    fun håndterFerdigstillBehandling(behandling: Behandling): Behandling {
        val behandlingStegSteg: FerdigstillBehandlingSteg =
                hentBehandlingSteg(StegType.FERDIGSTILLE_BEHANDLING) as FerdigstillBehandlingSteg

        return håndterSteg(behandling, behandlingStegSteg) {
            behandlingStegSteg.utførSteg(behandling, "")
        }
    }

    // Generelle stegmetoder
    fun håndterSteg(behandling: Behandling, behandlingSteg: BehandlingSteg<*>, uførendeSteg: () -> Behandling): Behandling {
        try {
            if (behandling.steg == sisteSteg) {
                error("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes")
            }

            if (behandlingSteg.stegType() > behandling.steg) {
                error("${SikkerhetContext.hentSaksbehandler()} prøver å utføre steg ${behandlingSteg.stegType()}," +
                      " men behandlingen er på steg ${behandling.steg}")
            }

            val behandlerRolle =
                    SikkerhetContext.hentBehandlerRolleForSteg(rolleConfig, behandling.steg.tillattFor.minBy { it.nivå })

            LOG.info("${SikkerhetContext.hentSaksbehandler()} håndterer ${behandling.steg} på behandling ${behandling.id}")
            if (!behandling.steg.tillattFor.contains(behandlerRolle)) {
                error("${SikkerhetContext.hentSaksbehandler()} kan ikke utføre steg '${behandling.steg}")
            }

            val behandlingEtterSteg = uførendeSteg()
            LOG.info("${SikkerhetContext.hentSaksbehandler()} har håndtert ${behandling.steg} på behandling ${behandling.id}")

            stegSuksessMetrics[behandling.steg]?.increment()

            val nesteSteg = behandling.steg.hentNesteSteg()
            behandlingService.oppdaterStegPåBehandling(behandlingId = behandlingEtterSteg.id,
                                                       steg = nesteSteg)

            if (nesteSteg == sisteSteg) {
                LOG.info("${SikkerhetContext.hentSaksbehandler()} er ferdig med stegprosess på behandling ${behandling.id}")
            }

            return behandlingEtterSteg
        } catch (exception: Exception) {
            stegFeiletMetrics[behandling.steg]?.increment()
            LOG.error("Håndtering av stegtype '${behandling.steg}' feilet på behandling ${behandling.id}.")
            secureLogger.info("Håndtering av stegtype '${behandling.steg}' feilet.",
                              exception)
            error(exception.message!!)
        }
    }

    private fun hentBehandlingSteg(stegType: StegType): BehandlingSteg<*>? {
        return steg.firstOrNull { it.stegType() == stegType }
    }

    private fun initStegMetrikker(type: String): Map<StegType, Counter> {
        return steg.map {
            it.stegType() to Metrics.counter("behandling.steg.$type",
                                             "steg",
                                             it.stegType().name,
                                             "beskrivelse",
                                             it.stegType().beskrivelse)
        }.toMap()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(this::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}