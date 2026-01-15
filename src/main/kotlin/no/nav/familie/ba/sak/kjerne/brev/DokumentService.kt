package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonInfoQuery
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.PersonForManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerValidering
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Bruker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.FullmektigEllerVerge
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Institusjon
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerInfo
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.leggTilBlankAnnenVurdering
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.JournalførManueltBrevTask
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class DokumentService(
    private val taskRepository: TaskRepositoryWrapper,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
    private val rolleConfig: RolleConfig,
    private val settPåVentService: SettPåVentService,
    private val fagsakRepository: FagsakRepository,
    private val organisasjonService: OrganisasjonService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val brevmottakerService: BrevmottakerService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
    private val saksbehandlerContext: SaksbehandlerContext,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val pdlRestKlient: PdlRestKlient,
    private val persongrunnlagService: PersongrunnlagService,
    private val featureToggleService: FeatureToggleService,
    private val integrasjonKlient: IntegrasjonKlient,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        val høyesteRolletilgangForInnloggetBruker = SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        val funksjonelleRoller =
            listOf(
                BehandlerRolle.VEILEDER,
                BehandlerRolle.FORVALTER,
                BehandlerRolle.SAKSBEHANDLER,
                BehandlerRolle.BESLUTTER,
            )
        if (høyesteRolletilgangForInnloggetBruker in funksjonelleRoller && vedtak.stønadBrevPdF == null) {
            throw FunksjonellFeil(
                melding =
                    "Klarte ikke finne vedtaksbrev for vedtak med id ${vedtak.id}. Innlogget bruker har rolle: $høyesteRolletilgangForInnloggetBruker",
                frontendFeilmelding = "Det finnes ikke noe vedtaksbrev.",
            )
        } else {
            val pdf =
                vedtak.stønadBrevPdF ?: throw Feil(
                    "Klarte ikke finne vedtaksbrev for vedtak med id ${vedtak.id}. " +
                        "Innlogget bruker har rolle: $høyesteRolletilgangForInnloggetBruker",
                )
            return Ressurs.success(pdf)
        }
    }

    @Transactional
    fun sendManueltBrev(
        manueltBrevRequest: ManueltBrevRequest,
        behandling: Behandling? = null,
        fagsakId: Long,
    ) {
        if (behandling == null) {
            validerBrevmottakerService.validerAtFagsakIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                fagsakId = fagsakId,
                manueltBrevRequest.manuelleBrevmottakere,
                barnLagtTilIBrev = manueltBrevRequest.barnIBrev,
            )
        } else {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
                behandlingId = behandling.id,
                ekstraBarnLagtTilIBrev = manueltBrevRequest.barnIBrev,
            )
        }

        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw Feil("Finnes ikke fagsak for fagsakId=$fagsakId")

        val brevmottakereFraBehandling = behandling?.let { brevmottakerService.hentBrevmottakere(it.id) } ?: emptyList()
        val brevmottakere =
            manueltBrevRequest.manuelleBrevmottakere + brevmottakereFraBehandling.map { ManuellBrevmottaker(it) }

        if (!BrevmottakerValidering.erBrevmottakereGyldige(brevmottakere)) {
            throw FunksjonellFeil(
                melding = "Det finnes ugyldige brevmottakere i utsending av manuelt brev",
                frontendFeilmelding = "Adressen som er lagt til manuelt har ugyldig format, og brevet kan ikke sendes. Du må legge til manuell adresse på nytt.",
            )
        }

        lagMottakere(fagsak = fagsak, brevmottakere = brevmottakere)
            .forEach { mottakerInfo ->
                taskRepository.save(
                    JournalførManueltBrevTask
                        .opprettTask(
                            behandlingId = behandling?.id,
                            fagsakId = fagsak.id,
                            manuellBrevRequest = manueltBrevRequest,
                            mottakerInfo = mottakerInfo,
                            saksbehandlerSignaturTilBrev = saksbehandlerContext.hentSaksbehandlerSignaturTilBrev(),
                        ),
                )
            }

        if (behandling != null && manueltBrevRequest.brevmal.førerTilOpplysningsplikt()) {
            leggTilOpplysningspliktIVilkårsvurdering(behandling)
        }

        if (behandling != null && manueltBrevRequest.brevmal.setterBehandlingPåVent()) {
            settPåVentService.settBehandlingPåVent(
                behandlingId = behandling.id,
                frist =
                    LocalDate
                        .now()
                        .plusDays(
                            manueltBrevRequest.brevmal.ventefristDager(
                                manuellFrist = manueltBrevRequest.antallUkerSvarfrist?.let { it * 7 }?.toLong(),
                                behandlingKategori = behandling.kategori,
                            ),
                        ),
                årsak = manueltBrevRequest.brevmal.venteårsak(),
            )
        }
    }

    private fun lagMottakere(
        fagsak: Fagsak,
        brevmottakere: List<ManuellBrevmottaker>,
    ): List<MottakerInfo> =
        when {
            fagsak.type == FagsakType.INSTITUSJON -> {
                val orgNummer = checkNotNull(fagsak.institusjon).orgNummer
                listOf(
                    Institusjon(
                        orgNummer = orgNummer,
                        navn = organisasjonService.hentOrganisasjon(orgNummer).navn,
                    ),
                )
            }

            brevmottakere.isNotEmpty() -> {
                brevmottakerService.lagMottakereFraBrevMottakere(
                    brevmottakere,
                )
            }

            else -> {
                listOf(Bruker)
            }
        }

    private fun leggTilOpplysningspliktIVilkårsvurdering(behandling: Behandling) {
        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandling.id)
                ?: vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                    behandling = behandling,
                    bekreftEndringerViaFrontend = false,
                    forrigeBehandlingSomErVedtatt =
                        behandlingHentOgPersisterService
                            .hentForrigeBehandlingSomErVedtatt(behandling),
                )
        vilkårsvurdering.personResultater
            .single { it.erSøkersResultater() }
            .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
    }

    fun byggMottakerdataFraBehandling(
        behandling: Behandling,
        manueltBrevRequest: ManueltBrevRequest,
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

                manueltBrevRequest.copy(
                    enhet = enhet,
                    mottakerMålform = person.målform,
                    vedrørende = PersonForManueltBrevRequest(navn = person.navn, fødselsnummer = fødselsnummerPåPerson),
                )
            }

            FagsakType.NORMAL,
            FagsakType.BARN_ENSLIG_MINDREÅRIG,
            FagsakType.SKJERMET_BARN,
            -> {
                hentPerson(mottakerIdent).let { mottakerPerson ->
                    manueltBrevRequest.copy(
                        enhet = enhet,
                        mottakerMålform = mottakerPerson.målform,
                    )
                }
            }
        }
    }

    fun byggMottakerdataFraFagsak(
        fagsak: Fagsak,
        manueltBrevRequest: ManueltBrevRequest,
    ): ManueltBrevRequest {
        val enhet =
            if (featureToggleService.isEnabled(FeatureToggle.HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE)) {
                val enheterSomNavIdentHarTilgangTil = integrasjonKlient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(NavIdent(SikkerhetContext.hentSaksbehandler()))
                if (enheterSomNavIdentHarTilgangTil.size == 1) {
                    Enhet(
                        enhetId = enheterSomNavIdentHarTilgangTil.first().enhetsnummer,
                        enhetNavn = enheterSomNavIdentHarTilgangTil.first().enhetsnavn,
                    )
                } else {
                    val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id)

                    arbeidsfordelingService
                        .hentArbeidsfordelingsenhetPåIdenter(
                            søkerIdent = fagsak.aktør.aktivFødselsnummer(),
                            barnIdenter = manueltBrevRequest.barnIBrev,
                            behandlingstype = sisteVedtatteBehandling?.kategori?.tilOppgavebehandlingType(),
                        ).run {
                            Enhet(enhetId = enhetId, enhetNavn = enhetNavn)
                        }
                }
            } else {
                arbeidsfordelingService
                    .hentArbeidsfordelingsenhetPåIdenter(
                        søkerIdent = fagsak.aktør.aktivFødselsnummer(),
                        barnIdenter = manueltBrevRequest.barnIBrev,
                        behandlingstype = null,
                    ).run {
                        Enhet(enhetId = enhetId, enhetNavn = enhetNavn)
                    }
            }

        return when (fagsak.type) {
            FagsakType.INSTITUSJON,
            FagsakType.SKJERMET_BARN,
            -> {
                val aktør = fagsak.skjermetBarnSøker?.aktør ?: fagsak.aktør

                val personNavn = pdlRestKlient.hentPerson(aktør, PersonInfoQuery.ENKEL).navn ?: throw FunksjonellFeil("Finner ikke navn på person i PDL")

                manueltBrevRequest.copy(
                    enhet = enhet,
                    vedrørende = PersonForManueltBrevRequest(navn = personNavn, fødselsnummer = aktør.aktivFødselsnummer()),
                )
            }

            FagsakType.NORMAL,
            FagsakType.BARN_ENSLIG_MINDREÅRIG,
            -> {
                manueltBrevRequest.copy(
                    enhet = enhet,
                )
            }
        }
    }

    companion object {
        fun genererEksternReferanseIdForJournalpost(
            fagsakId: Long,
            behandlingId: Long?,
            mottakerInfo: MottakerInfo,
        ) = listOfNotNull(
            fagsakId,
            behandlingId,
            if (mottakerInfo is FullmektigEllerVerge) "verge" else null,
            MDC.get(MDCConstants.MDC_CALL_ID),
        ).joinToString("_")
    }
}
