package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class HenleggeTrukketSøknadBrev(
        override val type: BrevType = BrevType.HENLEGGE_TRUKKET_SØKNAD,
        override val data: HenleggeTrukketSøknadData
) : Brev

data class HenleggeTrukketSøknadData(
        override val delmalData: DelmalData,
        override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            // TODO: Fjern etter at brevOpprettetDato er lagt til i familie brev. dato -> brevOpprettetDato
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    ) {

        constructor(navn: String, fodselsnummer: String) : this(navn = flettefelt(navn),
                                                                fodselsnummer = flettefelt(fodselsnummer))
    }

    data class DelmalData(
            val signatur: SignaturDelmal
    )
}