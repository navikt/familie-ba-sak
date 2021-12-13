package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.behandlingsresultat.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.BrevGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilBrevPeriodeGrunnlag
import java.time.LocalDate

data class UtvidetVedtaksperiodeMedBegrunnelser(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<RestVedtaksbegrunnelse>,
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<VedtakBegrunnelseSpesifikasjon> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
) {
    fun hentBegrunnelserOgFritekster(
        brevGrunnlag: BrevGrunnlag,
        sanityBegrunnelser: List<SanityBegrunnelse>,
        erFørsteVedtaksperiodePåFagsak: Boolean,
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        målformSøker: Målform,
    ) = this
        .tilBrevPeriodeGrunnlag(sanityBegrunnelser)
        .tilBrevPeriodeGrunnlagMedPersoner(
            brevGrunnlag = brevGrunnlag,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
        )
        .byggBegrunnelserOgFritekster(
            brevGrunnlag = brevGrunnlag,
            uregistrerteBarn = uregistrerteBarn,
            målformSøker = målformSøker
        )
}

data class RestVedtaksbegrunnelse(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val personIdenter: List<String> = emptyList(),
)

fun List<UtvidetVedtaksperiodeMedBegrunnelser>.sorter(): List<UtvidetVedtaksperiodeMedBegrunnelser> {
    val (perioderMedFom, perioderUtenFom) = this.partition { it.fom != null }
    return perioderMedFom.sortedWith(compareBy { it.fom }) + perioderUtenFom
}

fun VedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>
): UtvidetVedtaksperiodeMedBegrunnelser {

    val utbetalingsperiodeDetaljer = hentUtbetalingsperiodeDetaljer(andelerTilkjentYtelse, personopplysningGrunnlag)

    return UtvidetVedtaksperiodeMedBegrunnelser(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.map { it.tilRestVedtaksbegrunnelse() },
        fritekster = this.fritekster.sortedBy { it.id }.map { it.fritekst },
        utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer
    )
}
