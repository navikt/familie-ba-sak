package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.erSvalbard
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.oppholdsadresseErPåSvalbardPåDato
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.tilAdresser
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.time.measureTimedValue

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask.TASK_STEP_TYPE,
    beskrivelse = "Finn personer med bostedsadresse eller delt bosted i Finnmark/Nord-Troms, oppholdsadresse på Svalbard eller D-nummer med geografisk tilknytning til Svalbard",
    maxAntallFeil = 1,
)
class FinnPersonerSomBorIFinnmarkNordTromsEllerPåSvalbardTask(
    private val systemOnlyPdlRestClient: SystemOnlyPdlRestClient,
    private val fagsakService: FagsakService,
    private val personidentService: PersonidentService,
) : AsyncTaskStep {
    @WithSpan
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

                                val harDNummerOgGeografiskTilknytningTilSvalbard by lazy {
                                    Fødselsnummer(ident).erDNummer &&
                                        systemOnlyPdlRestClient
                                            .hentGeografiskTilknytning(ident)
                                            .erSvalbard()
                                }

                                if (borIFinnmarkEllerNordTroms || borPåSvalbard || harDNummerOgGeografiskTilknytningTilSvalbard) {
                                    val aktør = personidentService.hentAktør(ident)
                                    val fagsakIder =
                                        fagsakService
                                            .finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør)
                                            .map { it.id }

                                    if (fagsakIder.isNotEmpty()) {
                                        ident to
                                            BorIFinnmarkNordTromsEllerPåSvalbard(
                                                borIFinnmarkNordTroms = borIFinnmarkEllerNordTroms,
                                                borPåSvalbard = borPåSvalbard,
                                                harDNummerOgGeografiskTilknytningTilSvalbard = harDNummerOgGeografiskTilknytningTilSvalbard,
                                                fagsakIder = fagsakIder,
                                            )
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                            }
                    }.toMap()
            }

        val personerSomBorIFinnmarkEllerNordTroms = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.borIFinnmarkNordTroms }
        val personerSomBorPåSvalbard = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.borPåSvalbard }
        val personerSomHarDNummerOgGeografiskTilknytningTilSvalbard = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.harDNummerOgGeografiskTilknytningTilSvalbard }
        val fagsakerMedPersonerSomBorIFinnmarkEllerNordTroms = personerSomBorIFinnmarkEllerNordTroms.values.flatMap { it.fagsakIder }.distinct()
        val fagsakerMedPersonerSomBorPåSvalbard = (personerSomBorPåSvalbard.values.flatMap { it.fagsakIder } + personerSomHarDNummerOgGeografiskTilknytningTilSvalbard.values.flatMap { it.fagsakIder }).distinct()

        task.metadata["BostedsadresseDeltBostedFinnmark"] = personerSomBorIFinnmarkEllerNordTroms.size
        task.metadata["OppholdsadresseSvalbard"] = personerSomBorPåSvalbard.size
        task.metadata["DNummerGeografiskTilknytningSvalbard"] = personerSomHarDNummerOgGeografiskTilknytningTilSvalbard.size
        task.metadata["FagsakerFinnmark"] = fagsakerMedPersonerSomBorIFinnmarkEllerNordTroms.size
        task.metadata["FagsakerSvalbard"] = fagsakerMedPersonerSomBorPåSvalbard.size
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
        val harDNummerOgGeografiskTilknytningTilSvalbard: Boolean,
        val fagsakIder: List<Long>,
    )
}
