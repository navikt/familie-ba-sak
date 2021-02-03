package no.nav.familie.ba.sak.brev.domene.maler

interface Vedtaksbrev {

    val type: VedtaksbrevType
    val data: VedtaksbrevData
}

enum class VedtaksbrevType(val apiNavn: String, val visningsTekst: String) {
    INNVILGET("vedtakInnvilgelse", "Invilget"),
}

interface VedtaksbrevData : BrevData {

    val perioder: Perioder
}

typealias Perioder = List<BrevPeriode>

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
                tom: String,
                belop: String,
                antallBarn: String,
                barnasFodselsdager: String,
                begrunnelser: List<String>,
                type: String) : this(
            fom = flettefelt(fom),
            tom = flettefelt(tom),
            belop = flettefelt(belop),
            antallBarn = flettefelt(antallBarn),
            barnasFodselsdager = flettefelt(barnasFodselsdager),
            begrunnelser = flettefelt(begrunnelser),
            type = flettefelt(type),
    )
}

/*
fun duFaarArrayTilPerioder(duFaar: List<DuFårSeksjon>): Perioder {
    val perioder = mutableMapOf<VedtakBegrunnelseType, Periode>()
    duFaar.forEach { perioder[it.begrunnelseType] = duFaarTilPeriode(it) }
    return perioder
}

fun duFaarTilPeriode(duFaarSeksjon: DuFårSeksjon): Periode {
    // TODO Legg inn funksjonallitet for hva som skal skje dersom det ikke er noen tom dato
    return Periode(fom = flettefelt(duFaarSeksjon.fom),
                   tom = flettefelt(duFaarSeksjon.tom ?: ""),
                   belop = flettefelt(duFaarSeksjon.belop),
                   antallBarn = flettefelt(duFaarSeksjon.antallBarn.toString()),
                   barnasFodselsdager = flettefelt(duFaarSeksjon.barnasFodselsdatoer),
                   begrunnelser = duFaarSeksjon.begrunnelser)
}/*