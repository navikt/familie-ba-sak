package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

interface BrevPeriode {

    val fom: Flettefelt
    val tom: Flettefelt
    val belop: Flettefelt
    val antallBarn: Flettefelt
    val barnasFodselsdager: Flettefelt
    val begrunnelser: Flettefelt
    val type: Flettefelt
}

data class InnvilgelseBrevPeriode(
        override val fom: Flettefelt,
        override val tom: Flettefelt,
        override val belop: Flettefelt,
        override val antallBarn: Flettefelt,
        override val barnasFodselsdager: Flettefelt,
        override val begrunnelser: Flettefelt,
        override val type: Flettefelt,
) : BrevPeriode {

    constructor(
            fom: String,
            tom: String? = null,
            belop: String,
            antallBarn: String,
            barnasFodselsdager: String,
            begrunnelser: List<String>,
    ) : this(
            fom = flettefelt(fom),
            tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
            belop = flettefelt(belop),
            antallBarn = flettefelt(antallBarn),
            barnasFodselsdager = flettefelt(barnasFodselsdager),
            begrunnelser = flettefelt(begrunnelser),
            type = flettefelt(BrevPeriodeType.INNVILGELSE.apiNavn),
    )
}

data class OpphørBrevPeriode(
        override val fom: Flettefelt,
        override val tom: Flettefelt,
        override val belop: Flettefelt,
        override val antallBarn: Flettefelt,
        override val barnasFodselsdager: Flettefelt,
        override val begrunnelser: Flettefelt,
        override val type: Flettefelt,
) : BrevPeriode {

    constructor(
            fom: String,
            tom: String? = null,
            begrunnelser: List<String>,
    ) : this(
            fom = flettefelt(fom),
            tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
            belop = flettefelt(null),
            antallBarn = flettefelt(null),
            barnasFodselsdager = flettefelt(null),
            begrunnelser = flettefelt(begrunnelser),
            type = flettefelt(BrevPeriodeType.OPPHOR.apiNavn),
    )
}

data class AvslagBrevPeriode(
        override val fom: Flettefelt,
        override val tom: Flettefelt,
        override val belop: Flettefelt,
        override val antallBarn: Flettefelt,
        override val barnasFodselsdager: Flettefelt,
        override val begrunnelser: Flettefelt,
        override val type: Flettefelt,
) : BrevPeriode {

    constructor(
            fom: String,
            tom: String? = null,
            begrunnelser: List<String>,
    ) : this(
            fom = flettefelt(fom),
            tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
            belop = flettefelt(null),
            antallBarn = flettefelt(null),
            barnasFodselsdager = flettefelt(null),
            begrunnelser = flettefelt(begrunnelser),
            type = flettefelt(BrevPeriodeType.AVSLAG.apiNavn),
    )
}

data class AvslagUtenPeriodeBrevPeriode(
        override val fom: Flettefelt,
        override val tom: Flettefelt,
        override val belop: Flettefelt,
        override val antallBarn: Flettefelt,
        override val barnasFodselsdager: Flettefelt,
        override val begrunnelser: Flettefelt,
        override val type: Flettefelt,
) : BrevPeriode {

    constructor(begrunnelser: List<String>) : this(
            fom = flettefelt(null),
            tom = flettefelt(null),
            belop = flettefelt(null),
            antallBarn = flettefelt(null),
            barnasFodselsdager = flettefelt(null),
            begrunnelser = flettefelt(begrunnelser),
            type = flettefelt(BrevPeriodeType.AVSLAG_UTEN_PERIODE.apiNavn),
    )
}


data class FortsattInnvilgetBrevPeriode(
        override val fom: Flettefelt,
        override val tom: Flettefelt,
        override val belop: Flettefelt,
        override val antallBarn: Flettefelt,
        override val barnasFodselsdager: Flettefelt,
        override val begrunnelser: Flettefelt,
        override val type: Flettefelt,
) : BrevPeriode {

    constructor(
            fom: String?,
            belop: String,
            antallBarn: String,
            barnasFodselsdager: String,
            begrunnelser: List<String>,
    ) : this(
            fom = flettefelt(fom),
            tom = flettefelt(null),
            belop = flettefelt(belop),
            antallBarn = flettefelt(antallBarn),
            barnasFodselsdager = flettefelt(barnasFodselsdager),
            begrunnelser = flettefelt(begrunnelser),
            type = flettefelt(BrevPeriodeType.FORTSATT_INNVILGET.apiNavn),
    )
}
