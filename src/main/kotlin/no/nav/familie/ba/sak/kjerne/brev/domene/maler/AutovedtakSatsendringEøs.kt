package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class AutovedtakSatsendringEøs(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_SATSENDRING_EØS,
    override val data: AutovedtakSatsendringEøsData,
) : Vedtaksbrev {
    constructor(
        delmalData: AutovedtakSatsendringEøsData.Delmaler,
        flettefelter: FlettefelterForDokumentImpl,
        utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
    ) : this(
        data =
            AutovedtakSatsendringEøsData(
                delmalData = delmalData,
                flettefelter = flettefelter,
                utbetalingerPerMndEøs = utbetalingerPerMndEøs,
            ),
    )
}

data class AutovedtakSatsendringEøsData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
) : VedtaksbrevData {
    data class Delmaler(
        val autoUnderskrift: AutoUnderskrift,
    )
}
