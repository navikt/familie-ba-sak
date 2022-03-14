package no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Flettefelt
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse

interface BrevPeriode {

    val fom: Flettefelt
    val tom: Flettefelt
    val belop: Flettefelt
    val antallBarn: Flettefelt
    val barnasFodselsdager: Flettefelt
    val begrunnelser: List<Any>
    val type: Flettefelt

    companion object {

        const val BEGRUNNELSE_ERROR_MSG = "Begrunnelse er ikke string eller begrunnelseData"
    }
}

// Kan omdøpes til BrevPeriode når alle perioder    bruker denne klassen
data class GenerellBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,

    val antallBarnMedUtbetaling: Flettefelt,
    val antallBarnUtenUtbetaling: Flettefelt,
    val fodselsdagerBarnUtenUtbetaling: Flettefelt,
    val fodselsdagerBarnMedUtbetaling: Flettefelt,
) : BrevPeriode {

    constructor(
        fom: String,
        tom: String,
        belop: String,
        begrunnelser: List<Begrunnelse>,
        brevPeriodeType: BrevPeriodeType,
        antallBarn: String,
        barnasFodselsdager: String,
        antallBarnMedUtbetaling: String,
        antallBarnUtenUtbetaling: String,
        fodselsdagerBarnUtenUtbetaling: String,
        fodselsdagerBarnMedUtbetaling: String
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(tom),
        belop = flettefelt(belop),
        antallBarn = flettefelt(antallBarn),
        barnasFodselsdager = flettefelt(barnasFodselsdager),
        antallBarnMedUtbetaling = flettefelt(antallBarnMedUtbetaling),
        antallBarnUtenUtbetaling = flettefelt(antallBarnUtenUtbetaling),
        fodselsdagerBarnUtenUtbetaling = flettefelt(fodselsdagerBarnUtenUtbetaling),
        fodselsdagerBarnMedUtbetaling = flettefelt(fodselsdagerBarnMedUtbetaling),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BrevPeriode.BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(brevPeriodeType.apiNavn),
    )
}
