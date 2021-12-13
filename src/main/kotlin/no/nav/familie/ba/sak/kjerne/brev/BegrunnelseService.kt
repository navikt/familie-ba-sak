package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.tilUregisrertBarnEnkel
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.erFørsteVedtaksperiodePåFagsak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.tilUtvidetVedtaksperiodeMedBegrunnelser
import org.springframework.stereotype.Service

@Service
class BegrunnelseService(
    private val persongrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val sanityService: SanityService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val brevService: BrevService,
) {
    fun genererBrevBegrunnelserForPeriode(vedtaksperiodeId: Long): List<Begrunnelse> {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        val sanityBegrunnelser = sanityService.hentSanityBegrunnelser()

        val behandlingId = vedtaksperiodeMedBegrunnelser.vedtak.behandling.id

        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = behandlingId)?.hentUregistrerteBarn()
                ?.map { it.tilUregisrertBarnEnkel() } ?: emptyList()

        val brevGrunnlag = brevService.hentBrevGrunnlag(behandlingId)
        val personopplysningGrunnlag = persongrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)
            ?: throw Feil(PersongrunnlagService.finnerIkkePersongrunnlagFeilmelding(behandlingId = behandlingId))

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