package no.nav.familie.ba.sak.kjerne.dokument.domene

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.HENLEGGE_TRUKKET_SØKNAD
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.INFORMASJONSBREV_DELT_BOSTED
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.INNHENTE_OPPLYSNINGER
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType.VARSEL_OM_REVURDERING
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.FlettefelterForDokumentImpl
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.HenleggeTrukketSøknadBrev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.HenleggeTrukketSøknadData
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InformasjonsbrevDeltBostedBrev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InformasjonsbrevDeltBostedData
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InnhenteOpplysningerBrev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.InnhenteOpplysningerData
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.MedVennilgHilsen
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.SignaturDelmal
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.VarselOmRevurderingBrev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.VarselOmRevurderingData
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet


data class ManueltBrevRequest(
        val brevmal: BrevType,
        val multiselectVerdier: List<String> = emptyList(),
        val mottakerIdent: String,
        // Brukes kun ved sending av dokumenter på fagsak uten behandling
        val barnIBrev: List<String> = emptyList(),
        // Settes av backend ved utsending fra behandling
        val mottakerMålform: Målform = Målform.NB,
        val mottakerNavn: String = "",
        val enhet: Enhet? = null) {

    override fun toString(): String {
        return "${ManueltBrevRequest::class}, $brevmal"
    }

    fun enhetNavn(): String = this.enhet?.enhetNavn ?: error("Finner ikke enhetsnavn på manuell brevrequest")
}

fun ManueltBrevRequest.byggMottakerdata(behandling: Behandling,
                                        persongrunnlagService: PersongrunnlagService,
                                        arbeidsfordelingService: ArbeidsfordelingService): ManueltBrevRequest {
    val mottaker =
            persongrunnlagService.hentPersonerPåBehandling(listOf(this.mottakerIdent), behandling).singleOrNull()
            ?: error("Fant en eller ingen mottakere på behandling")

    val arbeidsfordelingPåBehandling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id)
    return this.copy(
            enhet = Enhet(
                    enhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                    enhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId
            ),
            mottakerMålform = mottaker.målform,
            mottakerNavn = mottaker.navn
    )
}

fun ManueltBrevRequest.leggTilEnhet(arbeidsfordelingService: ArbeidsfordelingService): ManueltBrevRequest {
    val arbeidsfordelingsenhet = arbeidsfordelingService.hentArbeidsfordelingsenhetPåIdenter(
            søkerIdent = mottakerIdent,
            barnIdenter = barnIBrev
    )
    return this.copy(
            enhet = Enhet(
                    enhetNavn = arbeidsfordelingsenhet.enhetNavn,
                    enhetId = arbeidsfordelingsenhet.enhetId
            ),
    )
}


fun ManueltBrevRequest.tilBrevmal() = when (this.brevmal.malId) {
    INFORMASJONSBREV_DELT_BOSTED.malId ->
        InformasjonsbrevDeltBostedBrev(
                data = InformasjonsbrevDeltBostedData(
                        delmalData = InformasjonsbrevDeltBostedData.DelmalData(medVennilgHilsen = MedVennilgHilsen(enhet = flettefelt(
                                this.enhetNavn()))),
                        flettefelter = InformasjonsbrevDeltBostedData.Flettefelter(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                                barnMedDeltBostedAvtale = this.multiselectVerdier,
                        ))
        )
    INNHENTE_OPPLYSNINGER.malId ->
        InnhenteOpplysningerBrev(
                data = InnhenteOpplysningerData(
                        delmalData = InnhenteOpplysningerData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                        flettefelter = InnhenteOpplysningerData.Flettefelter(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                                dokumentliste = this.multiselectVerdier,
                        ))
        )
    HENLEGGE_TRUKKET_SØKNAD.malId ->
        HenleggeTrukketSøknadBrev(
                data = HenleggeTrukketSøknadData(
                        delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                        flettefelter = FlettefelterForDokumentImpl(
                                navn = this.mottakerNavn,
                                fodselsnummer = this.mottakerIdent,
                        ))
        )
    VARSEL_OM_REVURDERING.malId ->
        VarselOmRevurderingBrev(
                data = VarselOmRevurderingData(
                        delmalData = VarselOmRevurderingData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
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
