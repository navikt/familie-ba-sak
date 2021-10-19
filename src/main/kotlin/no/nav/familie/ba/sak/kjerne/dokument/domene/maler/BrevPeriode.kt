package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.BrevPeriode.Companion.BEGRUNNELSE_ERROR_MSG
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

data class InnvilgelseBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,
) : BrevPeriode {

    constructor(
        fom: String,
        tom: String? = null,
        belop: String,
        antallBarn: String,
        barnasFodselsdager: String,
        begrunnelser: List<Begrunnelse>,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
        belop = flettefelt(belop),
        antallBarn = flettefelt(antallBarn),
        barnasFodselsdager = flettefelt(barnasFodselsdager),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(BrevPeriodeType.INNVILGELSE.apiNavn),
    )
}

data class EndretUtbetalingBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,
    val typeBarnetrygd: Flettefelt,
) : BrevPeriode {

    constructor(
        fom: String,
        tom: String? = null,
        belop: String,
        antallBarn: String,
        barnasFodselsdager: String,
        begrunnelser: List<Begrunnelse>,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
        belop = flettefelt(belop),
        antallBarn = flettefelt(antallBarn),
        barnasFodselsdager = flettefelt(barnasFodselsdager),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(BrevPeriodeType.ENDRET_UTBETALINGSPERIODE.apiNavn),
        typeBarnetrygd = flettefelt("ordinær") // TODO fiks
    )
}

data class OpphørBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,
) : BrevPeriode {

    constructor(
        fom: String,
        tom: String? = null,
        begrunnelser: List<Begrunnelse>,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
        belop = flettefelt(null),
        antallBarn = flettefelt(null),
        barnasFodselsdager = flettefelt(null),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(BrevPeriodeType.OPPHOR.apiNavn),
    )
}

data class AvslagBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,
) : BrevPeriode {

    constructor(
        fom: String,
        tom: String? = null,
        begrunnelser: List<Begrunnelse>,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
        belop = flettefelt(null),
        antallBarn = flettefelt(null),
        barnasFodselsdager = flettefelt(null),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(BrevPeriodeType.AVSLAG.apiNavn),
    )
}

data class AvslagUtenPeriodeBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,
) : BrevPeriode {

    constructor(begrunnelser: List<Begrunnelse>) : this(
        fom = flettefelt(null),
        tom = flettefelt(null),
        belop = flettefelt(null),
        antallBarn = flettefelt(null),
        barnasFodselsdager = flettefelt(null),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(BrevPeriodeType.AVSLAG_UTEN_PERIODE.apiNavn),
    )
}

data class FortsattInnvilgetBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,
) : BrevPeriode {

    constructor(
        fom: String?,
        belop: String,
        antallBarn: String,
        barnasFodselsdager: String,
        begrunnelser: List<Begrunnelse>,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(null),
        belop = flettefelt(belop),
        antallBarn = flettefelt(antallBarn),
        barnasFodselsdager = flettefelt(barnasFodselsdager),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(BrevPeriodeType.FORTSATT_INNVILGET.apiNavn),
    )
}
