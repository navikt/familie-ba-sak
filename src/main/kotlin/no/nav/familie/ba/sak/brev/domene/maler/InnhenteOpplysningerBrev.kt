package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InnhenteOpplysningerBrev(
        override val type: BrevType = EnkelBrevtype.INNHENTE_OPPLYSNINGER,
        override val data: InnhenteOpplysningerData
) : Brev

data class InnhenteOpplysningerData(
        override val delmalData: DelmalData,
        override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            // TODO: Fjern etter at brevOpprettetDato er lagt til i familie brev. dato -> brevOpprettetDato
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