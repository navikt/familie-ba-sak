package no.nav.familie.ba.sak.brev.domene.maler

interface Vedtaksbrev: Brev {

    override val type: BrevType
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevData {

    val perioder: Perioder
}

typealias Perioder = List<BrevPeriode>

enum class PeriodeType(val apiNavn: String){
    INNVILGELSE("innvilgelse"),
    OPPHOØR("opphor"),
}

data class BrevPeriode(
        val fom: Flettefelt,
        val tom: Flettefelt,
        val belop: Flettefelt,
        val antallBarn: Flettefelt,
        val barnasFodselsdager: Flettefelt,
        val begrunnelser: Flettefelt,
        val type: Flettefelt
) {

    constructor(fom: String,
                tom: String?,
                belop: String,
                antallBarn: String,
                barnasFodselsdager: String,
                begrunnelser: List<String>,
                type: PeriodeType) : this(
            fom = flettefelt(fom),
            tom = flettefelt(if (tom.isNullOrBlank()) "" else "til og med $tom "),
            belop = flettefelt(belop),
            antallBarn = flettefelt(antallBarn),
            barnasFodselsdager = flettefelt(barnasFodselsdager),
            begrunnelser = flettefelt(begrunnelser),
            type = flettefelt(type.apiNavn),
    )
}