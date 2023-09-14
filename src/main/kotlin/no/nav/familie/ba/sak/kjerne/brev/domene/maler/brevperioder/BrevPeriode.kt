package no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Flettefelt
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse

data class BrevPeriode(
    val fom: Flettefelt,
    val tom: Flettefelt,
    val belop: Flettefelt,
    val antallBarn: Flettefelt,
    val barnasFodselsdager: Flettefelt,
    val begrunnelser: List<Any>,
    val type: Flettefelt,

    val antallBarnMedUtbetaling: Flettefelt,
    val antallBarnMedNullutbetaling: Flettefelt,
    val fodselsdagerBarnMedUtbetaling: Flettefelt,
    val fodselsdagerBarnMedNullutbetaling: Flettefelt,
) {

    constructor(
        fom: String,
        tom: String,
        belop: String,
        begrunnelser: List<BrevBegrunnelse>,
        brevPeriodeType: BrevPeriodeType,
        antallBarn: String,
        barnasFodselsdager: String,
        antallBarnMedUtbetaling: String,
        antallBarnMedNullutbetaling: String,
        fodselsdagerBarnMedUtbetaling: String,
        fodselsdagerBarnMedNullutbetaling: String,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(tom),
        belop = flettefelt(belop),
        antallBarn = flettefelt(antallBarn),
        barnasFodselsdager = flettefelt(barnasFodselsdager),
        antallBarnMedUtbetaling = flettefelt(antallBarnMedUtbetaling),
        antallBarnMedNullutbetaling = flettefelt(antallBarnMedNullutbetaling),
        fodselsdagerBarnMedUtbetaling = flettefelt(fodselsdagerBarnMedUtbetaling),
        fodselsdagerBarnMedNullutbetaling = flettefelt(fodselsdagerBarnMedNullutbetaling),
        begrunnelser = begrunnelser,
        type = flettefelt(brevPeriodeType.apiNavn),
    )
}
