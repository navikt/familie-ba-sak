package no.nav.familie.ba.sak.kjerne.porteføljejustering

import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.OSLO
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.STEINKJER
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.VADSØ
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.klage.KlageKlient
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.overstyrTaskMedNyCallId
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleSak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleUnderkjentVedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.GodkjenneVedtak
import no.nav.familie.log.IdUtils
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE,
    beskrivelse = "Flytt oppgave til riktig enhet",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class PorteføljejusteringFlyttOppgaveTask(
    private val integrasjonKlient: IntegrasjonKlient,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val klageKlient: KlageKlient,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val personidentService: PersonidentService,
    private val fagsakService: FagsakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val porteføljejusteringFlyttOppgaveDto: PorteføljejusteringFlyttOppgaveDto = objectMapper.readValue(task.payload)
        val oppgave = integrasjonKlient.finnOppgaveMedId(porteføljejusteringFlyttOppgaveDto.oppgaveId)
        if (oppgave.tildeltEnhetsnr != STEINKJER.enhetsnummer && oppgave.tildeltEnhetsnr != VADSØ.enhetsnummer && oppgave.tildeltEnhetsnr != OSLO.enhetsnummer) {
            logger.info("Oppgave med id ${porteføljejusteringFlyttOppgaveDto.oppgaveId} er ikke tildelt Steinkjer. Avbryter flytting av oppgave.")
            task.metadata["status"] = "Tildelt enhet på oppgave er ikke Steinkjer, Vadsø eller Oslo"
            return
        }

        val nyEnhetId = validerOgHentNyEnhetForOppgave(oppgave)
        if (nyEnhetId != OSLO.enhetsnummer && nyEnhetId != VADSØ.enhetsnummer) {
            logger.info("Oppgave med id ${porteføljejusteringFlyttOppgaveDto.oppgaveId} skal flyttes til enhet $nyEnhetId. Avbryter flytting av oppgave.")
            task.metadata["status"] = "Ny enhet for oppgave er ikke Oslo eller Vadsø"
            return
        }

        val nyMappeId = hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(oppgave.mappeId, nyEnhetId)

        integrasjonKlient.tilordneEnhetOgMappeForOppgave(
            oppgaveId = porteføljejusteringFlyttOppgaveDto.oppgaveId,
            nyEnhet = nyEnhetId,
            nyMappe = nyMappeId,
        )
        logger.info(
            "Oppdatert oppgave med id ${porteføljejusteringFlyttOppgaveDto.oppgaveId}.\n" +
                "Fra enhet ${oppgave.tildeltEnhetsnr} til ny enhet $nyEnhetId.\n" +
                "Fra mappe ${oppgave.mappeId} til ny mappe $nyMappeId.",
        )

        // Vi går bare videre med oppdatering i fagsystemer hvis typen er av BehandleSak, GodkjenndeVedtak eller BehandleUnderkjentVedtak
        // og oppgaven har en tilknyttet saksreferanse

        try {
            val saksreferanse = oppgave.saksreferanse
            when {
                saksreferanse == null -> {
                    return
                }

                oppgave.oppgavetype !in setOf(BehandleSak.value, GodkjenneVedtak.value, BehandleUnderkjentVedtak.value) -> {
                    return
                }

                oppgave.behandlesAvApplikasjon == "familie-ba-sak" -> {
                    oppdaterÅpenBehandlingIBaSak(oppgave, nyEnhetId)
                }

                oppgave.behandlesAvApplikasjon == "familie-klage" -> {
                    oppdaterEnhetPåÅpenBehandlingIKlage(porteføljejusteringFlyttOppgaveDto.oppgaveId, nyEnhetId)
                }

                oppgave.behandlesAvApplikasjon == "familie-tilbake" -> {
                    oppdaterEnhetPåÅpenBehandlingITilbakekreving(UUID.fromString(saksreferanse), nyEnhetId)
                }
            }
        } catch (e: Exception) {
            integrasjonKlient.tilordneEnhetOgMappeForOppgave(
                oppgaveId = porteføljejusteringFlyttOppgaveDto.oppgaveId,
                nyEnhet = porteføljejusteringFlyttOppgaveDto.originalEnhet,
                nyMappe = porteføljejusteringFlyttOppgaveDto.originalMappeId,
            )
            logger.info(
                "Ruller oppdatert oppgave tilbake med id ${porteføljejusteringFlyttOppgaveDto.oppgaveId}.\n" +
                    "Fra enhet $nyEnhetId til original enhet ${porteføljejusteringFlyttOppgaveDto.originalEnhet}.\n" +
                    "Fra mappe $nyMappeId til original mappe ${porteføljejusteringFlyttOppgaveDto.originalMappeId}.",
            )
            throw e
        }
        task.metadata["status"] = "Flytting av oppgave fullført"
        task.metadata["nyEnhetId"] = nyEnhetId
    }

    private fun validerOgHentNyEnhetForOppgave(
        oppgave: Oppgave,
    ): String {
        val ident =
            oppgave.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                ?: throw Feil("Oppgave med id ${oppgave.id} er ikke tilknyttet en ident.")

        val arbeidsfordelingsenheter = integrasjonKlient.hentBehandlendeEnhet(ident)
        if (arbeidsfordelingsenheter.isEmpty()) {
            logger.error("Fant ingen arbeidsfordelingsenheter for ident. Se SecureLogs for detaljer.")
            secureLogger.error("Fant ingen arbeidsfordelingsenheter for ident $ident.")
            throw Feil("Fant ingen arbeidsfordelingsenhet for ident.")
        }

        if (arbeidsfordelingsenheter.size > 1) {
            logger.error("Fant flere arbeidsfordelingsenheter for ident. Se SecureLogs for detaljer.")
            secureLogger.error("Fant flere arbeidsfordelingsenheter for ident $ident.")
            throw Feil("Fant flere arbeidsfordelingsenheter for ident.")
        }

        val nyEnhetId = arbeidsfordelingsenheter.single().enhetId
        if (nyEnhetId == STEINKJER.enhetsnummer) {
            throw Feil("Oppgave med id ${oppgave.id} tildeles fortsatt Steinkjer som enhet")
        }

        return nyEnhetId
    }

    private fun oppdaterÅpenBehandlingIBaSak(
        oppgave: Oppgave,
        nyEnhet: String,
    ) {
        val aktørIdPåOppgave = oppgave.aktoerId ?: throw Feil("Fant ikke aktørId på oppgave for å oppdatere åpen behandling i ba-sak")
        val aktørPåOppgave = personidentService.hentAktør(aktørIdPåOppgave)

        val åpenBehandlingPåAktør =
            fagsakService.hentNormalFagsak(aktørPåOppgave)?.let {
                behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(it.id)
            } ?: throw Feil("Fant ikke åpen behandling på aktør til oppgaveId ${oppgave.id}")

        arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(åpenBehandlingPåAktør, nyEnhet)
    }

    private fun oppdaterEnhetPåÅpenBehandlingITilbakekreving(
        behandlingEksternBrukId: UUID,
        nyEnhetId: String,
    ) {
        tilbakekrevingKlient.oppdaterEnhetPåÅpenBehandling(behandlingEksternBrukId, nyEnhetId)
    }

    private fun oppdaterEnhetPåÅpenBehandlingIKlage(
        oppgaveId: Long,
        nyEnhetId: String,
    ) {
        klageKlient.oppdaterEnhetPåÅpenBehandling(oppgaveId, nyEnhetId)
    }

    companion object {
        const val TASK_STEP_TYPE = "porteføljejusteringFlyttOppgaveTask"
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(
            oppgaveId: Long,
            enhetId: String,
            mappeId: Long?,
        ): Task =
            overstyrTaskMedNyCallId(IdUtils.generateId()) {
                Task(
                    type = TASK_STEP_TYPE,
                    payload =
                        objectMapper.writeValueAsString(
                            PorteføljejusteringFlyttOppgaveDto(
                                oppgaveId = oppgaveId,
                                originalEnhet = enhetId,
                                originalMappeId = mappeId,
                            ),
                        ),
                    properties =
                        Properties().apply {
                            this["oppgaveId"] = oppgaveId.toString()
                            enhetId.let { this["enhetId"] = it }
                            mappeId?.let { this["mappeId"] = it }
                        },
                )
            }
    }
}
