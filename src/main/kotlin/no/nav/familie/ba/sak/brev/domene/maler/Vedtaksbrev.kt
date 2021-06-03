package no.nav.familie.ba.sak.brev.domene.maler

interface Vedtaksbrev : Brev {

    override val type: Vedtaksbrevtype
    override val data: VedtaksbrevData
}

interface VedtaksbrevData : BrevData {

    val perioder: List<BrevPeriode>
}

enum class BrevPeriodeType(val apiNavn: String) {
    INNVILGELSE("innvilgelse"),
    OPPHOR("opphor"),
    AVSLAG("avslag"),
    AVSLAG_UTEN_PERIODE("avslagUtenPeriode"),
    FORTSATT_INNVILGET("fortsattInnvilget"),
}

data class VedtakFellesfelter(
        val enhet: String,
        val saksbehandler: String,
        val beslutter: String,
        val hjemmeltekst: Hjemmeltekst,
        val søkerNavn: String,
        val søkerFødselsnummer: String,
        val perioder: List<BrevPeriode>
)