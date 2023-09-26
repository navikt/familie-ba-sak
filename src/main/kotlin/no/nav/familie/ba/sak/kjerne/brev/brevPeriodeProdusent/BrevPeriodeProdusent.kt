package no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent

import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.lagBrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.finnBegrunnelseGrunnlagPerPerson
import lagBrevBegrunnelse

fun VedtaksperiodeMedBegrunnelser.lagBrevPeriode(
    grunnlagForBegrunnelse: GrunnlagForBegrunnelse,
    landkoder: Map<String, String>,
): BrevPeriode? {
    val begrunnelsesGrunnlagPerPerson = this.finnBegrunnelseGrunnlagPerPerson(grunnlagForBegrunnelse)

    val standardbegrunnelser =
        this.begrunnelser.map {
            it.standardbegrunnelse.lagBrevBegrunnelse(
                this,
                grunnlagForBegrunnelse,
                begrunnelsesGrunnlagPerPerson,
            )
        }

    val eøsBegrunnelser =
        this.eøsBegrunnelser.flatMap {
            it.begrunnelse.lagBrevBegrunnelse(
                this,
                grunnlagForBegrunnelse,
                begrunnelsesGrunnlagPerPerson,
                landkoder,
            )
        }

    val fritekster = this.fritekster.map { FritekstBegrunnelse(it.fritekst) }

    val barnMedUtbetaling = begrunnelsesGrunnlagPerPerson.finnBarnMedUtbetaling()

    val begrunnelserOgFritekster =
        standardbegrunnelser + eøsBegrunnelser + fritekster

    if (begrunnelserOgFritekster.isEmpty()) return null

    return this.byggBrevPeriode(
        begrunnelserOgFritekster = begrunnelserOgFritekster,
        antallBarn = barnMedUtbetaling.size.toString(),
    )
}

private fun Map<Person, IBegrunnelseGrunnlagForPeriode>.finnBarnMedUtbetaling() =
    filterKeys { it.type == PersonType.BARN }
        .filterValues {
            val endretUtbetalingAndelIPeriodeErDeltBosted =
                it.dennePerioden.endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED
            val utbetalingssumIPeriode = it.dennePerioden.andeler.sumOf { andel -> andel.kalkulertUtbetalingsbeløp }

            utbetalingssumIPeriode != 0 || endretUtbetalingAndelIPeriodeErDeltBosted
        }

private fun VedtaksperiodeMedBegrunnelser.byggBrevPeriode(
    begrunnelserOgFritekster: List<BrevBegrunnelse>,
    antallBarn: String,
): BrevPeriode {
    return BrevPeriode(

        fom = "",
        tom = "",
        belop = "",
        begrunnelser = begrunnelserOgFritekster,
        brevPeriodeType = BrevPeriodeType.INNVILGELSE,
        antallBarn = antallBarn,
        barnasFodselsdager = "",
        antallBarnMedUtbetaling = "",
        antallBarnMedNullutbetaling = "",
        fodselsdagerBarnMedUtbetaling = "",
        fodselsdagerBarnMedNullutbetaling = "",
        duEllerInstitusjonen = "",
    )
}
