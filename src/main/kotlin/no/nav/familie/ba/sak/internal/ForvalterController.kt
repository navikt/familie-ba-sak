package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.RestartAvSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.collections.LinkedHashSet

@RestController
@RequestMapping("/api/forvalter")
@ProtectedWithClaims(issuer = "azuread")
class ForvalterController(
    private val oppgaveRepository: OppgaveRepository,
    private val integrasjonClient: IntegrasjonClient,
    private val restartAvSmåbarnstilleggService: RestartAvSmåbarnstilleggService,
    private val forvalterService: ForvalterService,
    private val oppgaveService: OppgaveService,
    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository,
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val hentOgPersisterService: BehandlingHentOgPersisterService,
    private val stegService: StegService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val autovedtakService: AutovedtakService,
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

    /**
     *   Steg 1 i å fikse NAV-12506. Denne identifiserer alle saker som man kan fikse automatisk.
     *
     *   Skal slettes etter opprydding ferdig
     */
    @PostMapping(path = ["/nav-12506/finn-mangelfull-migrering"])
    fun identifiserMigreringUtenSatsAlleMåneder(): ResponseEntity<Map<String, Set<Long>>> {
        val årMånedMedMuligMigreringsfeil = YearMonth.of(2023, 3)

        val muligeMigreringerMedManglendeSats = behandlingMigreringsinfoRepository.finnMuligeMigreringerMedManglendeSats(årMånedMedMuligMigreringsfeil.atDay(1))
        logger.info("Fant ${muligeMigreringerMedManglendeSats.size} saker med mulig manglende sats:  $muligeMigreringerMedManglendeSats")

        val sakerSomKanFikses = muligeMigreringerMedManglendeSats.fold(Pair(LinkedHashSet<Long>(), LinkedHashSet<Long>())) { accumulator, fagsakId ->
            try {
                validerAtFagsakKanFiksesAutomatisk(fagsakId, årMånedMedMuligMigreringsfeil)
                accumulator.first.add(fagsakId)
            } catch (e: IllegalStateException) {
                logger.info("Ignorerer automatisk fiks for fagsakId=$fagsakId pga: ${e.message}")
                if (e.message == "Siste aktive behandling er ENDRE_MIGRERINGSDATO") {
                    accumulator.second.add(fagsakId)
                }
            }
            accumulator
        }

        val returnMap = mutableMapOf<String, Set<Long>>()
        if (sakerSomKanFikses.first.isNotEmpty()) {
            returnMap["trengerEndreMigreringsdato"] = sakerSomKanFikses.first
        }
        if (sakerSomKanFikses.second.isNotEmpty()) {
            returnMap["harEndreMigreringsdato"] = sakerSomKanFikses.second
        }

        return ResponseEntity.ok(returnMap)
    }

    /**
     *   Steg 2 i å fikse NAV-12506. Denne oppretter endre migreringsdato for behandlinger. Må kjøres for de fagsakene
     *   som ligger i trengerEndreMigreringsdato returnert i Steg 1
     *
     *   Skal slettes etter opprydding ferdig
     */
    @PostMapping("/nav-12506/opprett-endremigreringsdato-behandling")
    @Transactional
    fun opprettEndreMigreringsdatoFor(@RequestBody fagsakListe: List<Long>) {
        fagsakListe.forEach { fagsakId ->
            try {
                if (hentOgPersisterService.finnAktivForFagsak(fagsakId)?.opprettetÅrsak != BehandlingÅrsak.MIGRERING) {
                    error("Siste aktive behandling er ikke MIGRERING")
                }
                opprettetEndreMigreringsdatobehandlingSimulerOgFerdistill(fagsakId)
            } catch (e: Exception) {
                logger.warn("Klarte ikke å opprette endre migreringsbehandling på fagsakId=$fagsakId", e)
            }
        }
    }

    /**
     *   Steg 3 i å fikse NAV-12506. Denne kjører satsendring på fagsaker. Må kjøres på de som kjørte uten problemer i
     *   steg 2 og de fagsakene med harEndreMigreringsdato i Steg 1
     *
     *   Skal slettes etter opprydding ferdig
     */
    @PostMapping("/nav-12506/kjor-satsendring")
    @Transactional
    fun kjørSatsendringFor(@RequestBody fagsakListe: List<Long>) {
        fagsakListe.forEach { fagsakId ->
            try {
                val fagsak = fagsakService.hentPåFagsakId(fagsakId)

                if (hentOgPersisterService.finnAktivForFagsak(fagsakId)?.opprettetÅrsak != BehandlingÅrsak.ENDRE_MIGRERINGSDATO) {
                    error("Siste aktive behandling er ikke ENDRE_MIGRERINGSDATO")
                }

                val nyBehandling = stegService.håndterNyBehandling(
                    NyBehandling(
                        behandlingType = BehandlingType.REVURDERING,
                        behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                        søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                        skalBehandlesAutomatisk = true,
                        fagsakId = fagsakId,
                    ),
                )

                val behandlingEtterVilkårsvurdering =
                    stegService.håndterVilkårsvurdering(nyBehandling)

                val opprettetVedtak =
                    autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                        behandlingEtterVilkårsvurdering,
                    )
                behandlingService.oppdaterStatusPåBehandling(nyBehandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
                val task = IverksettMotOppdragTask.opprettTask(nyBehandling, opprettetVedtak, SikkerhetContext.hentSaksbehandler())
                taskRepository.save(task)
            } catch (e: Exception) {
                logger.warn("Klarte ikke kjøre satsendring for fagsakId=$fagsakId", e)
            }
        }
    }

    private fun opprettetEndreMigreringsdatobehandlingSimulerOgFerdistill(fagsakId: Long) {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)

        val nyBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
                søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                skalBehandlesAutomatisk = true,
                fagsakId = fagsakId,
                nyMigreringsdato = LocalDate.of(2023, 2, 1),
            ),
        )

        val behandlingEtterBehandlingsresultat = stegService.håndterVilkårsvurdering(nyBehandling)
        val behandlingEtterSimulering = stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultat,
            null,
        )
        behandlingService.oppdaterStatusPåBehandling(nyBehandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        stegService.håndterFerdigstillBehandling(behandlingEtterSimulering)
    }

    private fun validerAtFagsakKanFiksesAutomatisk(fagsakId: Long, årMånedMedMuligMigreringsfeil: YearMonth) {
        if (hentOgPersisterService.erÅpenBehandlingPåFagsak(fagsakId)) {
            error("Fant åpen behandling")
        }

        val aktivBehandling =
            hentOgPersisterService.finnAktivForFagsak(fagsakId) ?: error("Ingen aktiv behandling for fagsakId=$fagsakId")

        if (aktivBehandling.fagsak.status != FagsakStatus.LØPENDE) {
            error("Fagsak er ikke løpende")
        }

        if (aktivBehandling.opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO) {
            error("Siste aktive behandling er ENDRE_MIGRERINGSDATO")
        }

        if (aktivBehandling.opprettetÅrsak != BehandlingÅrsak.MIGRERING) {
            error("Siste aktive behandling er ikke migreringsbehandling")
        }

        val infotrygdSak =
            infotrygdBarnetrygdClient.hentSaker(listOf(aktivBehandling.fagsak.aktør.aktivFødselsnummer()))
        val migrertStønad =
            infotrygdSak.bruker.firstOrNull { it.stønad?.opphørsgrunn == "5" }?.stønad ?: error("Finner ikke stønad som er migrert") // Stønaden som er migrert er

        val virkningfom = 999999L - (
            migrertStønad.virkningFom?.toLong()
                ?: error("Mangler virkningFom")
            ) // seq dato: 999999-797790 - 202209

        val yearMonthSeqFomatter = DateTimeFormatter.ofPattern("yyyyMM")
        val virkningfomDate = YearMonth.parse(virkningfom.toString(), yearMonthSeqFomatter)

        if (virkningfomDate.isAfter(årMånedMedMuligMigreringsfeil)) {
            error("Virkningfom i Infotrygd er etter dato med mulig feil. Bør fikses manuelt")
        }
    }
}

data class GjenåpneOppgaverRequest(
    val behandlinger: List<Long>,
    val oppgavetype: Oppgavetype,
    val ferdigstiltFom: LocalDateTime,
    val ferdigstiltTom: LocalDateTime,
)
