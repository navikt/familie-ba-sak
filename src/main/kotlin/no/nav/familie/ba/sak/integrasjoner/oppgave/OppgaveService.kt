package no.nav.familie.ba.sak.integrasjoner.oppgave

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.DbOppgave
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.tilArbeidsfordelingsenhet
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(
    private val integrasjonKlient: IntegrasjonKlient,
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val opprettTaskService: OpprettTaskService,
    private val loggService: LoggService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService,
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
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
            val arbeidsfordelingsenhet =
                arbeidsfordelingPåBehandlingRepository
                    .hentArbeidsfordelingPåBehandling(behandlingId)
                    .tilArbeidsfordelingsenhet()

            val navIdent = tilordnetNavIdent?.let { NavIdent(it) }
            val tilordnetRessurs = tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)

            val opprettOppgave =
                OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = behandling.fagsak.aktør.aktørId, gruppe = IdentGruppe.AKTOERID),
                    saksId = fagsakId.toString(),
                    tema = Tema.BAR,
                    oppgavetype = oppgavetype,
                    fristFerdigstillelse = fristForFerdigstillelse,
                    beskrivelse = lagOppgaveTekst(fagsakId, beskrivelse),
                    enhetsnummer = arbeidsfordelingsenhet.enhetId,
                    behandlingstema = behandling.tilOppgaveBehandlingTema().value,
                    behandlingstype = behandling.kategori.tilOppgavebehandlingType().value,
                    tilordnetRessurs = tilordnetRessurs?.ident,
                    behandlesAvApplikasjon =
                        when {
                            oppgavetyperSomBehandlesAvBaSak.contains(oppgavetype) -> "familie-ba-sak"
                            manuellOppgaveType?.settBehandlesAvApplikasjon == true -> "familie-ba-sak"
                            else -> null
                        },
                )
            val opprettetOppgaveId = integrasjonKlient.opprettOppgave(opprettOppgave).oppgaveId.toString()

            val oppgave = DbOppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = oppgavetype)
            oppgaveRepository.save(oppgave)

            økTellerForAntallOppgaveTyper(oppgavetype)

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
        val opprettetOppgaveId = integrasjonKlient.opprettOppgave(opprettOppgave).oppgaveId.toString()

        økTellerForAntallOppgaveTyper(oppgavetype)

        return opprettetOppgaveId
    }

    fun opprettOppgaveForFinnmarksOgSvalbardtillegg(
        fagsakId: Long,
        beskrivelse: String,
    ): String {
        logger.info("Sender autovedtak til manuell behandling, se secureLogger for mer detaljer.")
        secureLogger.info("Sender autovedtak til manuell behandling. Beskrivelse: $beskrivelse")

        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)
                ?: throw Feil("Finner ikke siste vedtatte behandling for fagsak $fagsakId")

        val arbeidsfordelingsenhet =
            arbeidsfordelingPåBehandlingRepository
                .hentArbeidsfordelingPåBehandling(sisteVedtatteBehandling.id)
                .tilArbeidsfordelingsenhet()

        val opprettOppgave =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = sisteVedtatteBehandling.fagsak.aktør.aktørId, gruppe = IdentGruppe.AKTOERID),
                saksId = fagsakId.toString(),
                tema = Tema.BAR,
                oppgavetype = Oppgavetype.VurderLivshendelse,
                fristFerdigstillelse = LocalDate.now(),
                beskrivelse = lagOppgaveTekst(fagsakId, beskrivelse),
                enhetsnummer = arbeidsfordelingsenhet.enhetId,
                behandlingstema = sisteVedtatteBehandling.tilOppgaveBehandlingTema().value,
                behandlingstype = sisteVedtatteBehandling.kategori.tilOppgavebehandlingType().value,
                behandlesAvApplikasjon = null,
            )
        val opprettetOppgaveId = integrasjonKlient.opprettOppgave(opprettOppgave).oppgaveId.toString()

        økTellerForAntallOppgaveTyper(Oppgavetype.VurderLivshendelse)

        return opprettetOppgaveId
    }

    private fun økTellerForAntallOppgaveTyper(oppgavetype: Oppgavetype) {
        if (antallOppgaveTyper[oppgavetype] == null) {
            antallOppgaveTyper[oppgavetype] = Metrics.counter("oppgave.opprettet", "type", oppgavetype.name)
        }

        antallOppgaveTyper[oppgavetype]?.increment()
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse = integrasjonKlient.patchOppgave(patchOppgave)

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
                integrasjonKlient.tilordneEnhetOgRessursForOppgave(oppgaveId = oppgave.id!!, nyEnhet = nyEnhet)
            }
        }
    }

    fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String,
        overstyrFordeling: Boolean = false,
    ): String {
        if (!overstyrFordeling) {
            val oppgave = integrasjonKlient.finnOppgaveMedId(oppgaveId)
            if (oppgave.tilordnetRessurs != null) {
                throw FunksjonellFeil(
                    melding = "Oppgaven er allerede fordelt",
                    frontendFeilmelding = "Oppgaven er allerede fordelt til ${oppgave.tilordnetRessurs}",
                )
            }
        }

        return integrasjonKlient.fordelOppgave(oppgaveId, saksbehandler).oppgaveId.toString()
    }

    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): Oppgave {
        integrasjonKlient.fordelOppgave(oppgaveId, null)
        return integrasjonKlient.finnOppgaveMedId(oppgaveId)
    }

    fun hentOppgaverSomIkkeErFerdigstilt(
        oppgavetype: Oppgavetype,
        behandling: Behandling,
    ): List<DbOppgave> = oppgaveRepository.finnOppgaverSomSkalFerdigstilles(oppgavetype, behandling)

    fun hentOppgaverSomIkkeErFerdigstilt(behandling: Behandling): List<DbOppgave> = oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling)

    fun hentOppgave(oppgaveId: Long): Oppgave = integrasjonKlient.finnOppgaveMedId(oppgaveId)

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
                integrasjonKlient.ferdigstillOppgave(gsakId.toLong())

                erFerdigstilt = true
                // I tilfelle noen av de andre dbOppgavene feiler
                oppgaveRepository.saveAndFlush(this)
            } catch (exception: Exception) {
                throw Feil(message = "Klarte ikke å ferdigstille oppgave med id $gsakId.", cause = exception)
            }
        }
    }

    fun settNyFristPåOppgaver(
        behandlingId: Long,
        nyFrist: LocalDate,
    ) {
        val dbOppgaver = oppgaveRepository.findByBehandlingIdAndIkkeFerdigstilt(behandlingId)

        dbOppgaver.forEach { dbOppgave ->
            val gammelOppgave = hentOppgave(dbOppgave.gsakId.toLong())
            val oppgaveErAvsluttet = gammelOppgave.ferdigstiltTidspunkt != null

            when {
                gammelOppgave.id == null -> {
                    logger.warn("Finner ikke oppgave ${dbOppgave.gsakId} ved oppdatering av frist")
                }

                gammelOppgave.fristFerdigstillelse == null -> {
                    logger.warn("Oppgave ${dbOppgave.gsakId} har ingen oppgavefrist ved oppdatering av frist")
                }

                oppgaveErAvsluttet -> {
                    logger.info("Oppgave ${dbOppgave.gsakId} er allerede avsluttet")
                }

                else -> {
                    val nyOppgave = gammelOppgave.copy(fristFerdigstillelse = nyFrist.toString())
                    logger.info("Oppgave ${dbOppgave.gsakId} endrer frist fra ${gammelOppgave.fristFerdigstillelse} til $nyFrist")
                    integrasjonKlient.oppdaterOppgave(nyOppgave.id!!, nyOppgave)
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
                gammelOppgave.id == null -> {
                    logger.warn("Finner ikke oppgave ${dbOppgave.gsakId} ved oppdatering av frist")
                }

                oppgaveErAvsluttet -> {}

                else -> {
                    val nyOppgave = gammelOppgave.copy(fristFerdigstillelse = nyFrist.toString())
                    integrasjonKlient.oppdaterOppgave(nyOppgave.id!!, nyOppgave)
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

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto = integrasjonKlient.hentOppgaver(finnOppgaveRequest)

    fun ferdigstillOppgave(oppgave: Oppgave) {
        require(oppgave.id != null) { "Oppgaven må ha en id for å kunne ferdigstilles" }
        integrasjonKlient.ferdigstillOppgave(oppgaveId = oppgave.id!!)
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
            logger.info("Fant ingen oppgaver å avslutte ved sending til godkjenner på behandling ${behandling.id}")

            return
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
