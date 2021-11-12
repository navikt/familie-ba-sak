package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.BrevPeriode

data class Autovedtak6eller18år(
    override val mal: Brevmal = Brevmal.AUTOVEDTAK_BARN_6_ELLER_18_ÅR,
    override val data: Autovedtak6og18årData
) : Vedtaksbrev {

    constructor(
        vedtakFellesfelter: VedtakFellesfelter,
    ) :
        this(
            data = Autovedtak6og18årData(
                delmalData = Autovedtak6og18årData.Delmaler(
                    hjemmeltekst = vedtakFellesfelter.hjemmeltekst,
                ),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = vedtakFellesfelter.søkerNavn,
                    fodselsnummer = vedtakFellesfelter.søkerFødselsnummer
                ),
                perioder = vedtakFellesfelter.perioder
            )
        )
}

data class Autovedtak6og18årData(
    override val delmalData: Delmaler,
    override val flettefelter: FlettefelterForDokumentImpl,
    override val perioder: List<BrevPeriode>
) : VedtaksbrevData {

    data class Delmaler(
        val hjemmeltekst: Hjemmeltekst,
    )
}
