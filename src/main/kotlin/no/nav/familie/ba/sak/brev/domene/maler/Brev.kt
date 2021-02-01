package no.nav.familie.ba.sak.brev.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.dokument.DokumentController.ManueltBrevRequest
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

interface Brev {

    val brevType: BrevType
    val brevData: BrevData
}

enum class BrevType(val apiNavn: String, val visningsTekst: String) {
    INNHENTE_OPPLYSNINGER("innhenteOpplysninger", "innhente opplysninger"),
    HENLEGGE_TRUKKET_SØKNAD("henleggeTrukketSøknad", "henlegge trukket søknad"),
    VARSEL_OM_REVURDERING("varselOmRevurdering", "varsel om revurdering"),
}

interface BrevData {

    val delmalData: Any
    val flettefelter: Any
    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

typealias Flettefelt = List<String>

fun flettefelt(flettefeltData: String): Flettefelt = listOf(flettefeltData)
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData


fun ManueltBrevRequest.tilBrevmal(enhetNavn: String, mottaker: Person) = when (this.brevmal.malId) {
    no.nav.familie.ba.sak.dokument.domene.BrevType.INNHENTE_OPPLYSNINGER.malId ->
        InnhenteOpplysningerBrev(
                brevData = InnhenteOpplysningerData(
                        delmalData = InnhenteOpplysningerData.DelmalData(
                                signatur = SignaturDelmal(
                                        enhet = flettefelt(enhetNavn),
                                        saksbehandler = flettefelt(SikkerhetContext.hentSaksbehandlerNavn())
                                )
                        ),
                        flettefelter = InnhenteOpplysningerData.Flettefelter(
                                navn = flettefelt(mottaker.navn),
                                fodselsnummer = flettefelt(mottaker.personIdent.ident),
                                dokumentliste = flettefelt(this.multiselectVerdier),
                                dato = flettefelt(LocalDate.now().tilDagMånedÅr())
                        ))
        )
    no.nav.familie.ba.sak.dokument.domene.BrevType.HENLEGGE_TRUKKET_SØKNAD.malId ->
        HenleggeTrukketSøknadBrev(
                brevData = HenleggeTrukketSøknadData(
                        delmalData = HenleggeTrukketSøknadData.DelmalData(
                                signatur = SignaturDelmal(
                                        enhet = flettefelt(enhetNavn),
                                        saksbehandler = flettefelt(SikkerhetContext.hentSaksbehandlerNavn())
                                )),
                        flettefelter = HenleggeTrukketSøknadData.Flettefelter(
                                navn = flettefelt(mottaker.navn),
                                fodselsnummer = flettefelt(mottaker.personIdent.ident),
                                dato = flettefelt(LocalDate.now().tilDagMånedÅr())
                        ))
        )
    no.nav.familie.ba.sak.dokument.domene.BrevType.VARSEL_OM_REVURDERING.malId ->
        VarselOmRevurderingBrev(
                brevData = VarselOmRevurderingData(
                        delmalData = VarselOmRevurderingData.DelmalData(
                                signatur = SignaturDelmal(
                                        enhet = flettefelt(enhetNavn),
                                        saksbehandler = flettefelt(SikkerhetContext.hentSaksbehandlerNavn())
                                )
                        ),
                        flettefelter = VarselOmRevurderingData.Flettefelter(
                                navn = flettefelt(mottaker.navn),
                                fodselsnummer = flettefelt(mottaker.personIdent.ident),
                                varselÅrsaker = flettefelt(this.multiselectVerdier),
                                dato = flettefelt(LocalDate.now().tilDagMånedÅr())
                        ))
        )
    else -> error("Kan ikke mappe brevmal for ${this.brevmal.visningsTekst} til ny brevtype da denne ikke er støttet i ny løsning enda.")
}
