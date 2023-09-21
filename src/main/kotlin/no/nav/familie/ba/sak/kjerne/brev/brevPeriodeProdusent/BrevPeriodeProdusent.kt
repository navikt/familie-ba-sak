package no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent

import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.lagBrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun VedtaksperiodeMedBegrunnelser.lagBrevPeriode(
    grunnlagForBegrunnelse: GrunnlagForBegrunnelse,
): BrevPeriode? {
    val standardbegrunnelser =
        this.begrunnelser.map { it.standardbegrunnelse.lagBrevBegrunnelse(this, grunnlagForBegrunnelse) }
    val eøsBegrunnelser = emptyList<BegrunnelseData>()
    val fritekster = this.fritekster.map { FritekstBegrunnelse(it.fritekst) }

    val begrunnelserOgFritekster =
        standardbegrunnelser + eøsBegrunnelser + fritekster

    if (begrunnelserOgFritekster.isEmpty()) return null

    return this.byggBrevPeriode(
        begrunnelserOgFritekster = begrunnelserOgFritekster,
    )
}

private fun VedtaksperiodeMedBegrunnelser.byggBrevPeriode(
    begrunnelserOgFritekster: List<BrevBegrunnelse>,
): BrevPeriode {
    return BrevPeriode(

        fom = "",
        tom = "",
        belop = "",
        begrunnelser = begrunnelserOgFritekster,
        brevPeriodeType = BrevPeriodeType.INNVILGELSE,
        antallBarn = "",
        barnasFodselsdager = "",
        antallBarnMedUtbetaling = "",
        antallBarnMedNullutbetaling = "",
        fodselsdagerBarnMedUtbetaling = "",
        fodselsdagerBarnMedNullutbetaling = "",
        duEllerInstitusjonen = "",
    )
}
