package no.nav.familie.ba.sak.kjerne.porteføljejustering

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = PorteføljejusteringTask.TASK_STEP_TYPE,
    beskrivelse = "Flytt oppgave til riktig enhet",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class PorteføljejusteringTask(
    private val integrasjonKlient: IntegrasjonKlient,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val oppgaveId = task.payload.toLong()
        val oppgave = integrasjonKlient.finnOppgaveMedId(oppgaveId)

        val ident = oppgave.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
        if (ident == null) {
            throw Feil("Oppgave med id $oppgaveId er ikke tilknyttet en ident.")
        }

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
        val nyEnhetId = arbeidsfordelingsenheter.first().enhetId

        if (nyEnhetId == BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer) {
            logger.warn("Oppgave med id $oppgaveId tilhører midlertidig enhet, hopper ut av task.")
            return
        }

        val nyMappeId: String = TODO("mapping av mapper er ikke implementert")

        integrasjonKlient.tilordneEnhetOgMappeForOppgave(oppgaveId = oppgaveId, nyEnhet = nyEnhetId, nyMappe = nyMappeId)
    }

    companion object {
        const val TASK_STEP_TYPE = "porteføljejusteringTask"
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(oppgaveId: Long): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = oppgaveId.toString(),
                properties = Properties().apply { this["oppgaveId"] = oppgaveId.toString() },
            )
    }
}
