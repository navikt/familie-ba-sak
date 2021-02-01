package no.nav.familie.ba.sak.brev.domene.maler

data class InnhenteOpplysningeBrev(
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
            val dokumentliste: Flettefelt,
            val dato: Flettefelt,
    )

    data class DelmalData(
            val signatur: SignaturDelmal
    )
}