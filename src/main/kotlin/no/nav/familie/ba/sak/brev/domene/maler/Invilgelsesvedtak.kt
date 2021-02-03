package no.nav.familie.ba.sak.brev.domene.maler

data class Innvilgelsesvedtak(
        override val type: VedtaksbrevType = VedtaksbrevType.INNVILGET, override val data: InnvilgelsesvedtakData
) : Vedtaksbrev

data class InnvilgelsesvedtakData(
        override val delmalData: Delmaler,
        override val flettefelter: Flettefelter,
        override val perioder: Perioder,
) : VedtaksbrevData{
    data class Flettefelter(
            val navn: Flettefelt,
            val fødselsnummer: Flettefelt,
            val dato: Flettefelt,
            val hjemler: Flettefelt,
    )
    data class Delmaler(
            val signaturVedtak: SignaturVedtatk,
            val etterbetaling: Etterbetaling?,
    )
}