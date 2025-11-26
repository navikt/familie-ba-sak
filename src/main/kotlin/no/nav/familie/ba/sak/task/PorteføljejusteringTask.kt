package no.nav.familie.ba.sak.task

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.task.PorteføljejusteringTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Finner oppgaver som skal flyttes til ny enhet og oppretter tasker for å oppdatere enhet",
)
class PorteføljejusteringTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                tema = Tema.BAR,
                enhet = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                offset = 0,
                limit = task.payload.toLong(),
            )
        val finnOppgaveResponseDto: FinnOppgaveResponseDto = oppgaveService.hentOppgaver(finnOppgaveRequest)
        val oppgaveGruppereringer = finnOppgaveResponseDto.oppgaver.tilOppgaveGrupperinger()
        val grupperteOppgaver =
            grupperOppgaverEtterSaksreferanseBehandlesAvApplikasjonOgOppgavetype(oppgaveGruppereringer)
        secureLogger.info(objectMapper.writeValueAsString(grupperteOppgaver))
        // TODO: Legg inn logikk for å opprette tasker som oppdaterer enhet på oppgavene. Kommer i en senere PR når håndtering av porteføljejustering er ferdig avklart.
    }

    private fun List<Oppgave>.tilOppgaveGrupperinger(): List<OppgaveGruppering> =
        this.map {
            when (it.saksreferanse) {
                null -> {
                    // Tror disse kan oppdateres med ny enhet uten at det vil påvirke fagsystemene negativt
                    OppgaveUtenSaksreferanse(
                        id = it.id!!,
                        oppgavetype = it.oppgavetype,
                        behandlesAvApplikasjon = it.behandlesAvApplikasjon,
                    )
                }

                else -> {
                    // For disse må vi nok kommunisere med de ulike fagsystemene slik at de ikke kommer ut av synk ved oppdatering av enhet. Ihvertfall for BehandleSak, GodkjenneVedtak og BehandleUnderkjentVedtak.
                    // For de øvrige oppgavetypene som måtte dukke opp her er jeg usikker på om det er nødvendig. Mulig oppgavene kan oppdateres uten å "si ifra" til noen.
                    OppgaveMedSaksreferanse(
                        id = it.id!!,
                        oppgavetype = it.oppgavetype,
                        behandlesAvApplikasjon =
                            it.behandlesAvApplikasjon ?: if (it.saksreferanse!!.matches(Regex("\\d+[A-Z]\\d+"))) {
                                "infotrygd"
                            } else {
                                null
                            },
                        saksreferanse = it.saksreferanse!!,
                    )
                }
            }
        }

    private fun grupperOppgaverEtterSaksreferanseBehandlesAvApplikasjonOgOppgavetype(
        oppgaveGrupperinger: List<OppgaveGruppering>,
    ): Map<String, Map<String, Map<String, Int>>> =
        oppgaveGrupperinger
            .groupBy { it::class.simpleName ?: "null" }
            .mapValues { (_, oppgaveGrupperinger) ->
                oppgaveGrupperinger
                    .groupBy {
                        it.behandlesAvApplikasjon ?: "null"
                    }.mapValues { (_, oppgaveGrupperinger) ->
                        oppgaveGrupperinger.groupingBy { it.oppgavetype ?: "null" }.eachCount()
                    }
            }

    sealed interface OppgaveGruppering {
        val id: Long
        val oppgavetype: String?
        val behandlesAvApplikasjon: String?
    }

    data class OppgaveMedSaksreferanse(
        override val id: Long,
        override val oppgavetype: String?,
        override val behandlesAvApplikasjon: String?,
        val saksreferanse: String,
    ) : OppgaveGruppering

    data class OppgaveUtenSaksreferanse(
        override val id: Long,
        override val oppgavetype: String?,
        override val behandlesAvApplikasjon: String?,
    ) : OppgaveGruppering

    companion object {
        val logger = LoggerFactory.getLogger(PorteføljejusteringTask::class.java)
        const val TASK_STEP_TYPE = "porteføljejusteringTask"

        fun opprettTask(antallOppgaver: Long): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = antallOppgaver.toString(),
            )
    }
}
