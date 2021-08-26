package no.nav.familie.ba.sak.kjerne.dokument.domene

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.*
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.FlettefelterForDokumentImpl
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.HenleggeTrukketSøknadBrev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.HenleggeTrukketSøknadData
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InformasjonsbrevDeltBostedBrev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InformasjonsbrevDeltBostedData
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InnhenteOpplysningerBrev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InnhenteOpplysningerData
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.SignaturDelmal
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.VarselOmRevurderingBrev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.VarselOmRevurderingData


data class ManueltBrevRequest(
        val brevmal: BrevType,
        val multiselectVerdier: List<String> = emptyList(),
        val mottakerIdent: String,
        // Brukes kun ved sending av dokumenter på fagsak uten behandling
        val barnIBrev: List<String> = emptyList(),
        // Settes av backend ved utsending fra behandling
        val mottakerMålform: Målform = Målform.NB,
        val mottakerNavn: String = "",
        val enhetNavn: String = "") {

    override fun toString(): String {
        return "${ManueltBrevRequest::class}, $brevmal"
    }
}

fun ManueltBrevRequest.byggMottakerdata(behandling: Behandling,
                                        persongrunnlagService: PersongrunnlagService,
                                        arbeidsfordelingService: ArbeidsfordelingService): ManueltBrevRequest {
    val mottaker =
            persongrunnlagService.hentPersonPåBehandling(PersonIdent(this.mottakerIdent), behandling)
            ?: error("Finner ikke mottaker på behandlingen")

    return this.copy(
            enhetNavn = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id).behandlendeEnhetNavn,
            mottakerMålform = mottaker.målform,
            mottakerNavn = mottaker.navn
    )
}

fun ManueltBrevRequest.leggTilEnhet(arbeidsfordelingService: ArbeidsfordelingService): ManueltBrevRequest {
    return this.copy(
            enhetNavn = arbeidsfordelingService.hentArbeidsfordelingsenhetPåIdenter(
                    søkerIdent = mottakerIdent,
                    barnIdenter = barnIBrev
            ).enhetNavn,
    )
}


fun ManueltBrevRequest.tilBrevmal() = when (this.brevmal.malId) {
    INFORMASJONSBREV_DELT_BOSTED.malId ->
        InformasjonsbrevDeltBostedBrev(
                data = InformasjonsbrevDeltBostedData(
                        delmalData = InformasjonsbrevDeltBostedData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn)),
                        flettefelter = InformasjonsbrevDeltBostedData.Flettefelter(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                                barnMedDeltBostedAvtale = this.multiselectVerdier,
                        ))
        )
    INNHENTE_OPPLYSNINGER.malId ->
        InnhenteOpplysningerBrev(
                data = InnhenteOpplysningerData(
                        delmalData = InnhenteOpplysningerData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn)),
                        flettefelter = InnhenteOpplysningerData.Flettefelter(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                                dokumentliste = this.multiselectVerdier,
                        ))
        )
    HENLEGGE_TRUKKET_SØKNAD.malId ->
        HenleggeTrukketSøknadBrev(
                data = HenleggeTrukketSøknadData(
                        delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn)),
                        flettefelter = FlettefelterForDokumentImpl(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                        ))
        )
    VARSEL_OM_REVURDERING.malId ->
        VarselOmRevurderingBrev(
                data = VarselOmRevurderingData(
                        delmalData = VarselOmRevurderingData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn)),
                        flettefelter = VarselOmRevurderingData.Flettefelter(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                                varselÅrsaker = this.multiselectVerdier,
                        ))
        )
    else -> error("Kan ikke mappe brevmal for ${
        this.brevmal.visningsTekst
    } til ny brevtype da denne ikke er støttet i ny løsning enda.")
}
