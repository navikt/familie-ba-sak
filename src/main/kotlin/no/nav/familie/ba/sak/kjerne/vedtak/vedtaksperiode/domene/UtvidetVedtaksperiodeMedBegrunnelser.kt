package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentUtbetalingsperiodeDetaljerNy
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

data class UtvidetVedtaksperiodeMedBegrunnelser(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<Vedtaksbegrunnelse>,
    val eøsBegrunnelser: List<EØSBegrunnelse>,
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<IVedtakBegrunnelse> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
)

fun List<UtvidetVedtaksperiodeMedBegrunnelser>.sorter(): List<UtvidetVedtaksperiodeMedBegrunnelser> {
    val (perioderMedFom, perioderUtenFom) = this.partition { it.fom != null }
    return perioderMedFom.sortedWith(compareBy { it.fom }) + perioderUtenFom
}

fun VedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    skalBrukeNyVedtaksperiodeLøsning: Boolean,
): UtvidetVedtaksperiodeMedBegrunnelser {
    val utbetalingsperiodeDetaljer = if (skalBrukeNyVedtaksperiodeLøsning) {
        this.hentUtbetalingsperiodeDetaljerNy(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag,
        )
    } else {
        hentUtbetalingsperiodeDetaljer(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag,
        )
    }

    return UtvidetVedtaksperiodeMedBegrunnelser(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.toList(),
        eøsBegrunnelser = this.eøsBegrunnelser.toList(),
        fritekster = this.fritekster.sortedBy { it.id }.map { it.fritekst },
        utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer,
    )
}
