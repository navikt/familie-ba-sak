package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg.finnInnvilgedeOgReduserteSvalbardtilleggPerioder
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg.leggTilBegrunnelseIVedtaksperiode
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service

@Service
class AutovedtakSvalbardtilleggBegrunnelseService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val beregningService: BeregningService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
) {
    fun begrunnAutovedtakForSvalbardtillegg(
        behandlingEtterBehandlingsresultat: Behandling,
    ) {
        val sistIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandlingEtterBehandlingsresultat.fagsak.id) ?: throw Feil(
                "Finner ikke siste iverksatte behandling",
            )
        val forrigeAndeler = beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = sistIverksatteBehandling.id)
        val nåværendeAndeler = beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = behandlingEtterBehandlingsresultat.id)

        val (innvilgetMånedTidspunkt, redusertMånedTidspunkt) =
            finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
                forrigeAndeler = forrigeAndeler,
                nåværendeAndeler = nåværendeAndeler,
            )

        if (innvilgetMånedTidspunkt.isEmpty() && redusertMånedTidspunkt.isEmpty()) {
            throw Feil("Det er forsøkt å begrunne autovedtak men det ble ikke funnet noen perioder med innvilgelse eller reduksjon.")
        }

        val vedtaksperioder =
            vedtaksperiodeService.hentPersisterteVedtaksperioder(
                vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterBehandlingsresultat.id),
            )

        innvilgetMånedTidspunkt.forEach {
            leggTilBegrunnelseIVedtaksperiode(
                vedtaksperiodeStartDato = it,
                standardbegrunnelse = Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG,
                vedtaksperioder = vedtaksperioder,
            )
        }

        val alleSvalbardtilleggAndelerHarForsvunnet = redusertMånedTidspunkt.size == 1 && nåværendeAndeler.none { it.erSvalbardtillegg() }

        if (alleSvalbardtilleggAndelerHarForsvunnet) {
            leggTilBegrunnelseIVedtaksperiode(
                vedtaksperiodeStartDato = redusertMånedTidspunkt.single(),
                standardbegrunnelse = Standardbegrunnelse.REDUKSJON_SVALBARDTILLEGG_BODDE_IKKE_PÅ_SVALBARD,
                vedtaksperioder = vedtaksperioder,
            )
        } else {
            redusertMånedTidspunkt.forEach {
                leggTilBegrunnelseIVedtaksperiode(
                    vedtaksperiodeStartDato = it,
                    standardbegrunnelse = Standardbegrunnelse.REDUKSJON_SVALBARDTILLEGG,
                    vedtaksperioder = vedtaksperioder,
                )
            }
        }

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperioder)
    }
}
