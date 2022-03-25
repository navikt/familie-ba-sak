package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
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
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    private val opprettTaskService: OpprettTaskService,
    private val loggService: LoggService
) {

    private val antallOppgaveTyper: MutableMap<Oppgavetype, Counter> = mutableMapOf()

    fun opprettOppgave(
        behandlingId: Long,
        oppgavetype: Oppgavetype,
        fristForFerdigstillelse: LocalDate,
        tilordnetNavIdent: String? = null,
        beskrivelse: String? = null
    ): String {

        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val fagsakId = behandling.fagsak.id

        val eksisterendeOppgave =
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling)

        return if (eksisterendeOppgave != null &&
            oppgavetype != Oppgavetype.Journalføring
        ) {
            logger.warn(
                "Fant eksisterende oppgave med samme oppgavetype som ikke er ferdigstilt ved opprettelse av ny oppgave $eksisterendeOppgave. " +
                    "Vi oppretter ikke ny oppgave, men gjenbruker eksisterende."
            )

            eksisterendeOppgave.gsakId
        } else {
            val arbeidsfordelingsenhet =
                arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)

            if (arbeidsfordelingsenhet == null) {
                logger.warn("Fant ikke behandlende enhet på behandling ${behandling.id} ved opprettelse av $oppgavetype-oppgave.")
            }

            val opprettOppgave = OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = behandling.fagsak.aktør.aktørId, gruppe = IdentGruppe.AKTOERID),
                saksId = fagsakId.toString(),
                tema = Tema.BAR,
                oppgavetype = oppgavetype,
                fristFerdigstillelse = fristForFerdigstillelse,
                beskrivelse = lagOppgaveTekst(fagsakId, beskrivelse),
                enhetsnummer = arbeidsfordelingsenhet?.behandlendeEnhetId,
                behandlingstema = behandling.underkategori.tilOppgaveBehandlingTema().value,
                behandlingstype = behandling.kategori.tilOppgavebehandlingType().value,
                tilordnetRessurs = tilordnetNavIdent
            )
            val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgave).oppgaveId.toString()

            val oppgave = DbOppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = oppgavetype)
            oppgaveRepository.save(oppgave)

            økTellerForAntallOppgaveTyper(oppgavetype)

            opprettetOppgaveId
        }
    }

    fun opprettOppgaveForManuellBehandling(
        behandling: Behandling,
        oppgavetype: Oppgavetype,
        begrunnelse: String = "",
        opprettLogginnslag: Boolean = false
    ): String {
        logger.info("Sender autovedtak til manuell behandling, se secureLogger for mer detaljer.")
        secureLogger.info("Sender autovedtak til manuell behandling. Begrunnelse: $begrunnelse")
        opprettTaskService.opprettOppgaveTask(
            behandlingId = behandling.id,
            oppgavetype = oppgavetype,
            beskrivelse = begrunnelse
        )

        if (opprettLogginnslag) {
            loggService.opprettAutovedtakTilManuellBehandling(
                behandling = behandling,
                tekst = begrunnelse
            )
        }

        return begrunnelse
    }

    private fun økTellerForAntallOppgaveTyper(oppgavetype: Oppgavetype) {
        if (antallOppgaveTyper[oppgavetype] == null) {
            antallOppgaveTyper[oppgavetype] = Metrics.counter("oppgave.opprettet", "type", oppgavetype.name)
        }

        antallOppgaveTyper[oppgavetype]?.increment()
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        return integrasjonClient.patchOppgave(patchOppgave)
    }

    fun patchOppgaverForBehandling(behandling: Behandling, copyOppgave: (oppgave: Oppgave) -> Oppgave?) {
        hentOppgaverSomIkkeErFerdigstilt(behandling).forEach { dbOppgave ->
            val oppgave = hentOppgave(dbOppgave.gsakId.toLong())
            copyOppgave(oppgave)?.also { patchOppgave(it) }
        }
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String, overstyrFordeling: Boolean = false): String {
        if (!overstyrFordeling) {
            val oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)
            if (oppgave.tilordnetRessurs != null) {
                throw FunksjonellFeil(
                    melding = "Oppgaven er allerede fordelt",
                    frontendFeilmelding = "Oppgaven er allerede fordelt til ${oppgave.tilordnetRessurs}"
                )
            }
        }

        return integrasjonClient.fordelOppgave(oppgaveId, saksbehandler).oppgaveId.toString()
    }

    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): Oppgave {
        integrasjonClient.fordelOppgave(oppgaveId, null)
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun hentOppgaverSomIkkeErFerdigstilt(oppgavetype: Oppgavetype, behandling: Behandling): List<DbOppgave> {
        return oppgaveRepository.finnOppgaverSomSkalFerdigstilles(oppgavetype, behandling)
    }

    fun hentOppgaverSomIkkeErFerdigstilt(behandling: Behandling): List<DbOppgave> {
        return oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling)
    }

    fun hentOppgave(oppgaveId: Long): Oppgave {
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun ferdigstillOppgaver(behandlingId: Long, oppgavetype: Oppgavetype) {
        oppgaveRepository.finnOppgaverSomSkalFerdigstilles(
            oppgavetype,
            behandlingRepository.finnBehandling(
                behandlingId
            )
        ).forEach {
            try {
                integrasjonClient.ferdigstillOppgave(it.gsakId.toLong())

                it.erFerdigstilt = true
                // Her sørger vi for at oppgaver som blir ferdigstilt riktig får samme status hos oss selv om en av de andre dbOppgavene feiler.
                oppgaveRepository.saveAndFlush(it)
            } catch (exception: Exception) {
                throw Feil(message = "Klarte ikke å ferdigstille oppgave med id ${it.gsakId}.", cause = exception)
            }
        }
    }

    fun forlengFrsitÅpneOppgaverPåBehandling(behandlingId: Long, forlengelse: Period) {
        val dbOppgaver = oppgaveRepository.findByBehandlingIdAndIkkeFerdigstilt(behandlingId)

        dbOppgaver.forEach { dbOppgave ->
            val gammelOppgave = hentOppgave(dbOppgave.gsakId.toLong())
            val oppgaveErAvsluttet = gammelOppgave.ferdigstiltTidspunkt != null

            when {
                gammelOppgave.id == null ->
                    logger.warn("Finner ikke oppgave ${dbOppgave.gsakId} ved oppdatering av frist")
                gammelOppgave.fristFerdigstillelse == null ->
                    logger.warn("Oppgave ${dbOppgave.gsakId} har ingen oppgavefrist ved oppdatering av frist")
                oppgaveErAvsluttet -> {}
                else -> {
                    val nyFrist = LocalDate.parse(gammelOppgave.fristFerdigstillelse!!).plus(forlengelse)
                    val nyOppgave = gammelOppgave.copy(fristFerdigstillelse = nyFrist?.toString())
                    integrasjonClient.oppdaterOppgave(nyOppgave.id!!, nyOppgave)
                }
            }
        }
    }

    fun settFrsitÅpneOppgaverPåBehandlingTil(behandlingId: Long, nyFrist: LocalDate) {
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

    fun lagOppgaveTekst(fagsakId: Long, beskrivelse: String? = null): String {
        return if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } +
            "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
            "https://barnetrygd.intern.nav.no/fagsak/$fagsakId"
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        return integrasjonClient.hentOppgaver(finnOppgaveRequest)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLoger")
    }
}
