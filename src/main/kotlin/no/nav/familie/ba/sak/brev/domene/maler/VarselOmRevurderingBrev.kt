package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class VarselOmRevurderingBrev(
        override val type: BrevType = EnkelBrevtype.VARSEL_OM_REVURDERING,
        override val data: VarselOmRevurderingData
) : Brev

data class VarselOmRevurderingData(
        override val delmalData: DelmalData,
        override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            // TODO: Fjern etter at brevOpprettetDato er lagt til i familie brev. dato -> brevOpprettetDato
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            // TODO: expand and contract varselÅrsaker -> varselAarsaker
            val varselÅrsaker: Flettefelt,
            val varselAarsaker: Flettefelt,
    ) {

        constructor(navn: String,
                    fodselsnummer: String,
                    varselÅrsaker: List<String>) : this(
                navn = flettefelt(navn),
                fodselsnummer = flettefelt(fodselsnummer),
                varselÅrsaker = flettefelt(varselÅrsaker),
                varselAarsaker = flettefelt(varselÅrsaker),
        )
    }

    data class DelmalData(
            val signatur: SignaturDelmal
    )
}