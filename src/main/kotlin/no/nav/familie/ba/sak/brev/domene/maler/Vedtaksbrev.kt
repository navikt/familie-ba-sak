package no.nav.familie.ba.sak.brev.domene.maler

interface Vedtaksbrev : Brev {

    override val type: Vedtaksbrevtype
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevData {

    val perioder: Perioder
}

typealias Perioder = List<BrevPeriode>

enum class PeriodeType(val apiNavn: String) {
    INNVILGELSE("innvilgelse"),
    OPPHOR("opphor"),
    AVSLAG("avslag"),
    AVSLAG_UTEN_PERIODE("avslagUtenPeriode"),
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

    constructor(fom: String? = null,
                tom: String? = null,
                belop: String? = null,
                antallBarn: String? = null,
                barnasFodselsdager: String? = null,
                begrunnelser: List<String>,
                type: PeriodeType) : this(
            fom = flettefelt(if (fom.isNullOrBlank()) "" else "$fom"),
            tom = flettefelt(if (tom.isNullOrBlank()) "" else "til og med $tom "),
            belop = flettefelt(belop),
            antallBarn = flettefelt(antallBarn),
            barnasFodselsdager = flettefelt(barnasFodselsdager),
            begrunnelser = flettefelt(begrunnelser),
            type = flettefelt(type.apiNavn),
    )
}

data class VedtakFellesfelter(
        val enhet: String,
        val saksbehandler: String,
        val beslutter: String,
        val hjemmeltekst: Hjemmeltekst,
        val søkerNavn: String,
        val søkerFødselsnummer: String,
        val perioder: Perioder
)