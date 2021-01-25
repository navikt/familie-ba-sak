package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.dokument.DokumentController
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

data class InnhenteOpplysningerMal(
        override val delmalData: InnhenteOpplysningerDelmaler,
        override val flettefelter: InnhenteOpplysningerFlettefelter,
) : Brev {

    override fun toBrevString(): String {
        return objectMapper.writeValueAsString(this)
    }
}

data class InnhenteOpplysningerFlettefelter(
        val navn: Flettefelt,
        val fodselsnummer: Flettefelt,
        val dokumentliste: Flettefelt,
        val dato: Flettefelt,
)

data class InnhenteOpplysningerDelmaler(
        val signatur: SignaturDelmal
)

data class SignaturDelmal(
        val enhet: Flettefelt,
        val saksbehandler: Flettefelt,
)

fun DokumentController.ManueltBrevRequest.tilBrevmal(enhetNavn: String, mottaker: Person) =
        InnhenteOpplysningerMal(
                delmalData = InnhenteOpplysningerDelmaler(
                        signatur = SignaturDelmal(
                                enhet = flettefelt(enhetNavn),
                                saksbehandler = flettefelt(SikkerhetContext.hentSaksbehandlerNavn())
                        )
                ),
                flettefelter = InnhenteOpplysningerFlettefelter(
                        navn = flettefelt(mottaker.navn),
                        fodselsnummer = flettefelt(mottaker.personIdent.ident),
                        dokumentliste = flettefelt(this.multiselectVerdier),
                        dato = flettefelt(LocalDate.now().tilDagMånedÅr())
                )
        )