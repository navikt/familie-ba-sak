package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.NullableMånedPeriode
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.overlapperHeltEllerDelvisMed
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelsePerson
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

data class BrevPeriodeGrunnlag(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlag>,
    val fritekster: List<String> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
) {
    fun tilBrevPeriodeGrunnlagMedPersoner(
        begrunnelseGrunnlag: BegrunnelseGrunnlag,
        erFørsteVedtaksperiodePåFagsak: Boolean
    ): BrevPeriodeGrunnlagMedPersoner {
        return BrevPeriodeGrunnlagMedPersoner(
            fom = this.fom,
            tom = this.tom,
            type = this.type,
            begrunnelser = this.begrunnelser.map {
                it.tilBrevBegrunnelseGrunnlagMedPersoner(
                    periode = NullablePeriode(
                        fom = this.fom,
                        tom = this.tom
                    ),
                    periodeType = this.type,
                    begrunnelseGrunnlag = begrunnelseGrunnlag,
                    identerMedUtbetaling = this.utbetalingsperiodeDetaljer
                        .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent },
                    erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak
                )
            },
            fritekster = this.fritekster,
            utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        )
    }

    fun hentMånedPeriode() = MånedPeriode(
        (fom ?: TIDENES_MORGEN).toYearMonth(),
        (tom ?: TIDENES_ENDE).toYearMonth()
    )
}

fun UtvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriodeGrunnlag(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BrevPeriodeGrunnlag {
    return BrevPeriodeGrunnlag(
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        fritekster = this.fritekster,
        utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer,
        begrunnelser = this.begrunnelser.map { it.tilBrevBegrunnelseGrunnlag(sanityBegrunnelser) }
    )
}

data class BrevBegrunnelseGrunnlag(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
) {
    fun tilBrevBegrunnelseGrunnlagMedPersoner(
        periode: NullablePeriode,
        periodeType: Vedtaksperiodetype,
        begrunnelseGrunnlag: BegrunnelseGrunnlag,
        identerMedUtbetaling: List<String>,
        erFørsteVedtaksperiodePåFagsak: Boolean,
    ): BrevBegrunnelseGrunnlagMedPersoner {
        val personidenterGjeldendeForBegrunnelse: List<String> = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = this.triggesAv,
            vedtakBegrunnelseType = this.vedtakBegrunnelseType,
            periode = periode,
            vedtaksperiodeType = periodeType,
            begrunnelseGrunnlag = begrunnelseGrunnlag,
            identerMedUtbetaling = identerMedUtbetaling,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
        )

        return BrevBegrunnelseGrunnlagMedPersoner(
            vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
            vedtakBegrunnelseType = this.vedtakBegrunnelseType,
            triggesAv = this.triggesAv,
            personIdenter = personidenterGjeldendeForBegrunnelse
        )
    }
}

fun RestVedtaksbegrunnelse.tilBrevBegrunnelseGrunnlag(
    sanityBegrunnelser: List<SanityBegrunnelse>
): BrevBegrunnelseGrunnlag {
    return BrevBegrunnelseGrunnlag(
        vedtakBegrunnelseSpesifikasjon = this.vedtakBegrunnelseSpesifikasjon,
        vedtakBegrunnelseType = this.vedtakBegrunnelseType,
        triggesAv = sanityBegrunnelser
            .firstOrNull { it.apiNavn == this.vedtakBegrunnelseSpesifikasjon.sanityApiNavn }!! // TODO : Håndtere feil
            .tilTriggesAv()
    )
}

data class BrevBegrunnelseGrunnlagMedPersoner(
    val vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
    val personIdenter: List<String> = emptyList(),
)

data class BegrunnelseGrunnlag(
    val begrunnelsePersoner: List<BegrunnelsePerson>,
    val personResultater: Set<PersonResultat>, // TODO: Kan minimeres?
    val minimerteEndredeUtbetalingAndeler: List<MinimertEndretUtbetalingAndel>,
)

data class MinimertEndretUtbetalingAndel(
    val periode: MånedPeriode,
    val personIdent: String,
    val årsak: Årsak,
) {
    fun erOverlappendeMed(nullableMånedPeriode: NullableMånedPeriode): Boolean {
        if (nullableMånedPeriode.fom == null) {
            throw Feil("Fom ble null ved sjekk av overlapp av periode til endretUtbetalingAndel")
        }

        return MånedPeriode(
            this.periode.fom,
            this.periode.tom
        ).overlapperHeltEllerDelvisMed(
            MånedPeriode(
                nullableMånedPeriode.fom,
                nullableMånedPeriode.tom ?: TIDENES_ENDE.toYearMonth()
            )
        )
    }
}

fun List<MinimertEndretUtbetalingAndel>.somOverlapper(nullableMånedPeriode: NullableMånedPeriode) =
    this.filter { it.erOverlappendeMed(nullableMånedPeriode) }

data class BegrunnelseGrunnlag2(

    val persongrunnlag: PersonopplysningGrunnlag,
    val vilkårsvurdering: Vilkårsvurdering,
    val endredeUtbetalingAndeler: List<EndretUtbetalingAndel>,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
)

fun EndretUtbetalingAndel.tilMinimertEndretUtbetalingAndel() = MinimertEndretUtbetalingAndel(
    periode = this.periode,
    personIdent = this.person?.personIdent?.ident ?: throw Feil(
        "Har ikke ident på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertEndretUtbetalingsandel"
    ),
    årsak = this.årsak ?: throw Feil(
        "Har ikke årsak på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertEndretUtbetalingsandel"
    )
)

