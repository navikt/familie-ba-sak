package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.oppholdsadresseErPåSvalbardPåDato
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.tilAdresser
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.time.measureTimedValue

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask.TASK_STEP_TYPE,
    beskrivelse = "Finn personer med bostedsadresse eller delt bosted i Finnmark/Nord-Troms eller oppholdsadresse på Svalbard",
    maxAntallFeil = 1,
)
class FinnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask(
    private val systemOnlyPdlRestClient: SystemOnlyPdlRestClient,
    private val fagsakService: FagsakService,
    private val personidentService: PersonidentService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val identer = task.payload.split(",")

        val (personerSomBorIFinnmarkNordTromsEllerPåSvalbard, tid) =
            measureTimedValue {
                identer
                    .chunked(1000)
                    .flatMap { identer ->
                        systemOnlyPdlRestClient
                            .hentBostedsadresseDeltBostedOgOppholdsadresseForPersoner(identer)
                            .mapNotNull { (ident, adresser) ->
                                val dato = LocalDate.now()

                                val borIFinnmarkEllerNordTroms =
                                    adresser
                                        .tilAdresser()
                                        .bostedsadresseEllerDeltBostedErIFinnmarkEllerNordTromsPåDato(dato)

                                val borPåSvalbard =
                                    adresser
                                        .oppholdsadresse
                                        .oppholdsadresseErPåSvalbardPåDato(dato)

                                if (borIFinnmarkEllerNordTroms || borPåSvalbard) {
                                    val aktør = personidentService.hentAktør(ident)
                                    val fagsakIder =
                                        fagsakService
                                            .finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør)
                                            .map { it.id }
                                            .distinct()

                                    ident to
                                        BorIFinnmarkNordTromsEllerPåSvalbard(
                                            borIFinnmarkNordTroms = borIFinnmarkEllerNordTroms,
                                            borPåSvalbard = borPåSvalbard,
                                            fagsakIder = fagsakIder,
                                        )
                                } else {
                                    null
                                }
                            }
                    }.toMap()
            }

        val personerSomBorIFinnmarkEllerNordTroms = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.borIFinnmarkNordTroms }
        val personerSomBorPåSvalbard = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.borPåSvalbard }
        val fagsakerMedPersonerSomBorIFinnmarkEllerNordTroms = personerSomBorIFinnmarkEllerNordTroms.values.flatMap { it.fagsakIder }.distinct()
        val fagsakerMedPersonerSomBorPåSvalbard = personerSomBorPåSvalbard.values.flatMap { it.fagsakIder }.distinct()

        task.metadata["Antall personer med bostedsadresse eller delt bosted i Finnmark"] = personerSomBorIFinnmarkEllerNordTroms.size
        task.metadata["Antall personer med oppholdsadresse på Svalbard"] = personerSomBorPåSvalbard.size
        task.metadata["Antall fagsaker der minst én person har bostedsadresse eller delt bosted i Finnmark"] = fagsakerMedPersonerSomBorIFinnmarkEllerNordTroms.size
        task.metadata["Antall fagsaker der minst én person har oppholdsadresse på Svalbard"] = fagsakerMedPersonerSomBorPåSvalbard.size
        task.metadata["Tid brukt på å kjøre task"] = "${tid.inWholeMilliseconds} ms"
    }

    companion object {
        const val TASK_STEP_TYPE = "finnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask"

        fun opprettTask(
            identer: List<String>,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = identer.joinToString(","),
            )
    }

    private data class BorIFinnmarkNordTromsEllerPåSvalbard(
        val borIFinnmarkNordTroms: Boolean,
        val borPåSvalbard: Boolean,
        val fagsakIder: List<Long>,
    )
}
