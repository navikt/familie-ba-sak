package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.MidlertidigEnhetIAutomatiskBehandlingFeil
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.dto.BehandleFødselshendelseTaskDTO
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleFødselshendelseTask.TASK_STEP_TYPE,
    beskrivelse = "Setter i gang behandlingsløp for fødselshendelse",
    maxAntallFeil = 3,
)
class BehandleFødselshendelseTask(
    private val autovedtakStegService: AutovedtakStegService,
    private val velgFagsystemService: VelgFagSystemService,
    private val infotrygdFeedService: InfotrygdFeedService,
    private val personidentService: PersonidentService,
    private val startSatsendring: StartSatsendring,
    private val taskRepositoryWrapper: TaskRepositoryWrapper,
) : AsyncTaskStep {
    private val dagerSidenBarnBleFødt: DistributionSummary = Metrics.summary("foedselshendelse.dagersidenbarnfoedt")

    @WithSpan
    override fun doTask(task: Task) {
        val behandleFødselshendelseTaskDTO =
            jsonMapper.readValue(task.payload, BehandleFødselshendelseTaskDTO::class.java)

        val nyBehandling = behandleFødselshendelseTaskDTO.nyBehandling

        logger.info("Behandler fødselshendelse")
        secureLogger.info("Behandler fødselshendelse, mor=${nyBehandling.morsIdent}, barna=${nyBehandling.barnasIdenter}")

        nyBehandling.barnasIdenter.forEach {
            // En litt forenklet løsning for å hente fødselsdato uten å kalle PDL. Gir ikke helt riktige data, men godt nok.
            val dagerSidenBarnetBleFødt =
                ChronoUnit.DAYS.between(
                    @Suppress("DEPRECATION") Fødselsnummer(it).fødselsdato,
                    LocalDateTime.now(),
                )
            dagerSidenBarnBleFødt.record(dagerSidenBarnetBleFødt.toDouble())
        }

        try {
            when (velgFagsystemService.velgFagsystem(nyBehandling).first) {
                FagsystemRegelVurdering.SEND_TIL_BA -> {
                    val harOpprettetSatsendring =
                        startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(nyBehandling.morsIdent)
                    if (harOpprettetSatsendring) {
                        throw RekjørSenereException(
                            "Satsendring skal kjøre ferdig før man behandler fødselsehendelse",
                            LocalDateTime.now().plusMinutes(60),
                        )
                    }
                    autovedtakStegService.kjørBehandlingFødselshendelse(
                        mottakersAktør =
                            personidentService.hentAktør(
                                nyBehandling.morsIdent,
                            ),
                        nyBehandlingHendelse = nyBehandling,
                        førstegangKjørt = task.opprettetTid,
                    )
                }

                FagsystemRegelVurdering.SEND_TIL_INFOTRYGD -> {
                    infotrygdFeedService.sendTilInfotrygdFeed(nyBehandling.barnasIdenter)
                }
            }
        } catch (e: FunksjonellFeil) {
            val aktør = personidentService.hentAktør(nyBehandling.morsIdent)
            taskRepositoryWrapper.save(
                OpprettVurderFødselshendelseKonsekvensForYtelseOppgave.opprettTask(
                    aktør = aktør,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    beskrivelse = "Saksbehandler må vurdere konsekvens for ytelse fordi fødselshendelsen ikke kunne håndteres automatisk",
                ),
            )
        } catch (e: MidlertidigEnhetIAutomatiskBehandlingFeil) {
            logger.info("Kan ikke behandle fødselshendelse hvis mor ikke har norsk adresse. De må sende søknad.")
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "behandleFødselshendelseTask"
        private val logger = LoggerFactory.getLogger(BehandleFødselshendelseTask::class.java)

        fun opprettTask(behandleFødselshendelseTaskDTO: BehandleFødselshendelseTaskDTO): Task {
            val triggerTid = if (erKlokkenMellom21Og06()) utledKl06IdagEllerNesteDag() else LocalDateTime.now()
            return Task(
                type = TASK_STEP_TYPE,
                payload = jsonMapper.writeValueAsString(behandleFødselshendelseTaskDTO),
                properties =
                    Properties().apply {
                        this["morsIdent"] = behandleFødselshendelseTaskDTO.nyBehandling.morsIdent
                    },
            ).copy(
                triggerTid = triggerTid.plusDays(7),
            )
        }

        private fun erKlokkenMellom21Og06(localTime: LocalTime = LocalTime.now()): Boolean = localTime.isAfter(LocalTime.of(21, 0)) || localTime.isBefore(LocalTime.of(6, 0))

        private fun utledKl06IdagEllerNesteDag(date: LocalDateTime = LocalDateTime.now()): LocalDateTime =
            if (date.toLocalTime().isBefore(LocalTime.of(6, 0))) {
                date.withHour(6)
            } else {
                date.plusDays(1).withHour(6)
            }
    }
}
