package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.tilMinimertUregisrertBarn
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class BegrunnelseService(
    private val persongrunnlagService: PersongrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
) {
    fun genererBrevBegrunnelserForPeriode(vedtaksperiodeId: Long): List<Begrunnelse> {
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

        val brevGrunnlag = hentBrevGrunnlag(
            vilkårsvurdering = vilkårsvurdering,
            endredeUtbetalingAndeler = endredeUtbetalingAndeler,
            persongrunnlag = personopplysningGrunnlag
        )

        val utvidetVedtaksperiodeMedBegrunnelse = vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId
            )
        )

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

        return utvidetVedtaksperiodeMedBegrunnelse.hentBegrunnelserOgFritekster(
            brevGrunnlag = brevGrunnlag,
            sanityBegrunnelser = sanityBegrunnelser,
            uregistrerteBarn = uregistrerteBarn,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak(
                andelerTilkjentYtelse,
                utvidetVedtaksperiodeMedBegrunnelse.fom
            ),
            målformSøker = personopplysningGrunnlag.søker.målform,
        )
    }
}