package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.EnkeltInformasjonsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.FlettefelterForDokumentImpl
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevKanSøke
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InnhenteOpplysningerBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InnhenteOpplysningerData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.SignaturDelmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingDeltBostedParagraf14Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingDeltBostedParagraf14Data
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingSamboerBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingSamboerData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import java.time.LocalDate

data class ManueltBrevRequest(
    val brevmal: Brevmal,
    val multiselectVerdier: List<String> = emptyList(),
    val mottakerIdent: String,
    val barnIBrev: List<String> = emptyList(),
    val datoAvtale: String? = null,
    // Settes av backend ved utsending fra behandling
    val mottakerMålform: Målform = Målform.NB,
    val mottakerNavn: String = "",
    val enhet: Enhet? = null,
    val antallUkerSvarfrist: Int? = null,
) {

    override fun toString(): String {
        return "${ManueltBrevRequest::class}, $brevmal"
    }

    fun enhetNavn(): String = this.enhet?.enhetNavn ?: error("Finner ikke enhetsnavn på manuell brevrequest")
}

fun ManueltBrevRequest.byggMottakerdata(
    behandling: Behandling,
    persongrunnlagService: PersongrunnlagService,
    arbeidsfordelingService: ArbeidsfordelingService
): ManueltBrevRequest {
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

fun ManueltBrevRequest.tilBrev() = when (this.brevmal) {
    Brevmal.INFORMASJONSBREV_DELT_BOSTED ->
        InformasjonsbrevDeltBostedBrev(
            data = InformasjonsbrevDeltBostedData(
                delmalData = InformasjonsbrevDeltBostedData.DelmalData(
                    signatur = SignaturDelmal(
                        enhet = flettefelt(
                            this.enhetNavn()
                        )
                    )
                ),
                flettefelter = InformasjonsbrevDeltBostedData.Flettefelter(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                    barnMedDeltBostedAvtale = this.multiselectVerdier,
                )
            )
        )
    Brevmal.INNHENTE_OPPLYSNINGER ->
        InnhenteOpplysningerBrev(
            data = InnhenteOpplysningerData(
                delmalData = InnhenteOpplysningerData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                flettefelter = InnhenteOpplysningerData.Flettefelter(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                    dokumentliste = this.multiselectVerdier,
                )
            )
        )
    Brevmal.HENLEGGE_TRUKKET_SØKNAD ->
        HenleggeTrukketSøknadBrev(
            data = HenleggeTrukketSøknadData(
                delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                flettefelter = FlettefelterForDokumentImpl(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                )
            )
        )
    Brevmal.VARSEL_OM_REVURDERING ->
        VarselOmRevurderingBrev(
            data = VarselOmRevurderingData(
                delmalData = VarselOmRevurderingData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                flettefelter = VarselOmRevurderingData.Flettefelter(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                    varselÅrsaker = this.multiselectVerdier,
                )
            )
        )
    Brevmal.VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 ->
        VarselOmRevurderingDeltBostedParagraf14Brev(
            data = VarselOmRevurderingDeltBostedParagraf14Data(
                delmalData = VarselOmRevurderingDeltBostedParagraf14Data.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                flettefelter = VarselOmRevurderingDeltBostedParagraf14Data.Flettefelter(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                    barnMedDeltBostedAvtale = this.multiselectVerdier,
                )
            )
        )
    Brevmal.VARSEL_OM_REVURDERING_SAMBOER ->
        if (this.datoAvtale == null) throw FunksjonellFeil(
            frontendFeilmelding = "Du må sette dato for samboerskap for å sende dette brevet.",
            melding = "Dato er ikke satt for brevtype 'varsel om revurdering samboer'"
        )
        else VarselOmRevurderingSamboerBrev(
            data = VarselOmRevurderingSamboerData(
                delmalData = VarselOmRevurderingSamboerData.DelmalData(signatur = SignaturDelmal(enhet = this.enhetNavn())),
                flettefelter = VarselOmRevurderingSamboerData.Flettefelter(
                    navn = this.mottakerNavn,
                    fodselsnummer = this.mottakerIdent,
                    datoAvtale = LocalDate.parse(this.datoAvtale).tilDagMånedÅr()
                )
            )
        )
    Brevmal.SVARTIDSBREV ->
        EnkeltInformasjonsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.SVARTIDSBREV
        )
    Brevmal.FORLENGET_SVARTIDSBREV ->
        ForlengetSvartidsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhetNavn = this.enhetNavn(),
            årsaker = this.multiselectVerdier,
            antallUkerSvarfrist = this.antallUkerSvarfrist ?: throw Feil("Antall uker svarfrist er ikke satt")
        )
    Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG ->
        EnkeltInformasjonsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG
        )
    Brevmal.INFORMASJONSBREV_FØDSEL_UMYNDIG ->
        EnkeltInformasjonsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.INFORMASJONSBREV_FØDSEL_UMYNDIG
        )
    Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL ->
        EnkeltInformasjonsbrev(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            mal = Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL
        )
    Brevmal.INFORMASJONSBREV_KAN_SØKE ->
        InformasjonsbrevKanSøke(
            navn = this.mottakerNavn,
            fodselsnummer = this.mottakerIdent,
            enhet = this.enhetNavn(),
            dokumentliste = this.multiselectVerdier
        )
    else -> throw Feil("Kan ikke mappe fra manuel brevrequest til ${this.brevmal}.")
}
