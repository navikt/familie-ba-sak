package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InnhenteOpplysningerBrev(
        override val brevType: BrevType = BrevType.INNHENTE_OPPLYSNINGER,
        override val brevData: InnhenteOpplysningerData
) : Brev

data class InnhenteOpplysningerData(
        override val delmalData: DelmalData,
        override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            val dokumentliste: Flettefelt,
    ) {

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