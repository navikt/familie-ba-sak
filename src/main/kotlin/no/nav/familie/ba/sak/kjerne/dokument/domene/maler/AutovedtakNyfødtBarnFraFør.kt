package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

data class AutovedtakNyfødtBarnFraFør(
        override val mal: Brevmal = Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
        override val data: AutovedtakNyfødtBarnFraFørData
) : Vedtaksbrev {

    constructor(
            vedtakFellesfelter: VedtakFellesfelter,
            etterbetaling: Etterbetaling?,
    ) :
            this(data = AutovedtakNyfødtBarnFraFørData(
                    delmalData = AutovedtakNyfødtBarnFraFørData.Delmaler(
                            etterbetaling = etterbetaling,
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            medVennilgHilsen = MedVennilgHilsen(vedtakFellesfelter.enhet)
                    ),
                    flettefelter = FlettefelterForDokumentImpl(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer),
                    perioder = vedtakFellesfelter.perioder)
            )

}

data class AutovedtakNyfødtBarnFraFørData(
        override val delmalData: Delmaler,
        override val flettefelter: FlettefelterForDokumentImpl,
        override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
            val etterbetaling: Etterbetaling?,
            val hjemmeltekst: Hjemmeltekst,
            val medVennilgHilsen: MedVennilgHilsen
    )
}