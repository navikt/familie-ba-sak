package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class VarselbrevMedÅrsaker(
    override val mal: Brevmal,
    override val data: VarselOmRevurderingData
) : Brev {
    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        varselÅrsaker: List<String>,
        enhet: String,
    ) : this(
        mal = mal,
        data = VarselOmRevurderingData(
            delmalData = VarselOmRevurderingData.DelmalData(signatur = SignaturDelmal(enhet = enhet)),
            flettefelter = VarselOmRevurderingData.Flettefelter(
                navn = navn, fodselsnummer = fødselsnummer, varselÅrsaker = varselÅrsaker
            ),
        )
    )
}

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

        constructor(
            navn: String,
            fodselsnummer: String,
            varselÅrsaker: List<String>
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            varselAarsaker = flettefelt(varselÅrsaker),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal
    )
}
