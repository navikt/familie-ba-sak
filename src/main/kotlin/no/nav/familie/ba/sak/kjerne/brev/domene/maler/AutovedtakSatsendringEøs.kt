package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class AutovedtakSatsendringEøs(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_SATSENDRING_EØS,
    override val data: AutovedtakSatsendringEøsData,
) : Vedtaksbrev {
    constructor(vedtakFellesfelter: VedtakFellesfelter) :
        this(
            data =
                AutovedtakSatsendringEøsData(
                    delmalData =
                        AutovedtakSatsendringEøsData.Delmaler(
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            autoUnderskrift = AutoUnderskrift(enhet = vedtakFellesfelter.enhet),
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

data class AutovedtakSatsendringEøsData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>,
    override val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
) : VedtaksbrevStandardData {
    data class Delmaler(
        val hjemmeltekst: Hjemmeltekst,
        val autoUnderskrift: AutoUnderskrift,
    )
}
