package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

data class AutovedtakNyfødtFørsteBarn(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
    override val data: AutovedtakNyfødtFørsteBarnData
) : Vedtaksbrev {

    constructor(
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling?,
    ) :
        this(
            data = AutovedtakNyfødtFørsteBarnData(
                delmalData = AutovedtakNyfødtFørsteBarnData.Delmaler(
                    etterbetaling = etterbetaling,
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                    medVennilgHilsen = MedVennilgHilsen(vedtakFellesfelter.enhet)
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = vedtakFellesfelter.søkerNavn,
                    fodselsnummer = vedtakFellesfelter.søkerFødselsnummer
                ),
                perioder = vedtakFellesfelter.perioder
            )
        )
}

data class AutovedtakNyfødtFørsteBarnData(
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
