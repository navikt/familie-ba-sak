package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class AutovedtakSvalbardtillegg(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_SVALBARDTILLEGG,
    override val data: AutovedtakSvalbardtilleggData,
) : Vedtaksbrev {
    constructor(vedtakFellesfelter: VedtakFellesfelter) :
        this(
            data =
                AutovedtakSvalbardtilleggData(
                    delmalData =
                        AutovedtakSvalbardtilleggData.Delmaler(
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

data class AutovedtakSvalbardtilleggData(
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
