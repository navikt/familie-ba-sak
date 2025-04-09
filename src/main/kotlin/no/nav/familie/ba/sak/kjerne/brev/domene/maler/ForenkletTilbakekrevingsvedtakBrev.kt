package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class ForenkletTilbakekrevingsvedtakBrev(
    override val mal: Brevmal,
    override val data: ForenkletTilbakekrevingsvedtakBrevData,
) : Brev

data class ForenkletTilbakekrevingsvedtakBrevData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
    val fritekst: String,
) : BrevData {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) : FlettefelterForDokument {
        constructor(
            navn: String,
            fodselsnummer: String,
            brevOpprettetDato: LocalDate,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            brevOpprettetDato = flettefelt(brevOpprettetDato.tilDagMånedÅr()),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
