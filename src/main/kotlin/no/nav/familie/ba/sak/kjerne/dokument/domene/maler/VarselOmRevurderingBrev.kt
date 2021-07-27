package no.nav.familie.ba.sak.kjerne.dokument.domene.maler

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
            override val navn: Flettefelt,
            override val fodselsnummer: Flettefelt,
            override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
            val varselAarsaker: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(navn: String,
                    fodselsnummer: String,
                    varselÅrsaker: List<String>) : this(
                navn = flettefelt(navn),
                fodselsnummer = flettefelt(fodselsnummer),
                varselAarsaker = flettefelt(varselÅrsaker),
        )
    }

    data class DelmalData(
            val signatur: SignaturDelmal
    )
}