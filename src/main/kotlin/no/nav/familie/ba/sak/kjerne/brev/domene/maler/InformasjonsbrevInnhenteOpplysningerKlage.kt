package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.tilDagMånedÅr
import java.time.LocalDate

data class InformasjonsbrevInnhenteOpplysningerKlage(
    override val mal: Brevmal,
    override val data: InformasjonsbrevInnhenteOpplysningerKlageData,
) : Brev

data class InformasjonsbrevInnhenteOpplysningerKlageData(
    override val delmalData: DelmalData,
    override val flettefelter: Flettefelter,
) : BrevData {
    data class Flettefelter(
        override val navn: Flettefelt,
        override val fodselsnummer: Flettefelt,
        override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
        override val organisasjonsnummer: Flettefelt,
        override val gjelder: Flettefelt,
    ) : FlettefelterForDokument {
        constructor(
            navn: String,
            fodselsnummer: String,
            organisasjonsnummer: String? = null,
            gjelder: String? = null,
        ) : this(
            navn = flettefelt(navn),
            fodselsnummer = flettefelt(fodselsnummer),
            organisasjonsnummer = flettefelt(organisasjonsnummer),
            gjelder = flettefelt(gjelder),
        )
    }

    data class DelmalData(
        val signatur: SignaturDelmal,
        val fritekstAvsnitt: FritekstAvsnitt,
    )
}
