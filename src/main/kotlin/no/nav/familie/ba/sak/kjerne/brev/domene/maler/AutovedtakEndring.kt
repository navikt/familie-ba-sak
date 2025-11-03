package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class AutovedtakEndring(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_ENDRING,
    override val data: AutovedtakEndringData,
) : Vedtaksbrev {
    constructor(
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling? = null,
    ) :
        this(
            data =
                AutovedtakEndringData(
                    delmalData =
                        AutovedtakEndringData.Delmaler(
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            autoUnderskrift =
                                AutoUnderskrift(
                                    enhet = vedtakFellesfelter.enhet,
                                ),
                            etterbetaling = etterbetaling,
                        ),
                    flettefelter =
                        FlettefelterForDokumentImpl(
                            navn = vedtakFellesfelter.søkerNavn,
                            fodselsnummer = vedtakFellesfelter.søkerFødselsnummer,
                        ),
                    perioder = vedtakFellesfelter.perioder,
                ),
        )
}

data class AutovedtakEndringData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>,
    override val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
) : VedtaksbrevStandardData {
    data class Delmaler(
        val hjemmeltekst: Hjemmeltekst,
        val autoUnderskrift: AutoUnderskrift,
        val etterbetaling: Etterbetaling?,
    )
}
