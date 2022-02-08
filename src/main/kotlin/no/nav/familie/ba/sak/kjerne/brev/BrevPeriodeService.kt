package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.tilMinimertUregisrertBarn
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevperiodeData
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class BrevPeriodeService(
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
) {

    fun hentBrevperiodeData(vedtaksperiodeId: Long): BrevperiodeData {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        val behandlingId = vedtaksperiodeMedBegrunnelser.vedtak.behandling.id

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")
        val endredeUtbetalingAndeler =
            endretUtbetalingAndelService.hentForBehandling(behandlingId = behandlingId)
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)

        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = behandlingId)?.hentUregistrerteBarn()
                ?.map { it.tilMinimertUregisrertBarn() } ?: emptyList()

        val restBehandlingsgrunnlagForBrev = hentRestBehandlingsgrunnlagForBrev(
            vilkårsvurdering = vilkårsvurdering,
            endredeUtbetalingAndeler = endredeUtbetalingAndeler,
            persongrunnlag = personopplysningGrunnlag
        )

        val utvidetVedtaksperiodeMedBegrunnelse = vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId
            ),
        )

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

        val minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelse.tilMinimertVedtaksperiode(sanityBegrunnelser)

        return BrevperiodeData(
            minimertVedtaksperiode = minimertVedtaksperiode,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            uregistrerteBarn = uregistrerteBarn,
            brevMålform = personopplysningGrunnlag.søker.målform,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
                andelerTilkjentYtelse,
                utvidetVedtaksperiodeMedBegrunnelse.fom
            )
        )
    }

    fun genererBrevBegrunnelserForPeriode(vedtaksperiodeId: Long): List<Begrunnelse> {
        val begrunnelseDataForVedtaksperiode = hentBrevperiodeData(vedtaksperiodeId)
        return begrunnelseDataForVedtaksperiode.hentBegrunnelserOgFritekster()
    }
}
