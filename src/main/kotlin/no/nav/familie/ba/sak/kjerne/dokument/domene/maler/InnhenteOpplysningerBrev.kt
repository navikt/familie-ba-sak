package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InnhenteOpplysningerBrev(
        override val mal: Brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
        override val data: InnhenteOpplysningerData
) : Brev

data class InnhenteOpplysningerData(
        override val delmalData: DelmalData,
        override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
            override val navn: Flettefelt,
            override val fodselsnummer: Flettefelt,
            override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            val dokumentliste: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(navn: String,
                    fodselsnummer: String,
                    dokumentliste: List<String>) : this(navn = flettefelt(navn),
                                                        fodselsnummer = flettefelt(fodselsnummer),
                                                        dokumentliste = flettefelt(dokumentliste))
    }

    data class DelmalData(
            val signatur: SignaturDelmal
    )
}