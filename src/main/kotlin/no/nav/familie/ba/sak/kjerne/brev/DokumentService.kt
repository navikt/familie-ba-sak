package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerValidering
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Bruker
import no.nav.familie.ba.sak.kjerne.brev.mottaker.FullmektigEllerVerge
import no.nav.familie.ba.sak.kjerne.brev.mottaker.Institusjon
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerInfo
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.leggTilBlankAnnenVurdering
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.JournalførManueltBrevTask
import no.nav.familie.kontrakter.felles.Ressurs
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
