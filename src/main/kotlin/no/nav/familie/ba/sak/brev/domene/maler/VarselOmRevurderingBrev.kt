package no.nav.familie.ba.sak.brev.domene.maler

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
            val varselÅrsaker: Flettefelt,
            val dato: Flettefelt,
    )

    data class DelmalData(
            val signatur: SignaturDelmal
    )
}