package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.EnkeltInformasjonsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.FlettefelterForDokumentImpl
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.FritekstAvsnitt
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevInnhenteOpplysningerKlage
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevInnhenteOpplysningerKlageData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevKanSøke
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevTilForelderBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevTilForelderData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InnhenteOpplysningerBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InnhenteOpplysningerData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InnhenteOpplysningerOmBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.SignaturDelmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Svartidsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.UtbetalingEtterKAVedtak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.UtbetalingEtterKAVedtakData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingDeltBostedParagraf14Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingDeltBostedParagraf14Data
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingSamboerBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingSamboerData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselbrevMedÅrsaker
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselbrevÅrlegKontrollEøs
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.VarselbrevMedÅrsakerOgBarn
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import java.time.LocalDate

data class Person(
    val navn: String,
    val fødselsnummer: String,
)

data class ManuellBrevmottaker(
    val type: MottakerType,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String? = "",
    val postnummer: String,
    val poststed: String,
    val landkode: String,
) {
    constructor(brevmottakerDb: BrevmottakerDb) : this(
        type = brevmottakerDb.type,
        navn = brevmottakerDb.navn,
        adresselinje1 = brevmottakerDb.adresselinje1,
        adresselinje2 = brevmottakerDb.adresselinje2,
        postnummer = brevmottakerDb.postnummer,
        poststed = brevmottakerDb.poststed,
        landkode = brevmottakerDb.landkode,
    )

    fun harGyldigAdresse(): Boolean {
        if (this.landkode == "NO") {
            return this.navn.isNotEmpty() &&
                this.adresselinje1.isNotEmpty() &&
                this.postnummer.isNotEmpty() &&
                this.poststed.isNotEmpty()
        } else {
            // Utenlandske manuelle brevmottakere skal ha postnummer og poststed satt i adresselinjene
            return this.navn.isNotEmpty() &&
                this.adresselinje1.isNotEmpty() &&
                this.postnummer.isEmpty() &&
                this.poststed.isEmpty()
        }
    }
}

data class ManueltBrevRequest(
    val brevmal: Brevmal,
    val multiselectVerdier: List<String> = emptyList(),
    val barnIBrev: List<String> = emptyList(),
    val datoAvtale: String? = null,
    // Settes av backend ved utsending fra behandling
    val mottakerMålform: Målform = Målform.NB,
    val enhet: Enhet? = null,
    val antallUkerSvarfrist: Int? = null,
    val barnasFødselsdager: List<LocalDate>? = null,
    val behandlingKategori: BehandlingKategori? = null,
    val vedrørende: Person? = null,
    val mottakerlandSed: List<String> = emptyList(),
    val manuelleBrevmottakere: List<ManuellBrevmottaker> = emptyList(),
    val fritekstAvsnitt: String? = null,
) {
    override fun toString(): String = "${ManueltBrevRequest::class}, $brevmal"

    fun enhetNavn(): String = this.enhet?.enhetNavn ?: throw Feil("Finner ikke enhetsnavn på manuell brevrequest")

    fun mottakerlandSED(): List<String> {
        if (this.mottakerlandSed.contains("NO")) {
            throw FunksjonellFeil(
                frontendFeilmelding = "Norge kan ikke velges som mottakerland.",
                melding = "Ugyldig mottakerland for brevtype 'varsel om årlig revurdering EØS'",
            )
        }
        return this.mottakerlandSed.takeIf { it.isNotEmpty() }
            ?: throw Feil("Finner ikke noen mottakerland for SED på manuell brevrequest")
    }
}

