package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class Autovedtak6og18årOgSmåbarnstillegg(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
    override val data: Autovedtak6Og18ÅrData,
) : Vedtaksbrev {
    constructor(
        vedtakFellesfelter: VedtakFellesfelter,
    ) :
        this(
            data =
                Autovedtak6Og18ÅrData(
                    delmalData =
                        Autovedtak6Og18ÅrData.Delmaler(
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            autoUnderskrift =
                                AutoUnderskrift(
                                    enhet = vedtakFellesfelter.enhet,
                                ),
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

data class Autovedtak6Og18ÅrData(
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
