package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillBehandlingService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakRepository: VedtakRepository,
    private val tilbakekrevingService: TilbakekrevingService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService
) {

    @Transactional
    fun initierOgSettBehandlingTilVilkårsvurdering(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean = true
    ) {
        vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = bekreftEndringerViaFrontend,
            forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                behandling
            )
        )

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId = behandling.behandlingId.id)

        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.behandlingId)
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak = vedtak)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandling.behandlingId,
            steg = StegType.VILKÅRSVURDERING
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId = behandling.behandlingId)

        vedtakRepository.saveAndFlush(vedtak)
    }

    @Transactional
    fun tilbakestillBehandlingTilVilkårsvurdering(behandling: Behandling) {
        if (behandling.status.erLåstMenIkkeAvsluttet() || behandling.status == BehandlingStatus.AVSLUTTET) throw Feil("Prøver å tilbakestille $behandling, men den er avsluttet eller låst for endringer")

        beregningService.slettTilkjentYtelseForBehandling(behandlingId = behandling.behandlingId)
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(
            vedtak = vedtakRepository.findByBehandlingAndAktiv(
                behandlingId = behandling.behandlingId.id
            )
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId = behandling.behandlingId)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandling.behandlingId,
            steg = StegType.VILKÅRSVURDERING
        )
    }

    @Transactional
    fun tilbakestillDataTilVilkårsvurderingssteg(behandling: Behandling) {
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(
            vedtak = vedtakRepository.findByBehandlingAndAktiv(
                behandlingId = behandling.behandlingId.id
            )
        )
    }

    /**
     * Når et vilkår vurderes (endres) vil vi resette steget og slette data som blir generert senere i løypa
     */
    @Transactional
    fun resettStegVedEndringPåVilkår(behandlingId: BehandlingId): Behandling {
        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(
            vedtak = vedtakRepository.findByBehandlingAndAktiv(
                behandlingId.id
            )
        )
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId = behandlingId)
        return behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(
            behandlingId = behandlingId,
            steg = StegType.VILKÅRSVURDERING
        )
    }
}
