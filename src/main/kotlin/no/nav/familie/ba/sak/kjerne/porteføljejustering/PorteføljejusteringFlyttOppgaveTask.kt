package no.nav.familie.ba.sak.kjerne.porteføljejustering

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
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
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
        val oppgaveId = task.payload.toLong()
        val oppgave = integrasjonKlient.finnOppgaveMedId(oppgaveId)
        if (oppgave.tildeltEnhetsnr != STEINKJER.enhetsnummer) {
            logger.info("Oppgave med id $oppgaveId er ikke tildelt Steinkjer. Avbryter flytting av oppgave.")
            return
        }

        val nyEnhetId = validerOgHentNyEnhetForOppgave(oppgave)
        if (nyEnhetId != OSLO.enhetsnummer && nyEnhetId != VADSØ.enhetsnummer) {
            logger.info("Oppgave med id $oppgaveId skal flyttes til enhet $nyEnhetId. Avbryter flytting av oppgave.")
            return
        }

        val nyMappeId =
            oppgave.mappeId?.let {
                hentMappeIdHosOsloEllerVadsøSomTilsvarerMappeISteinkjer(
                    it.toInt(),
                    nyEnhetId,
                )
            }

        val skalOppdatereEnhetEllerMappe = nyMappeId != oppgave.mappeId?.toInt() || nyEnhetId != oppgave.tildeltEnhetsnr

        if (skalOppdatereEnhetEllerMappe) { // Vi oppdaterer bare hvis det er forskjell på enhet eller mappe. Kaster ikke feil grunnet ønsket om idempotens.
            integrasjonKlient.tilordneEnhetOgMappeForOppgave(
                oppgaveId = oppgaveId,
                nyEnhet = nyEnhetId,
                nyMappe = nyMappeId.toString(),
            )
            logger.info(
                "Oppdatert oppgave med id $oppgaveId." +
                    "Fra enhet ${oppgave.tildeltEnhetsnr} til ny enhet $nyEnhetId." +
                    "Fra mappe ${oppgave.mappeId} til ny mappe $nyMappeId ",
            )
        }

        // Vi går bare videre med oppdatering i fagsystemer hvis typen er av BehandleSak, GodkjenndeVedtak eller BehandleUnderkjentVedtak
        // og oppgaven har en tilknyttet saksreferanse

        val saksreferanse = oppgave.saksreferanse
        when {
            saksreferanse == null -> {
                return
            }

            oppgave.oppgavetype !in (listOf(Oppgavetype.BehandleSak.value, Oppgavetype.GodkjenneVedtak.value, Oppgavetype.BehandleUnderkjentVedtak.value)) -> {
                return
            }

            oppgave.behandlesAvApplikasjon == "familie-ba-sak" -> {
                oppdaterÅpenBehandlingIBaSak(oppgave, nyEnhetId)
            }

            // TODO I NAV-26753
            oppgave.behandlesAvApplikasjon == "familie-klage" -> {
                oppdaterEnhetPåÅpenBehandlingIKlage(oppgaveId, nyEnhetId)
            }

            oppgave.behandlesAvApplikasjon == "familie-tilbake" -> {
                oppdaterEnhetPåÅpenBehandlingITilbakekreving(UUID.fromString(saksreferanse), nyEnhetId)
            }
        }
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
            enhetId: String?,
            mappeId: String?,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = oppgaveId.toString(),
                properties =
                    Properties().apply {
                        this["oppgaveId"] = oppgaveId.toString()
                        enhetId?.let { this["enhetId"] = it }
                        mappeId?.let { this["mappeId"] = it }
                    },
            )
    }
}
