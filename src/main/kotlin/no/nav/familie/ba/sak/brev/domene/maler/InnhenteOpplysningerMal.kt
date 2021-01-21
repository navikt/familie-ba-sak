package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.dokument.DokumentController
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper

data class InnhenteOpplysningerMal(
        override val delmalData: InnhenteOpplysningerDelmaler,
        override val flettefelter: InnhenteOpplysningerFlettefelter,
) : Brev {

    override fun toFamilieBrevString(): String {
        return objectMapper.writeValueAsString(this)
    }
}

data class InnhenteOpplysningerFlettefelter(
        val navn: Flettefelt,
        val fodselsnummer: Flettefelt,
        val dokumentListe: Flettefelt
)

data class InnhenteOpplysningerDelmaler(
        val signatur: SignaturDelmal
)

data class SignaturDelmal(
        val ENHET: Flettefelt,
        val SAKSBEHANDLER1: Flettefelt,
)

fun DokumentController.ManueltBrevRequest.tilBrevmal(enhetNavn: String, mottaker: Person) =
        InnhenteOpplysningerMal(
                delmalData = InnhenteOpplysningerDelmaler(
                        signatur = SignaturDelmal(
                                ENHET = flettefelt(enhetNavn),
                                SAKSBEHANDLER1 = flettefelt(SikkerhetContext.hentSaksbehandlerNavn())
                        )
                ),
                flettefelter = InnhenteOpplysningerFlettefelter(
                        navn = flettefelt(mottaker.navn),
                        fodselsnummer = flettefelt(mottaker.personIdent.ident),
                        dokumentListe = flettefelt(this.multiselectVerdier)
                )
        )