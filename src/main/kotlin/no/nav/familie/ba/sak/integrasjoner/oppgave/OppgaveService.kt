package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum.FEILREGISTRERT
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum.FERDIGSTILT
import no.nav.familie.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(
    private val integrasjonClient: IntegrasjonClient,
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val opprettTaskService: OpprettTaskService,
    private val loggService: LoggService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val oppgaveArbeidsfordelingService: OppgaveArbeidsfordelingService,
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    private val unleashService: UnleashService,
) {
    private val antallOppgaveTyper: MutableMap<Oppgavetype, Counter> = mutableMapOf()

    fun opprettOppgave(
        behandlingId: Long,
        oppgavetype: Oppgavetype,
        fristForFerdigstillelse: LocalDate,
        tilordnetNavIdent: String? = null,
        beskrivelse: String? = null,
        manuellOppgaveType: ManuellOppgaveType? = null,
    ): String {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        val fagsakId = behandling.fagsak.id

        val eksisterendeOppgave =
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling)

        return if (eksisterendeOppgave != null && oppgavetype != Oppgavetype.Journalføring) {
            logger.warn(
                "Fant eksisterende oppgave med samme oppgavetype som ikke er ferdigstilt " +
                    "ved opprettelse av ny oppgave $eksisterendeOppgave. " +
                    "Vi oppretter ikke ny oppgave, men gjenbruker eksisterende.",
            )

            eksisterendeOppgave.gsakId
        } else {
            val arbeidsfordelingPåBehandling =
                arbeidsfordelingPåBehandlingRepository
                    .hentArbeidsfordelingPåBehandling(behandlingId)

            val opprettSakPåRiktigEnhetOgSaksbehandlerToggleErPå = unleashService.isEnabled(FeatureToggleConfig.OPPRETT_SAK_PÅ_RIKTIG_ENHET_OG_SAKSBEHANDLER, false)

            val oppgaveArbeidsfordeling =
                if (opprettSakPåRiktigEnhetOgSaksbehandlerToggleErPå) {
                    oppgaveArbeidsfordelingService.finnArbeidsfordelingForOppgave(
                        arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                        navIdent = tilordnetNavIdent?.let { NavIdent(it) },
                    )
                } else {
                    OppgaveArbeidsfordeling(
                        navIdent = tilordnetNavIdent?.let { NavIdent(it) },
                        enhetsnummer = arbeidsfordelingPåBehandling.behandlendeEnhetId,
                        enhetsnavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                    )
                }

            val opprettOppgave =
                OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = behandling.fagsak.aktør.aktørId, gruppe = IdentGruppe.AKTOERID),
                    saksId = fagsakId.toString(),
                    tema = Tema.BAR,
                    oppgavetype = oppgavetype,
                    fristFerdigstillelse = fristForFerdigstillelse,
                    beskrivelse = lagOppgaveTekst(fagsakId, beskrivelse),
                    enhetsnummer = oppgaveArbeidsfordeling.enhetsnummer,
                    behandlingstema = behandling.tilOppgaveBehandlingTema().value,
                    behandlingstype = behandling.kategori.tilOppgavebehandlingType().value,
                    tilordnetRessurs = oppgaveArbeidsfordeling.navIdent?.ident,
                    behandlesAvApplikasjon =
                        when {
                            oppgavetyperSomBehandlesAvBaSak.contains(oppgavetype) -> "familie-ba-sak"
                            manuellOppgaveType?.settBehandlesAvApplikasjon == true -> "familie-ba-sak"
                            else -> null
                        },
                )
            val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave).oppgaveId.toString()

            val oppgave = DbOppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = oppgavetype)
            oppgaveRepository.save(oppgave)

            økTellerForAntallOppgaveTyper(oppgavetype)

            if (opprettSakPåRiktigEnhetOgSaksbehandlerToggleErPå) {
                val erEnhetsnummerEndret = arbeidsfordelingPåBehandling.behandlendeEnhetId != oppgaveArbeidsfordeling.enhetsnummer

                if (erEnhetsnummerEndret) {
                    arbeidsfordelingPåBehandlingRepository.save(
                        arbeidsfordelingPåBehandling.copy(
                            behandlendeEnhetId = oppgaveArbeidsfordeling.enhetsnummer,
                            behandlendeEnhetNavn = oppgaveArbeidsfordeling.enhetsnavn,
                            manueltOverstyrt = false,
                        ),
                    )
                }
            }

            opprettetOppgaveId
        }
    }

    fun opprettOppgaveForManuellBehandling(
        behandlingId: Long,
        begrunnelse: String = "",
        opprettLogginnslag: Boolean = false,
        manuellOppgaveType: ManuellOppgaveType,
    ): String {
        logger.info("Sender autovedtak til manuell behandling, se secureLogger for mer detaljer.")
        secureLogger.info("Sender autovedtak til manuell behandling. Begrunnelse: $begrunnelse")
        opprettTaskService.opprettOppgaveForManuellBehandlingTask(
            behandlingId = behandlingId,
            beskrivelse = begrunnelse,
            manuellOppgaveType = manuellOppgaveType,
        )

        if (opprettLogginnslag) {
            loggService.opprettAutovedtakTilManuellBehandling(
                behandlingId = behandlingId,
                tekst = begrunnelse,
            )
        }

        return begrunnelse
    }

    fun opprettOppgaveForFødselshendelse(
        aktørId: String,
        oppgavetype: Oppgavetype,
        fristForFerdigstillelse: LocalDate,
        beskrivelse: String,
    ): String {
        val opprettOppgave =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = aktørId, gruppe = IdentGruppe.AKTOERID),
                tema = Tema.BAR,
                oppgavetype = oppgavetype,
                fristFerdigstillelse = fristForFerdigstillelse,
                beskrivelse = beskrivelse,
                saksId = null,
                behandlingstema = null,
                enhetsnummer = null,
            )
        val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave).oppgaveId.toString()

        økTellerForAntallOppgaveTyper(oppgavetype)

        return opprettetOppgaveId
    }

    private fun økTellerForAntallOppgaveTyper(oppgavetype: Oppgavetype) {
        if (antallOppgaveTyper[oppgavetype] == null) {
            antallOppgaveTyper[oppgavetype] = Metrics.counter("oppgave.opprettet", "type", oppgavetype.name)
        }

        antallOppgaveTyper[oppgavetype]?.increment()
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse = integrasjonClient.patchOppgave(patchOppgave)

    fun patchOppgaverForBehandling(
        behandling: Behandling,
        copyOppgave: (oppgave: Oppgave) -> Oppgave?,
    ) {
        hentOppgaverSomIkkeErFerdigstilt(behandling).forEach { dbOppgave ->
            val oppgave = hentOppgave(dbOppgave.gsakId.toLong())
            if (oppgave.status != FERDIGSTILT && oppgave.status != FEILREGISTRERT) {
                copyOppgave(oppgave)?.also { patchOppgave(it) }
            } else {
                logger.warn("Kan ikke patch'e ferdigstilt oppgave ${oppgave.id}, for behandling ${behandling.id}.")
                dbOppgave.erFerdigstilt = true
                oppgaveRepository.saveAndFlush(dbOppgave)
            }
        }
    }

    fun endreTilordnetEnhetPåOppgaverForBehandling(
        behandling: Behandling,
        nyEnhet: String,
    ) {
        hentOppgaverSomIkkeErFerdigstilt(behandling).forEach { dbOppgave ->
            val oppgave = hentOppgave(dbOppgave.gsakId.toLong())
            logger.info("Oppdaterer enhet fra ${oppgave.tildeltEnhetsnr} til $nyEnhet på oppgave ${oppgave.id}")
            if (oppgave.status == FERDIGSTILT && oppgave.oppgavetype == Oppgavetype.VurderLivshendelse.value) {
                dbOppgave.erFerdigstilt = true
            } else {
                val oppgaveOppdatering = Oppgave(id = oppgave.id, tildeltEnhetsnr = nyEnhet, tilordnetRessurs = null, mappeId = null)
                patchOppgave(oppgaveOppdatering)
            }
        }
    }

    fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String,
        overstyrFordeling: Boolean = false,
    ): String {
        if (!overstyrFordeling) {
            val oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)
            if (oppgave.tilordnetRessurs != null) {
                throw FunksjonellFeil(
                    melding = "Oppgaven er allerede fordelt",
                    frontendFeilmelding = "Oppgaven er allerede fordelt til ${oppgave.tilordnetRessurs}",
                )
            }
        }

        return integrasjonClient.fordelOppgave(oppgaveId, saksbehandler).oppgaveId.toString()
    }

    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): Oppgave {
        integrasjonClient.fordelOppgave(oppgaveId, null)
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun hentOppgaverSomIkkeErFerdigstilt(
        oppgavetype: Oppgavetype,
        behandling: Behandling,
    ): List<DbOppgave> = oppgaveRepository.finnOppgaverSomSkalFerdigstilles(oppgavetype, behandling)

    fun hentOppgaverSomIkkeErFerdigstilt(behandling: Behandling): List<DbOppgave> = oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling)

    fun hentOppgave(oppgaveId: Long): Oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)

    fun ferdigstillOppgaver(
        behandlingId: Long,
        oppgavetype: Oppgavetype,
    ) {
        oppgaveRepository
            .finnOppgaverSomSkalFerdigstilles(
                oppgavetype = oppgavetype,
                behandling =
                    behandlingHentOgPersisterService.hent(
                        behandlingId = behandlingId,
                    ),
            ).forEach {
                it.ferdigstill()
            }
    }

    private fun DbOppgave.ferdigstill() {
        val oppgave = hentOppgave(gsakId.toLong())

        if (oppgave.status == FERDIGSTILT || oppgave.status == FEILREGISTRERT) {
            erFerdigstilt = true

            // Her sørger vi for at oppgaver som blir ferdigstilt riktig får samme status hos oss selv om en av de andre dbOppgavene feiler.
            oppgaveRepository.saveAndFlush(this)
        } else {
            try {
                integrasjonClient.ferdigstillOppgave(gsakId.toLong())

                erFerdigstilt = true
                // I tilfelle noen av de andre dbOppgavene feiler
                oppgaveRepository.saveAndFlush(this)
            } catch (exception: Exception) {
                throw Feil(message = "Klarte ikke å ferdigstille oppgave med id $gsakId.", cause = exception)
            }
        }
    }

    fun forlengFristÅpneOppgaverPåBehandling(
        behandlingId: Long,
        forlengelse: Period,
    ) {
        val dbOppgaver = oppgaveRepository.findByBehandlingIdAndIkkeFerdigstilt(behandlingId)

        dbOppgaver.forEach { dbOppgave ->
            val gammelOppgave = hentOppgave(dbOppgave.gsakId.toLong())
            val oppgaveErAvsluttet = gammelOppgave.ferdigstiltTidspunkt != null

            when {
                gammelOppgave.id == null ->
                    logger.warn("Finner ikke oppgave ${dbOppgave.gsakId} ved oppdatering av frist")

                gammelOppgave.fristFerdigstillelse == null ->
                    logger.warn("Oppgave ${dbOppgave.gsakId} har ingen oppgavefrist ved oppdatering av frist")

                oppgaveErAvsluttet -> {
                    logger.info("Oppgave ${dbOppgave.gsakId} er allerede avsluttet")
                }

                else -> {
                    val nyFrist = LocalDate.parse(gammelOppgave.fristFerdigstillelse!!).plus(forlengelse)
                    val nyOppgave = gammelOppgave.copy(fristFerdigstillelse = nyFrist.toString())
                    logger.info("Oppgave ${dbOppgave.gsakId} endrer frist fra ${gammelOppgave.fristFerdigstillelse} til $nyFrist")
                    integrasjonClient.oppdaterOppgave(nyOppgave.id!!, nyOppgave)
                }
            }
        }
    }

    fun hentFristerForÅpneUtvidetBarnetrygdBehandlinger(): String {
        val åpneUtvidetBarnetrygdBehandlinger = behandlingRepository.finnÅpneUtvidetBarnetrygdBehandlinger()

        val behandlingsfrister =
            åpneUtvidetBarnetrygdBehandlinger
                .map { behandling ->
                    val behandleSakOppgave =
                        try {
                            oppgaveRepository
                                .findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.BehandleSak, behandling)
                                ?.let {
                                    hentOppgave(it.gsakId.toLong())
                                }
                        } catch (e: Exception) {
                            secureLogger.warn("Klarte ikke hente BehandleSak-oppgaven for behandling ${behandling.id}", e)
                            null
                        }
                    "${behandling.id};${behandleSakOppgave?.id};${behandleSakOppgave?.fristFerdigstillelse}\n"
                }.reduce { csvString, behandlingsfrist -> csvString + behandlingsfrist }

        return "behandlingId;oppgaveId;frist\n" + behandlingsfrister
    }

    fun settFristÅpneOppgaverPåBehandlingTil(
        behandlingId: Long,
        nyFrist: LocalDate,
    ) {
        val dbOppgaver = oppgaveRepository.findByBehandlingIdAndIkkeFerdigstilt(behandlingId)

        dbOppgaver.forEach { dbOppgave ->
            val gammelOppgave = hentOppgave(dbOppgave.gsakId.toLong())
            val oppgaveErAvsluttet = gammelOppgave.ferdigstiltTidspunkt != null

            when {
                gammelOppgave.id == null -> logger.warn("Finner ikke oppgave ${dbOppgave.gsakId} ved oppdatering av frist")
                oppgaveErAvsluttet -> {}
                else -> {
                    val nyOppgave = gammelOppgave.copy(fristFerdigstillelse = nyFrist.toString())
                    integrasjonClient.oppdaterOppgave(nyOppgave.id!!, nyOppgave)
                }
            }
        }
    }

    fun lagOppgaveTekst(
        fagsakId: Long,
        beskrivelse: String? = null,
    ): String =
        if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } +
            "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
            "https://barnetrygd.intern.nav.no/fagsak/$fagsakId"

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto = integrasjonClient.hentOppgaver(finnOppgaveRequest)

    fun ferdigstillOppgave(oppgave: Oppgave) {
        require(oppgave.id != null) { "Oppgaven må ha en id for å kunne ferdigstilles" }
        integrasjonClient.ferdigstillOppgave(oppgaveId = oppgave.id!!)
    }

    fun ferdigstillLagVedtakOppgaver(behandlingId: Long) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val oppgaverPåBehandling = oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling)

        val lagVedtakOppgaver =
            oppgaverPåBehandling.filter {
                it.type in
                    listOf(
                        Oppgavetype.BehandleSak,
                        Oppgavetype.BehandleUnderkjentVedtak,
                        Oppgavetype.VurderLivshendelse,
                    )
            }

        if (lagVedtakOppgaver.isEmpty() &&
            !behandling.skalBehandlesAutomatisk &&
            !behandling.erMigrering() &&
            !behandling.erTekniskEndring()
        ) {
            throw Feil("Fant ingen oppgaver å avslutte ved sending til godkjenner på $behandling")
        }

        lagVedtakOppgaver
            .filter { !it.erFerdigstilt }
            .forEach {
                logger.info("Ferdigstiller ${it.type}-oppgave på behandling $behandlingId")
                it.ferdigstill()
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
        private val oppgavetyperSomBehandlesAvBaSak =
            listOf(
                Oppgavetype.BehandleSak,
                Oppgavetype.GodkjenneVedtak,
                Oppgavetype.BehandleUnderkjentVedtak,
            )
    }
}
