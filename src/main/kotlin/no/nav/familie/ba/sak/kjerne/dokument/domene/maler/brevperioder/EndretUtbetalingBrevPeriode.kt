package no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.EndretUtbetalingBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Flettefelt
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse

data class EndretUtbetalingBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,
    override val typeBarnetrygd: Flettefelt,
) : IEndretUtbetalingBrevPeriode {

    constructor(
        fom: String,
        tom: String? = null,
        barnasFodselsdager: String,
        begrunnelser: List<Begrunnelse>,
        type: EndretUtbetalingBrevPeriodeType,
        typeBarnetrygd: EndretUtbetalingBarnetrygdType,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
        belop = flettefelt(null),
        antallBarn = flettefelt(null),
        barnasFodselsdager = flettefelt(barnasFodselsdager),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BrevPeriode.BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(type.apiNavn),
        typeBarnetrygd = flettefelt("${typeBarnetrygd.navn} ")
    )
}

enum class EndretUtbetalingBarnetrygdType(val navn: String) {
    DELT("delt"),
    DELT_UTVIDET_NB("delt utvidet"),
    DELT_UTVIDET_NN("delt utvida"),
}
