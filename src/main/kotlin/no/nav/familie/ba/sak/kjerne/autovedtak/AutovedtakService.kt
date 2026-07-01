package no.nav.familie.ba.sak.kjerne.autovedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingTilBehandlingsresultatService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AutovedtakService(
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val loggService: LoggService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingTilBehandlingsresultatService,
) {
    /**
     * Oppretter en ny, automatisk behandling med gitt type og årsak, og kjører den til behandlingsresultat.
     *
     * [førVilkårsvurdering] kalles med den nyopprettede behandlingen etter at den har fått en id,
     * men før vilkårsvurderingssteget kjøres. Nyttig for kallere som trenger å koble noe til
     * `behandlingId` før vilkårsvurderingssteget og behandlingsresultatsteget kjører.
     */
    fun opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
        behandlingType: BehandlingType,
        behandlingÅrsak: BehandlingÅrsak,
        fagsakId: Long,
        førVilkårsvurdering: (Behandling) -> Unit = {},
    ): Behandling {
        val nyBehandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    behandlingType = behandlingType,
                    behandlingÅrsak = behandlingÅrsak,
                    skalBehandlesAutomatisk = true,
                    fagsakId = fagsakId,
                ),
            )

        førVilkårsvurdering(nyBehandling)

        val behandlingEtterBehandlingsresultat = stegService.håndterVilkårsvurdering(nyBehandling)
        return behandlingEtterBehandlingsresultat
    }

    fun opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)

        loggService.opprettBeslutningOmVedtakLogg(
            behandling = behandling,
            beslutning = Beslutning.GODKJENT,
            behandlingErAutomatiskBesluttet = true,
        )

        val vedtak =
            vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke aktivt vedtak på behandling ${behandling.id}")
        return vedtakService.oppdaterVedtakMedStønadsbrev(vedtak = vedtak)
    }

    fun omgjørBehandlingTilManuellOgKjørSteg(
        behandling: Behandling,
        steg: StegType,
    ): Behandling {
        val omgjortBehandling = behandlingService.omgjørTilManuellBehandling(behandling)

        return when (steg) {
            StegType.VILKÅRSVURDERING -> {
                tilbakestillBehandlingTilBehandlingsresultatService
                    .tilbakestillBehandlingTilBehandlingsresultat(behandlingId = omgjortBehandling.id)
            }

            else -> {
                throw Feil("Steg $steg er ikke støttet ved omgjøring av automatisk behandling til manuell.")
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakService::class.java)
    }
}
