package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val beregningService: BeregningService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vedtakRepository: VedtakRepository,
    private val tilbakekrevingService: TilbakekrevingService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService
) {

    @Transactional
    fun initierOgSettBehandlingTilVilårsvurdering(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean = true
    ) {
        vilkårService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = bekreftEndringerViaFrontend,
            forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                behandling
            )
        )

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)

        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.id)
        vedtaksperiodeService.slettVedtaksperioderFor(vedtak = vedtak)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandling.id,
            steg = StegType.VILKÅRSVURDERING
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandling.id)

        vedtakRepository.saveAndFlush(vedtak)
    }

    @Transactional
    fun tilbakestillBehandlingTilVilkårsvurdering(behandling: Behandling) {
        if (behandling.status.erLåstMenIkkeAvsluttet() || behandling.status == BehandlingStatus.AVSLUTTET) throw Feil("Prøver å tilbakestille $behandling, men den er avsluttet eller låst for endringer")

        endretUtbetalingAndelService.fjernKnytningTilAndelTilkjentYtelse(behandling.id)
        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.id)
        vedtaksperiodeService.slettVedtaksperioderFor(vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id))
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandling.id)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandling.id,
            steg = StegType.VILKÅRSVURDERING
        )
    }

    @Transactional
    fun tilbakestillDataTilVilkårsvurderingssteg(behandling: Behandling) {
        endretUtbetalingAndelService.fjernKnytningTilAndelTilkjentYtelse(behandling.id)
        vedtaksperiodeService.slettVedtaksperioderFor(vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.id))
    }

    /**
     * Når et vilkår vurderes (endres) vil vi resette steget og slette data som blir generert senere i løypa
     */
    @Transactional
    fun resettStegVedEndringPåVilkår(behandlingId: Long): Behandling {
        vedtaksperiodeService.slettVedtaksperioderFor(vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId))
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId)
        return behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandlingId,
            steg = StegType.VILKÅRSVURDERING
        )
    }

    /**
     * Når en andel vurderes (endres) vil vi resette steget og slette data som blir generert senere i løypa
     */
    @Transactional
    fun tilbakestillBehandlingTilBehandlingsresultat(behandlingId: Long): Behandling {
        vedtaksperiodeService.slettVedtaksperioderFor(vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId))
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId)
        return behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandlingId,
            steg = StegType.BEHANDLINGSRESULTAT
        )
    }
}