fun ManueltBrevRequest.byggMottakerdataFraBehandling(
    behandling: Behandling,
    persongrunnlagService: PersongrunnlagService,
    arbeidsfordelingService: ArbeidsfordelingService,
): ManueltBrevRequest {
    val mottakerIdent = behandling.fagsak.institusjon?.orgNummer ?: behandling.fagsak.aktør.aktivFødselsnummer()

    val hentPerson = { ident: String ->
        persongrunnlagService.hentPersonerPåBehandling(listOf(ident), behandling).singleOrNull()
            ?: throw Feil("Fant flere eller ingen personer med angitt personident på behandlingId=${behandling.id}")
    }
    val enhet =
        arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id).run {
            Enhet(enhetId = behandlendeEnhetId, enhetNavn = behandlendeEnhetNavn)
        }
    return when (behandling.fagsak.type) {
        FagsakType.INSTITUSJON -> {
            val fødselsnummerPåPerson = behandling.fagsak.aktør.aktivFødselsnummer()
            val person = hentPerson(fødselsnummerPåPerson)

            this.copy(
                enhet = enhet,
                mottakerMålform = person.målform,
                vedrørende = Person(navn = person.navn, fødselsnummer = fødselsnummerPåPerson),
            )
        }

        FagsakType.NORMAL,
        FagsakType.BARN_ENSLIG_MINDREÅRIG,
        FagsakType.SKJERMET_BARN,
        -> {
            hentPerson(mottakerIdent).let { mottakerPerson ->
                this.copy(
                    enhet = enhet,
                    mottakerMålform = mottakerPerson.målform,
                )
            }
        }
    }
}

fun ManueltBrevRequest.byggMottakerdataFraFagsak(
    fagsak: Fagsak,
    arbeidsfordelingService: ArbeidsfordelingService,
    pdlRestKlient: PdlRestKlient,
): ManueltBrevRequest {
    val enhet =
        arbeidsfordelingService
            .hentArbeidsfordelingsenhetPåIdenter(
                søkerIdent = fagsak.aktør.aktivFødselsnummer(),
                barnIdenter = barnIBrev,
            ).run {
                Enhet(enhetId = enhetId, enhetNavn = enhetNavn)
            }

    return when (fagsak.type) {
        FagsakType.INSTITUSJON, FagsakType.SKJERMET_BARN -> {
            val aktør = fagsak.skjermetBarnSøker?.aktør ?: fagsak.aktør

            val personNavn = pdlRestKlient.hentPerson(aktør, PersonInfoQuery.ENKEL).navn ?: throw FunksjonellFeil("Finner ikke navn på person i PDL")

            this.copy(
                enhet = enhet,
                vedrørende = Person(navn = personNavn, fødselsnummer = aktør.aktivFødselsnummer()),
            )
        }

        FagsakType.NORMAL,
        FagsakType.BARN_ENSLIG_MINDREÅRIG,
        ->
            this.copy(
                enhet = enhet,
            )
    }
}

