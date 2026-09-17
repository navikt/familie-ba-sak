package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.utbetalingEøs.UtbetalingMndEøs

data class AutovedtakSøknad(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_SØKNAD,
    override val data: AutovedtakSøknadData,
) : Vedtaksbrev {
    constructor(
        vedtakFellesfelter: VedtakFellesfelter,
        etterbetaling: Etterbetaling?,
    ) :
        this(
            data =
                AutovedtakSøknadData(
                    delmalData =
                        AutovedtakSøknadData.Delmaler(
                            etterbetaling = etterbetaling,
                            hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                            autoUnderskrift = AutoUnderskrift(vedtakFellesfelter.enhet),
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

data class AutovedtakSøknadData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>,
    override val utbetalingerPerMndEøs: Map<String, UtbetalingMndEøs>? = null,
) : VedtaksbrevStandardData {
    data class Delmaler(
        val etterbetaling: Etterbetaling?,
        val hjemmeltekst: Hjemmeltekst,
        val autoUnderskrift: AutoUnderskrift,
    )
}
