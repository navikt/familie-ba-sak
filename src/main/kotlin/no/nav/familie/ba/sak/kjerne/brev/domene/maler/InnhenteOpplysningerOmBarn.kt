package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InnhenteOpplysningerOmBarn(
    override val mal: Brevmal,
    override val data: InnhenteOpplysningerOmBarnData
) : Brev {
    constructor(
        mal: Brevmal,
        navn: String,
        fødselsnummer: String,
        barnasFødselsdager: String,
        enhet: String,
    ) : this(
        mal = mal,
        data = InnhenteOpplysningerOmBarnData(
            delmalData = InnhenteOpplysningerOmBarnData.DelmalData(signatur = SignaturDelmal(enhet = enhet)),
            flettefelter = InnhenteOpplysningerOmBarnData.Flettefelter(
                navn = navn, fodselsnummer = fødselsnummer, barnasFødselsdager = barnasFødselsdager
            ),
        )
    )
}

data class InnhenteOpplysningerOmBarnData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {

    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        val barnasFodselsdager: Flettefelt,
    ) : FlettefelterForDokument {

        constructor(
            navn: String,
            fodselsnummer: String,
            barnasFødselsdager: String
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            barnasFodselsdager = flettefelt(barnasFødselsdager),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal
    )
}
