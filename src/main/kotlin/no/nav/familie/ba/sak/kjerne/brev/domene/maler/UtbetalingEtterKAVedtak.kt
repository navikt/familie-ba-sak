package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class UtbetalingEtterKAVedtak(
    override val mal: Brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
    override val data: UtbetalingEtterKAVedtakData,
) : Brev

data class UtbetalingEtterKAVedtakData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
    val fritekst: Flettefelt,
) : BrevData {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        override val organisasjonsnummer: Flettefelt,
        override val gjelder: Flettefelt,
    ) : FlettefelterForDokument {
        constructor(
            navn: String,
            fodselsnummer: String,
            organisasjonsnummer: String? = null,
            gjelder: String? = null,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            organisasjonsnummer = flettefelt(organisasjonsnummer),
            gjelder = flettefelt(gjelder),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
