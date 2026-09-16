package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.leggTilBegrunnelseIVedtaksperiode
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service

@Service
class AutovedtakSøknadBegrunnelseService(
    private val beregningService: BeregningService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
) {
    fun begrunnAutovedtakForSøknad(
        behandlingEtterBehandlingsresultat: Behandling,
    ) {
        val nyeAndeler =
            finnNyeAndeler(
                forrigeAndeler = beregningService.hentAndelerFraForrigeVedtatteBehandling(behandlingEtterBehandlingsresultat),
                nåværendeAndeler = beregningService.hentAndelerTilkjentYtelseForBehandling(behandlingId = behandlingEtterBehandlingsresultat.id),
            )
        val månederMedNyInnvilgelse = nyeAndeler.finnFørsteMånederMedUtbetaling(YtelseType.ORDINÆR_BARNETRYGD)

        if (månederMedNyInnvilgelse.isEmpty()) {
            throw AutovedtakMåBehandlesManueltFeil("Det er forsøkt å begrunne autovedtak søknad men det ble ikke funnet noen perioder med ny innvilgelse.\nSøknaden må behandles manuelt.")
        }

        val vedtaksperioder =
            vedtaksperiodeService.hentPersisterteVedtaksperioder(
                vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterBehandlingsresultat.id),
            )

        mapOf(
            Standardbegrunnelse.INNVILGET_AUTOMATISK_SØKNAD to månederMedNyInnvilgelse,
            Standardbegrunnelse.INNVILGET_SATSENDRING to nyeAndeler.finnMånederMedSatsøkning(),
            Standardbegrunnelse.INNVILGET_FINNMARKSTILLEGG_UTEN_DATO to nyeAndeler.finnFørsteMånederMedUtbetaling(YtelseType.FINNMARKSTILLEGG),
            Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG_UTEN_DATO to nyeAndeler.finnFørsteMånederMedUtbetaling(YtelseType.SVALBARDTILLEGG),
        ).forEach { (standardbegrunnelse, måneder) ->
            måneder.forEach { leggTilBegrunnelseIVedtaksperiode(vedtaksperiodeStartDato = it, standardbegrunnelse = standardbegrunnelse, vedtaksperioder = vedtaksperioder) }
        }

        vedtaksperioder
            .filter { Standardbegrunnelse.ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_AUTOMATISK.vedtakBegrunnelseType in it.type.tillatteBegrunnelsestyper }
            .mapNotNull { it.fom?.toYearMonth() }
            .filter { nyeAndeler.harAndelUtenUtbetalingI(it) }
            .forEach { leggTilBegrunnelseIVedtaksperiode(vedtaksperiodeStartDato = it, standardbegrunnelse = Standardbegrunnelse.ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_AUTOMATISK, vedtaksperioder = vedtaksperioder) }

        vedtaksperiodeHentOgPersisterService.lagre(vedtaksperioder)
    }
}