fun ManueltBrevRequest.tilBrev(
    mottakerIdent: String,
    mottakerNavn: String,
    saksbehandlerNavn: String,
    hentLandkoder: () -> Map<String, String>,
): Brev {
    val signaturDelmal =
        SignaturDelmal(
            enhet = this.enhetNavn(),
            saksbehandlerNavn = saksbehandlerNavn,
        )

    val fritekstAvsnitt =
        this.fritekstAvsnitt
            ?.takeIf { it.isNotBlank() }
            ?.let { FritekstAvsnitt(it) }

    return when (this.brevmal) {
        Brevmal.INFORMASJONSBREV_DELT_BOSTED ->
            InformasjonsbrevDeltBostedBrev(
                data =
                    InformasjonsbrevDeltBostedData(
                        delmalData =
                            InformasjonsbrevDeltBostedData.DelmalData(
                                signatur = signaturDelmal,
                            ),
                        flettefelter =
                            InformasjonsbrevDeltBostedData.Flettefelter(
                                navn = mottakerNavn,
                                fodselsnummer = mottakerIdent,
                                barnMedDeltBostedAvtale = this.multiselectVerdier,
                            ),
                    ),
            )

        Brevmal.INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HENTER_IKKE_REGISTEROPPLYSNINGER,
        Brevmal.INFORMASJONSBREV_KAN_HA_RETT_TIL_PENGESTØTTE_FRA_NAV,
        ->
            InformasjonsbrevTilForelderBrev(
                mal = this.brevmal,
                data =
                    InformasjonsbrevTilForelderData(
                        delmalData =
                            InformasjonsbrevTilForelderData.DelmalData(
                                signatur = signaturDelmal,
                            ),
                        flettefelter =
                            InformasjonsbrevTilForelderData.Flettefelter(
                                navn = mottakerNavn,
                                fodselsnummer = mottakerIdent,
                                barnIBrev = this.multiselectVerdier,
                            ),
                    ),
            )

        Brevmal.INNHENTE_OPPLYSNINGER,
        Brevmal.INNHENTE_OPPLYSNINGER_INSTITUSJON,
        ->
            InnhenteOpplysningerBrev(
                mal = brevmal,
                data =
                    InnhenteOpplysningerData(
                        delmalData =
                            InnhenteOpplysningerData.DelmalData(
                                signatur = signaturDelmal,
                                fritekstAvsnitt = fritekstAvsnitt,
                            ),
                        flettefelter =
                            InnhenteOpplysningerData.Flettefelter(
                                navn = mottakerNavn,
                                fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                                organisasjonsnummer = orgNrEllerNull(mottakerIdent),
                                gjelder = this.vedrørende?.navn,
                                dokumentliste = this.multiselectVerdier,
                            ),
                    ),
            )

        Brevmal.INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE,
        Brevmal.INFORMASJONSBREV_INNHENTE_OPPLYSNINGER_KLAGE_INSTITUSJON,
        -> {
            if (fritekstAvsnitt == null) {
                throw FunksjonellFeil("Du må legge til fritekst for å forklare hvilke opplysninger du ønsker å innhente.")
            }
            InformasjonsbrevInnhenteOpplysningerKlage(
                mal = brevmal,
                data =
                    InformasjonsbrevInnhenteOpplysningerKlageData(
                        delmalData =
                            InformasjonsbrevInnhenteOpplysningerKlageData.DelmalData(
                                signatur = signaturDelmal,
                                fritekstAvsnitt = fritekstAvsnitt,
                            ),
                        flettefelter =
                            InformasjonsbrevInnhenteOpplysningerKlageData.Flettefelter(
                                navn = mottakerNavn,
                                fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                                organisasjonsnummer = orgNrEllerNull(mottakerIdent),
                                gjelder = this.vedrørende?.navn,
                            ),
                    ),
            )
        }

        Brevmal.UTBETALING_ETTER_KA_VEDTAK ->
            UtbetalingEtterKAVedtak(
                mal = Brevmal.UTBETALING_ETTER_KA_VEDTAK,
                data =
                    UtbetalingEtterKAVedtakData(
                        delmalData =
                            UtbetalingEtterKAVedtakData.DelmalData(
                                signatur = signaturDelmal,
                            ),
                        flettefelter =
                            UtbetalingEtterKAVedtakData.Flettefelter(
                                navn = mottakerNavn,
                                fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                                organisasjonsnummer = orgNrEllerNull(mottakerIdent),
                                gjelder = this.vedrørende?.navn,
                            ),
                        fritekst = this.fritekstAvsnitt,
                    ),
            )

        Brevmal.HENLEGGE_TRUKKET_SØKNAD ->
            HenleggeTrukketSøknadBrev(
                mal = Brevmal.HENLEGGE_TRUKKET_SØKNAD,
                data =
                    HenleggeTrukketSøknadData(
                        delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = signaturDelmal),
                        flettefelter =
                            FlettefelterForDokumentImpl(
                                navn = mottakerNavn,
                                fodselsnummer = mottakerIdent,
                            ),
                    ),
            )

        Brevmal.HENLEGGE_TRUKKET_SØKNAD_INSTITUSJON ->
            HenleggeTrukketSøknadBrev(
                mal = Brevmal.HENLEGGE_TRUKKET_SØKNAD_INSTITUSJON,
                data =
                    HenleggeTrukketSøknadData(
                        delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = signaturDelmal),
                        flettefelter =
                            FlettefelterForDokumentImpl(
                                navn = mottakerNavn,
                                organisasjonsnummer = mottakerIdent,
                                gjelder = this.vedrørende?.navn,
                                fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                            ),
                    ),
            )

        Brevmal.VARSEL_OM_REVURDERING ->
            VarselbrevMedÅrsaker(
                mal = Brevmal.VARSEL_OM_REVURDERING,
                navn = mottakerNavn,
                fødselsnummer = mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_REVURDERING_INSTITUSJON ->
            VarselbrevMedÅrsaker(
                mal = Brevmal.VARSEL_OM_REVURDERING_INSTITUSJON,
                navn = mottakerNavn,
                fødselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                organisasjonsnummer = mottakerIdent,
                gjelder = this.vedrørende?.navn,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 ->
            VarselOmRevurderingDeltBostedParagraf14Brev(
                data =
                    VarselOmRevurderingDeltBostedParagraf14Data(
                        delmalData = VarselOmRevurderingDeltBostedParagraf14Data.DelmalData(signatur = signaturDelmal),
                        flettefelter =
                            VarselOmRevurderingDeltBostedParagraf14Data.Flettefelter(
                                navn = mottakerNavn,
                                fodselsnummer = mottakerIdent,
                                barnMedDeltBostedAvtale = this.multiselectVerdier,
                            ),
                    ),
            )

        Brevmal.VARSEL_OM_REVURDERING_SAMBOER -> {
            if (this.datoAvtale == null) {
                throw FunksjonellFeil(
                    frontendFeilmelding = "Du må sette dato for samboerskap for å sende dette brevet.",
                    melding = "Dato er ikke satt for brevtype 'varsel om revurdering samboer'",
                )
            }
            VarselOmRevurderingSamboerBrev(
                data =
                    VarselOmRevurderingSamboerData(
                        delmalData = VarselOmRevurderingSamboerData.DelmalData(signatur = signaturDelmal),
                        flettefelter =
                            VarselOmRevurderingSamboerData.Flettefelter(
                                navn = mottakerNavn,
                                fodselsnummer = mottakerIdent,
                                datoAvtale = LocalDate.parse(this.datoAvtale).tilDagMånedÅr(),
                            ),
                    ),
            )
        }

        Brevmal.VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT ->
            VarselbrevMedÅrsakerOgBarn(
                mal = Brevmal.VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT,
                navn = mottakerNavn,
                fødselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.SVARTIDSBREV ->
            Svartidsbrev(
                navn = mottakerNavn,
                fodselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.SVARTIDSBREV,
                erEøsBehandling = erEøsBehandling(behandlingKategori),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.SVARTIDSBREV_INSTITUSJON ->
            Svartidsbrev(
                navn = mottakerNavn,
                fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.SVARTIDSBREV_INSTITUSJON,
                erEøsBehandling = false,
                organisasjonsnummer = mottakerIdent,
                gjelder = this.vedrørende?.navn,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.FORLENGET_SVARTIDSBREV,
        Brevmal.FORLENGET_SVARTIDSBREV_INSTITUSJON,
        ->
            ForlengetSvartidsbrev(
                mal = brevmal,
                navn = mottakerNavn,
                fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                enhetNavn = this.enhetNavn(),
                årsaker = this.multiselectVerdier,
                antallUkerSvarfrist =
                    this.antallUkerSvarfrist ?: throw FunksjonellFeil(
                        melding = "Antall uker svarfrist er ikke satt",
                        frontendFeilmelding = "Antall uker svarfrist er ikke satt",
                    ),
                organisasjonsnummer = orgNrEllerNull(mottakerIdent),
                gjelder = this.vedrørende?.navn,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG ->
            EnkeltInformasjonsbrev(
                navn = mottakerNavn,
                fodselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_FØDSEL_VERGEMÅL ->
            EnkeltInformasjonsbrev(
                navn = mottakerNavn,
                fodselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_FØDSEL_VERGEMÅL,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL ->
            EnkeltInformasjonsbrev(
                navn = mottakerNavn,
                fodselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_KAN_SØKE ->
            InformasjonsbrevKanSøke(
                navn = mottakerNavn,
                fodselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                dokumentliste = this.multiselectVerdier,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED ->
            VarselbrevMedÅrsakerOgBarn(
                mal = Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
                navn = mottakerNavn,
                fødselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                varselÅrsaker = this.multiselectVerdier,
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
                fritekstAvsnitt = fritekstAvsnitt,
            )

        Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS ->
            VarselbrevMedÅrsaker(
                mal = Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
                navn = mottakerNavn,
                fødselsnummer = mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS ->
            VarselbrevÅrlegKontrollEøs(
                mal = Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS,
                navn = mottakerNavn,
                fødselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                mottakerlandSed = this.mottakerlandSED().map { tilLandNavn(hentLandkoder(), it) }.slåSammen(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER ->
            VarselbrevÅrlegKontrollEøs(
                mal = Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER,
                navn = mottakerNavn,
                fødselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                mottakerlandSed = this.mottakerlandSED().map { tilLandNavn(hentLandkoder(), it) }.slåSammen(),
                dokumentliste = this.multiselectVerdier,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED ->
            InnhenteOpplysningerOmBarn(
                mal = Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
                navn = mottakerNavn,
                fødselsnummer = mottakerIdent,
                dokumentliste = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
                fritekstAvsnitt = this.fritekstAvsnitt,
            )

        Brevmal.INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT ->
            InnhenteOpplysningerOmBarn(
                mal = Brevmal.INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT,
                navn = mottakerNavn,
                fødselsnummer = mottakerIdent,
                dokumentliste = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS ->
            EnkeltInformasjonsbrev(
                navn = mottakerNavn,
                fodselsnummer = mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
        Brevmal.VEDTAK_ENDRING,
        Brevmal.VEDTAK_OPPHØRT,
        Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        Brevmal.VEDTAK_AVSLAG,
        Brevmal.VEDTAK_FORTSATT_INNVILGET,
        Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV,
        Brevmal.VEDTAK_OPPHØR_DØDSFALL,
        Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON,
        Brevmal.VEDTAK_AVSLAG_INSTITUSJON,
        Brevmal.VEDTAK_OPPHØRT_INSTITUSJON,
        Brevmal.VEDTAK_ENDRING_INSTITUSJON,
        Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON,
        Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON,
        Brevmal.AUTOVEDTAK_ENDRING,
        Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
        Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
        Brevmal.AUTOVEDTAK_FINNMARKSTILLEGG,
        Brevmal.AUTOVEDTAK_SVALBARDTILLEGG,
        Brevmal.TILBAKEKREVINGSVEDTAK_MOTREGNING,
        -> throw Feil("Kan ikke mappe fra manuel brevrequest til ${this.brevmal}.")
    }
}

private fun tilLandNavn(
    landkoderISO2: Map<String, String>,
    landKode: String,
): String {
    if (landKode.length != 2) {
        throw Feil("LandkoderISO2 forventer en landkode med to tegn")
    }

    val landNavn = (
        landkoderISO2[landKode]
            ?: throw Feil("Fant ikke navn for landkode $landKode ")
    )

    return landNavn.storForbokstav()
}

private fun List<LocalDate>?.tilFormaterteFødselsdager() =
    this
        ?.map { it.tilKortString() }
        ?.slåSammen()
        ?: throw Feil(
            "Fikk ikke med barna sine fødselsdager",
        )

private fun erOrgNr(ident: String): Boolean = ident.length == 9 && ident.all { it.isDigit() }

private fun orgNrEllerNull(ident: String): String? = if (erOrgNr(ident)) ident else null

private fun erEøsBehandling(behandlingKategori: BehandlingKategori?): Boolean =
    when (behandlingKategori) {
        null -> throw Feil("Trenger å vite om behandling er EØS for å sende ut svartidsbrev.")
        BehandlingKategori.EØS -> true
        BehandlingKategori.NASJONAL -> false
    }
