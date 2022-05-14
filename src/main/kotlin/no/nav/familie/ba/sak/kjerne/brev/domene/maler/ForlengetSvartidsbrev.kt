package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class ForlengetSvartidsbrev(
    override val mal: Brevmal = Brevmal.FORLENGET_SVARTIDSBREV,
    override val data: ForlengetSvartidsbrevData
) : Brev {
    constructor(
        navn: String,
        fodselsnummer: String,
        enhetNavn: String,
        årsaker: List<String>,
        antallUkerSvarfrist: Int,
    ) : this(
        data = ForlengetSvartidsbrevData(
            delmalData = ForlengetSvartidsbrevData.DelmalData(signatur = SignaturDelmal(enhet = enhetNavn)),
            flettefelter = ForlengetSvartidsbrevData.Flettefelter(
                navn = flettefelt(navn),
                fodselsnummer = flettefelt(fodselsnummer),
                antallUkerSvarfrist = flettefelt(antallUkerSvarfrist.toString()),
                aarsakerSvartidsbrev = flettefelt(årsaker)
            )
        )
    )
}

data class ForlengetSvartidsbrevData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter
) : BrevData {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val antallUkerSvarfrist: Flettefelt,
        val aarsakerSvartidsbrev: Flettefelt,
    ) : FlettefelterForDokument

    data class DelmalData(
        val signatur: SignaturDelmal
    )
}
