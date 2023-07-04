package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.RestartAvSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val oppgaveRepository: OppgaveRepository,
    private val integrasjonClient: IntegrasjonClient,
    private val restartAvSmåbarnstilleggService: RestartAvSmåbarnstilleggService,
    private val forvalterService: ForvalterService,
    private val oppgaveService: OppgaveService,
    private val behandlingRepository: BehandlingRepository,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(ForvalterController::class.java)

    @PostMapping(
        path = ["/ferdigstill-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun ferdigstillListeMedOppgaver(@RequestBody oppgaveListe: List<Long>): ResponseEntity<String> {
        var antallFeil = 0
        oppgaveListe.forEach { oppgaveId ->
            Result.runCatching {
                ferdigstillOppgave(oppgaveId)
            }.fold(
                onSuccess = { logger.info("Har ferdigstilt oppgave med oppgaveId=$oppgaveId") },
                onFailure = {
                    logger.warn("Klarte ikke å ferdigstille oppgaveId=$oppgaveId", it)
                    antallFeil = antallFeil.inc()
                },
            )
        }
        return ResponseEntity.ok("Ferdigstill oppgaver kjørt. Antall som ikke ble ferdigstilt: $antallFeil")
    }

    data class KopiertFagsakInfo(
        val fagsakId: Long,
        val feil: String = "",
        val trengerNyBehandling: Boolean = true,
    )

    data class KopierteEUAFagsakerRespons(
        val feilet: List<Long>,
        val kopierte: List<Long>,
        val fagsakerSomIkkeTrengerNyBehandling: List<Long>,
        val kopierteFagsakerInfo: List<KopiertFagsakInfo>,
    )

    @PostMapping(
        path = ["/kopier-eua-satsendring/{skalKopiere}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun kopierEndretUtbetalingAndelerFraForrigeBehandling(
        @RequestBody fagsakListe: List<Long>,
        @PathVariable skalKopiere: Boolean = false,
    ): ResponseEntity<KopierteEUAFagsakerRespons> {
        val kopierteFagsakerInfo = mutableListOf<KopiertFagsakInfo>()
        fagsakListe.parallelStream().forEach { fagsakId ->
            try {
                val behandlinger = behandlingRepository.finnBehandlinger(fagsakId)
                val sorterteVedtatteBehandlinger = behandlinger
                    .filter { it.steg == StegType.BEHANDLING_AVSLUTTET && !it.erHenlagt() }
                    .sortedByDescending { it.aktivertTidspunkt }

                val sisteVedtatteBehandling = sorterteVedtatteBehandlinger[0]
                val nestSisteVedtatteBehandling = sorterteVedtatteBehandlinger[1]

                if (!sisteVedtatteBehandling.erSatsendring() && sisteVedtatteBehandling.opprettetTidspunkt > LocalDate.of(
                        2023,
                        6,
                        19,
                    ).atStartOfDay()
                ) {
                    throw Exception("Det er gjennomført en ny behandling etter satsendring som har tatt med seg feilen videre. Fagsak $fagsakId, Behandling $sisteVedtatteBehandling.")
                }

                val sisteEndreteUtbetalingsAndeler =
                    endretUtbetalingAndelRepository.findByBehandlingId(sisteVedtatteBehandling.id)

                val nestSisteEndreteUtbetalingsAndeler =
                    endretUtbetalingAndelRepository.findByBehandlingId(nestSisteVedtatteBehandling.id)

                if (sisteEndreteUtbetalingsAndeler.isNotEmpty() || nestSisteEndreteUtbetalingsAndeler.isEmpty()) {
                    logger.info("Siste behandling har allerede EUA eller nest siste har ingen EUA og vi trenger ikke å kopiere. Fagsak $fagsakId")
                    kopierteFagsakerInfo.add(KopiertFagsakInfo(fagsakId = fagsakId, trengerNyBehandling = false))
                } else {
                    logger.info("Kopierer EndretUtbetalingAndel fra behandling $nestSisteVedtatteBehandling til $sisteVedtatteBehandling.")

                    if (skalKopiere) {
                        forvalterService.kopierEndretUtbetalingFraForrigeBehandling(
                            sisteVedtatteBehandling = sisteVedtatteBehandling,
                            nestSisteVedtatteBehandling = nestSisteVedtatteBehandling,
                        )
                    }
                    kopierteFagsakerInfo.add(KopiertFagsakInfo(fagsakId = fagsakId))
                }
            } catch (exception: Exception) {
                logger.warn(exception.message)
                logger.warn("Feil ved kopiering av EndretUtbetalingAndel fra nest siste behandling. Fagsak $fagsakId.")
                kopierteFagsakerInfo.add(
                    KopiertFagsakInfo(
                        fagsakId = fagsakId,
                        exception.message
                            ?: "Feil ved kopiering av EndretUtbetalingAndel fra nest siste behandling. Fagsak $fagsakId.",
                    ),
                )
            }
        }

        return ResponseEntity.ok(
            KopierteEUAFagsakerRespons(
                feilet = kopierteFagsakerInfo.filter { it.feil.isNotEmpty() }.map { it.fagsakId },
                kopierte = kopierteFagsakerInfo.filter { it.feil.isEmpty() && it.trengerNyBehandling }
                    .map { it.fagsakId },
                fagsakerSomIkkeTrengerNyBehandling = kopierteFagsakerInfo.filter { !it.trengerNyBehandling }
                    .map { it.fagsakId },
                kopierteFagsakerInfo = kopierteFagsakerInfo,
            ),
        )
    }

    @PostMapping(
        path = ["/gjenåpne-oppgaver"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun gjenåpneFeilaktigLukketOppgave(@RequestBody request: GjenåpneOppgaverRequest): ResponseEntity<Map<String, Set<Long>>> {
        val result = mutableSetOf<Long>()

        request.behandlinger.forEach { behandlingId ->
            val lagretDbOppgave = oppgaveRepository.findByBehandlingAndTypeAndErFerdigstilt(
                behandlingId,
                request.oppgavetype,
            ).maxBy { it.opprettetTidspunkt }

            if (lagretDbOppgave == null) {
                logger.info("Finner ikke oppgave for behandlingId=$behandlingId")
            } else {
                val eksisterendeOppgave =
                    oppgaveService.hentOppgave(lagretDbOppgave.gsakId.toLong()) // Sikre med å sjekke oppgavetype er det som er i input

                val ferdigstiltTid =
                    LocalDateTime.parse(
                        eksisterendeOppgave.ferdigstiltTidspunkt,
                        DateTimeFormatter.ISO_DATE_TIME,
                    ) // format "ferdigstiltTidspunkt": "2023-03-28T11:13:16.79+02:00",
                if (eksisterendeOppgave.status == StatusEnum.FERDIGSTILT &&
                    ferdigstiltTid.isAfter(request.ferdigstiltFom) &&
                    ferdigstiltTid.isBefore(
                        request.ferdigstiltTom,
                    ) &&
                    eksisterendeOppgave.tema == Tema.BAR &&
                    eksisterendeOppgave.oppgavetype == request.oppgavetype.value
                ) {
                    val klonetOppgave = OpprettOppgaveRequest(
                        ident = OppgaveIdentV2(
                            ident = lagretDbOppgave.behandling.fagsak.aktør.aktørId,
                            gruppe = IdentGruppe.AKTOERID,
                        ),
                        saksId = eksisterendeOppgave.saksreferanse,
                        tema = Tema.BAR,
                        oppgavetype = request.oppgavetype,
                        fristFerdigstillelse = if (eksisterendeOppgave.fristFerdigstillelse != null) { // "fristFerdigstillelse": "2023-03-28",
                            LocalDate.parse(
                                eksisterendeOppgave.fristFerdigstillelse,
                                DateTimeFormatter.ISO_DATE,
                            )
                        } else {
                            LocalDate.now()
                        },
                        beskrivelse = "--- Gjenåpnet ferdigstilt oppgave med id ${eksisterendeOppgave.id} ---\n" + eksisterendeOppgave.beskrivelse,
                        enhetsnummer = eksisterendeOppgave.tildeltEnhetsnr,
                        behandlingstema = eksisterendeOppgave.behandlingstema,
                        behandlingstype = eksisterendeOppgave.behandlingstype,
                        tilordnetRessurs = eksisterendeOppgave.tilordnetRessurs,
                        mappeId = eksisterendeOppgave.mappeId,
                        prioritet = eksisterendeOppgave.prioritet ?: OppgavePrioritet.NORM,
                    )

                    val oppgaveOpprettet = integrasjonClient.opprettOppgave(klonetOppgave)
                    logger.info("Gjenåpnet oppgave med id=${eksisterendeOppgave.id}. Ny oppgave har oppgaveId=${oppgaveOpprettet.oppgaveId}")
                    secureLogger.info("Gjenåpnet oppgave med id=${eksisterendeOppgave.id}. Ny oppgave har oppgaveId==$oppgaveOpprettet oppgave=$klonetOppgave")
                    result.add(behandlingId)
                } else {
                    logger.info(
                        "Ignorerer gjenoppretting av eksisterende oppgave: " +
                            "request=${request.oppgavetype}, oppgavetype=${request.oppgavetype}, behandlingId=$behandlingId, fom=${request.ferdigstiltFom}, tom=${request.ferdigstiltTom} \n" +
                            "eksisterendeOppgave=${eksisterendeOppgave.id}, ${eksisterendeOppgave.tema}, ${eksisterendeOppgave.oppgavetype}, ${eksisterendeOppgave.ferdigstiltTidspunkt}, ${eksisterendeOppgave.status}",
                    )
                }
            }
        }

        val returnValue = mutableMapOf<String, Set<Long>>()
        returnValue.put("oppprettet", result)
        returnValue.put("ignorert", request.behandlinger.toSet() - result)
        return ResponseEntity.ok(returnValue)
    }

    @PostMapping(
        path = ["/start-manuell-restart-av-smaabarnstillegg-jobb/skalOppretteOppgaver/{skalOppretteOppgaver}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun triggManuellStartAvSmåbarnstillegg(@PathVariable skalOppretteOppgaver: Boolean = true): ResponseEntity<String> {
        restartAvSmåbarnstilleggService.finnOgOpprettetOppgaveForSmåbarnstilleggSomSkalRestartesIDenneMåned(
            skalOppretteOppgaver,
        )
        return ResponseEntity.ok("OK")
    }

    private fun ferdigstillOppgave(oppgaveId: Long) {
        integrasjonClient.ferdigstillOppgave(oppgaveId)
        oppgaveRepository.findByGsakId(oppgaveId.toString()).also {
            if (it != null && !it.erFerdigstilt) {
                it.erFerdigstilt = true
                oppgaveRepository.saveAndFlush(it)
            }
        }
    }

    @PostMapping(path = ["/lag-og-send-utbetalingsoppdrag-til-økonomi"])
    fun lagOgSendUtbetalingsoppdragTilØkonomi(@RequestBody behandlinger: Set<Long>): ResponseEntity<String> {
        behandlinger.forEach {
            try {
                forvalterService.lagOgSendUtbetalingsoppdragTilØkonomiForBehandling(it)
            } catch (exception: Exception) {
                secureLogger.info(
                    "Kunne ikke sende behandling med id $it til økonomi" +
                        "\n$exception",
                )
            }
        }

        return ResponseEntity.ok("OK")
    }

    @PostMapping("/kjor-satsendring-uten-validering")
    @Transactional
    fun kjørSatsendringFor(@RequestBody fagsakListe: List<Long>) {
        fagsakListe.parallelStream().forEach { fagsakId ->
            try {
                logger.info("Kjører satsendring uten validering for $fagsakId")
                forvalterService.kjørForenkletSatsendringFor(fagsakId)
            } catch (e: Exception) {
                logger.warn("Klarte ikke kjøre satsendring for fagsakId=$fagsakId", e)
            }
        }
    }
}

data class GjenåpneOppgaverRequest(
    val behandlinger: List<Long>,
    val oppgavetype: Oppgavetype,
    val ferdigstiltFom: LocalDateTime,
    val ferdigstiltTom: LocalDateTime,
)
