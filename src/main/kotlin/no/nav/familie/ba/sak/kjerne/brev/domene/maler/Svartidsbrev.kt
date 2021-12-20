package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class Svartidsbrev(
    override val mal: Brevmal = Brevmal.SVARTIDSBREV,
    override val data: SvartidsbrevData
) : Brev {

    constructor(
        navn: String,
        fodselsnummer: String,
        enhet: String,
    ) : this(
        data = SvartidsbrevData(
            flettefelter = SvartidsbrevData.Flettefelter(
                navn = navn,
                fodselsnummer = fodselsnummer,
            ),
            delmalData = SvartidsbrevData.DelmalData(
                SignaturDelmal(
                    enhet = enhet
                )
            )
        )
    )
}

data class SvartidsbrevData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal
    )
}
