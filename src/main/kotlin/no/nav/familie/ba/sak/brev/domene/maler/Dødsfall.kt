package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class Dødsfall(
        override val type: BrevType = EnkelBrevtype.DØDSFALL,
        override val data: DødsfallData
) : Brev

data class DødsfallData(
        override val delmalData: DelmalData,
        override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            // TODO: Fjern etter at brevOpprettetDato er lagt til i familie brev. dato -> brevOpprettetDato
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            val virkningstidspunkt: Flettefelt,
            val navnAvdode: Flettefelt,
    ) {

        constructor(navn: String,
                    fodselsnummer: String,
                    virkningstidspunkt: String,
                    navnAvdode: String) : this(navn = flettefelt(navn),
                                               fodselsnummer = flettefelt(fodselsnummer),
                                               virkningstidspunkt = flettefelt(virkningstidspunkt),
                                               navnAvdode = flettefelt(navnAvdode))
    }

    data class DelmalData(
            val signaturVedtak: SignaturVedtak
    )
}