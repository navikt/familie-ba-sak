package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.AutovedtakFinnmarkstilleggTask
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.AutovedtakSvalbardtilleggTask
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.dto.AutobrevOpphørSmåbarnstilleggDTO
import no.nav.familie.ba.sak.task.dto.AutobrevPgaAlderDTO
import no.nav.familie.ba.sak.task.dto.GrensesnittavstemmingTaskDTO
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.ba.sak.task.dto.OpprettOppgaveTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Task
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.util.Properties

@Service
class OpprettTaskService(
    private val taskRepository: TaskRepositoryWrapper,
    private val satskjøringRepository: SatskjøringRepository,
    private val envService: EnvService,
) {
    fun opprettOppgaveTask(
        behandlingId: Long,
        oppgavetype: Oppgavetype,
        beskrivelse: String? = null,
        fristForFerdigstillelse: LocalDate = LocalDate.now(),
    ) {
        taskRepository.save(
            OpprettOppgaveTask.opprettTask(
                behandlingId = behandlingId,
                oppgavetype = oppgavetype,
                fristForFerdigstillelse = fristForFerdigstillelse,
                beskrivelse = beskrivelse,
            ),
        )
    }

    fun opprettOppgaveForManuellBehandlingTask(
        behandlingId: Long,
        beskrivelse: String? = null,
        fristForFerdigstillelse: LocalDate = LocalDate.now(),
        manuellOppgaveType: ManuellOppgaveType,
    ) {
        taskRepository.save(
            Task(
                type = OpprettOppgaveTask.TASK_STEP_TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        OpprettOppgaveTaskDTO(
                            behandlingId,
                            Oppgavetype.VurderLivshendelse,
                            fristForFerdigstillelse,
                            null,
                            beskrivelse,
                            manuellOppgaveType,
                        ),
                    ),
            ),
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettOppgaveForFinnmarksOgSvalbardtilleggTask(
        fagsakId: Long,
        beskrivelse: String,
    ) {
        taskRepository.save(
            OpprettVurderLivshendelseOppgaveForFinnmarksOgSvalbardtilleggTask.opprettTask(
                fagsakId = fagsakId,
                beskrivelse = beskrivelse,
            ),
        )
    }

    fun opprettSendFeedTilInfotrygdTask(barnasIdenter: List<String>) {
        taskRepository.save(SendFødselsmeldingTilInfotrygdTask.opprettTask(barnasIdenter))
    }

    fun opprettSendStartBehandlingTilInfotrygdTask(aktørStoenadsmottaker: Aktør) {
        taskRepository.save(SendStartBehandlingTilInfotrygdTask.opprettTask(aktørStoenadsmottaker))
    }

    fun opprettSendAutobrevPgaAlderTask(
        fagsakId: Long,
        alder: Int,
    ) {
        val inneværendeMåned = inneværendeMåned()

        overstyrTaskMedNyCallId(IdUtils.generateId()) {
            taskRepository.save(
                Task(
                    type = SendAutobrevPgaAlderTask.TASK_STEP_TYPE,
                    payload =
                        objectMapper.writeValueAsString(
                            AutobrevPgaAlderDTO(
                                fagsakId = fagsakId,
                                alder = alder,
                                årMåned = inneværendeMåned,
                            ),
                        ),
                    properties =
                        Properties().apply {
                            this["fagsak"] = fagsakId.toString()
                            this["alder"] = alder.toString()
                            this["månedÅr"] = inneværendeMåned.tilMånedÅr()
                        },
                ),
            )
        }
    }

    fun opprettAutovedtakForOpphørSmåbarnstilleggTask(fagsakId: Long) {
        overstyrTaskMedNyCallId(IdUtils.generateId()) {
            taskRepository.save(
                Task(
                    type = SendAutobrevOpphørSmåbarnstilleggTask.TASK_STEP_TYPE,
                    payload =
                        objectMapper.writeValueAsString(
                            AutobrevOpphørSmåbarnstilleggDTO(
                                fagsakId = fagsakId,
                            ),
                        ),
                    properties =
                        Properties().apply {
                            this["fagsakId"] = fagsakId.toString()
                        },
                ),
            )
        }
    }

    fun opprettAktiverMinsideTask(aktør: Aktør) {
        taskRepository.save(AktiverMinsideTask.opprettTask(aktør))
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettSatsendringTask(
        fagsakId: Long,
        satstidspunkt: YearMonth,
    ) {
        if (satskjøringRepository.findByFagsakIdAndSatsTidspunkt(fagsakId, satstidspunkt) == null) {
            satskjøringRepository.save(Satskjøring(fagsakId = fagsakId, satsTidspunkt = satstidspunkt))
        }
        overstyrTaskMedNyCallId(IdUtils.generateId()) {
            taskRepository.save(
                Task(
                    type = SatsendringTask.TASK_STEP_TYPE,
                    payload = objectMapper.writeValueAsString(SatsendringTaskDto(fagsakId, satstidspunkt)),
                    properties =
                        Properties().apply {
                            this["fagsakId"] = fagsakId.toString()
                        },
                ),
            )
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettAutovedtakFinnmarkstilleggTask(
        fagsakId: Long,
    ) {
        overstyrTaskMedNyCallId(IdUtils.generateId()) {
            taskRepository.save(
                Task(
                    type = AutovedtakFinnmarkstilleggTask.TASK_STEP_TYPE,
                    payload = fagsakId.toString(),
                    properties =
                        Properties().apply {
                            this["fagsakId"] = fagsakId.toString()
                        },
                ).run {
                    if (envService.erProd()) {
                        medTriggerTid(utledNesteTriggerTidIHverdagerForTask(minimumForsinkelse = Duration.ofHours(1)))
                    } else {
                        this
                    }
                },
            )
        }
    }

    @Transactional
    fun opprettAutovedtakSvalbardtilleggTasker(
        fagsakIder: Collection<Long>,
    ) {
        fagsakIder.forEach { fagsakId ->
            overstyrTaskMedNyCallId(IdUtils.generateId()) {
                taskRepository.save(
                    Task(
                        type = AutovedtakSvalbardtilleggTask.TASK_STEP_TYPE,
                        payload = fagsakId.toString(),
                        properties =
                            Properties().apply {
                                this["fagsakId"] = fagsakId.toString()
                            },
                    ).apply {
                        if (envService.erProd()) {
                            medTriggerTid(utledNesteTriggerTidIHverdagerForTask(minimumForsinkelse = Duration.ofHours(1)))
                        }
                    },
                )
            }
        }
    }

    @Transactional
    fun opprettHenleggBehandlingTask(
        behandlingId: Long,
        årsak: HenleggÅrsak,
        begrunnelse: String,
        validerOppgavefristErEtterDato: LocalDate? = null,
    ) {
        taskRepository.save(
            Task(
                type = HenleggBehandlingTask.TASK_STEP_TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        HenleggBehandlingTaskDTO(
                            behandlingId = behandlingId,
                            årsak = årsak,
                            begrunnelse = begrunnelse,
                            validerOppgavefristErEtterDato = validerOppgavefristErEtterDato,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["behandlingId"] = behandlingId.toString()
                    },
            ),
        )
    }

    @Transactional
    fun opprettTaskForÅPatcheMergetIdent(
        dto: PatchMergetIdentDto,
    ) = taskRepository.save(
        Task(
            type = PatchMergetIdentTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(dto),
            properties =
                Properties().apply {
                    this["fagsakId"] = dto.fagsakId.toString()
                    this["gammelIdent"] = dto.gammelIdent.ident
                    this["nyIdent"] = dto.nyIdent.ident
                },
        ),
    )

    @Transactional
    fun opprettTaskForÅPatcheAktørIdent(
        dto: PatchMergetAktørDto,
    ) = taskRepository.save(
        Task(
            type = PatchMergetIdentTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(dto),
            properties =
                Properties().apply {
                    this["fagsakId"] = dto.fagsakId.toString()
                    this["gammelAktørId"] = dto.gammelAktørId
                    this["nyAktørId"] = dto.nyAktørId
                },
        ),
    )

    @Transactional
    fun opprettTaskForÅPatcheVilkårFom(
        dto: PatchFomPåVilkårTilFødselsdato,
    ) {
        taskRepository.save(
            Task(
                type = PatchFomPåVilkårTilFødselsdatoTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(dto),
                properties =
                    Properties().apply {
                        this["behandlingId"] = dto.behandlingId.toString()
                    },
            ),
        )
    }

    @Transactional
    fun opprettGrensesnittavstemMotOppdragTask(
        dto: GrensesnittavstemmingTaskDTO,
    ) {
        taskRepository.save(
            Task(
                type = GrensesnittavstemMotOppdrag.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(dto),
                properties =
                    Properties().apply {
                        this["fomDato"] = dto.fomDato.toString()
                        this["tomDato"] = dto.tomDato.toString()
                    },
            ).medTriggerTid(
                dto.tomDato.toLocalDate().atTime(8, 0),
            ),
        )
    }

    companion object {
        const val RETRY_BACKOFF_5000MS = "\${retry.backoff.delay:5000}"

        fun <T> overstyrTaskMedNyCallId(
            callId: String,
            body: () -> T,
        ): T {
            val originalCallId = MDC.get(MDCConstants.MDC_CALL_ID) ?: null

            return try {
                MDC.put(MDCConstants.MDC_CALL_ID, callId)
                body()
            } finally {
                if (originalCallId == null) {
                    MDC.remove(MDCConstants.MDC_CALL_ID)
                } else {
                    MDC.put(MDCConstants.MDC_CALL_ID, originalCallId)
                }
            }
        }
    }
}
