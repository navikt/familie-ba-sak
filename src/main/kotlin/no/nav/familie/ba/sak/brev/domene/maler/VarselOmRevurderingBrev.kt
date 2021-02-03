package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class VarselOmRevurderingBrev(
        override val brevType: BrevType = BrevType.VARSEL_OM_REVURDERING,
        override val brevData: VarselOmRevurderingData
) : Brev

data class VarselOmRevurderingData(
        override val delmalData: DelmalData,
        override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
            val navn: Flettefelt,
            val fodselsnummer: Flettefelt,
            val dato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            val varselÅrsaker: Flettefelt,
    ) {

        constructor(navn: String,
                    fodselsnummer: String,
                    varselÅrsaker: List<String>) : this(navn = flettefelt(navn),
                                                        fodselsnummer = flettefelt(fodselsnummer),
                                                        varselÅrsaker = flettefelt(varselÅrsaker))
    }

    data class DelmalData(
            val signatur: SignaturDelmal
    )
}