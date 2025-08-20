package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.erSvalbard
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.oppholdsadresseErPåSvalbardPåDato
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.tilAdresser
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.erIFinnmarkEllerNordTroms
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.hentForDato
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

                                val harBostedsadresseIFinnmarkEllerNordTroms =
                                    adresser
                                        .tilAdresser()
                                        .bostedsadresser
                                        .hentForDato(dato)
                                        ?.erIFinnmarkEllerNordTroms() ?: false

                                val harDeltBostedIFinnmarkEllerNordTroms =
                                    adresser
                                        .tilAdresser()
                                        .delteBosteder
                                        .hentForDato(dato)
                                        ?.erIFinnmarkEllerNordTroms() ?: false

                                val harOppholdsadressePåSvalbard =
                                    adresser
                                        .oppholdsadresse
                                        .oppholdsadresseErPåSvalbardPåDato(dato)

                                val harDNummerOgGeografiskTilknytningTilSvalbard by lazy {
                                    Fødselsnummer(ident).erDNummer &&
                                        systemOnlyPdlRestClient
                                            .hentGeografiskTilknytning(ident)
                                            .erSvalbard()
                                }

                                if (harBostedsadresseIFinnmarkEllerNordTroms ||
                                    harDeltBostedIFinnmarkEllerNordTroms ||
                                    harOppholdsadressePåSvalbard ||
                                    harDNummerOgGeografiskTilknytningTilSvalbard
                                ) {
                                    val aktør = personidentService.hentAktør(ident)
                                    val fagsakIder =
                                        fagsakService
                                            .finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(aktør)
                                            .map { it.id }

                                    if (fagsakIder.isNotEmpty()) {
                                        ident to
                                            BorIFinnmarkNordTromsEllerPåSvalbard(
                                                harBostedsadresseIFinnmarkNordTroms = harBostedsadresseIFinnmarkEllerNordTroms,
                                                harDeltBostedIFinnmarkNordTroms = harDeltBostedIFinnmarkEllerNordTroms,
                                                harOppholdsadressePåSvalbard = harOppholdsadressePåSvalbard,
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

        val personerMedBostedsadresseIFinnmarkNordTroms = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.harBostedsadresseIFinnmarkNordTroms }
        val personerMedDeltBostedIFinnmarkNordTroms = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.harDeltBostedIFinnmarkNordTroms }
        val overlappFinnmarkNordTroms = personerMedBostedsadresseIFinnmarkNordTroms.keys.intersect(personerMedDeltBostedIFinnmarkNordTroms.keys)

        val personerMedOppholdsadressePåSvalbard = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.harOppholdsadressePåSvalbard }
        val personerSomHarDNummerOgGeografiskTilknytningTilSvalbard = personerSomBorIFinnmarkNordTromsEllerPåSvalbard.filterValues { it.harDNummerOgGeografiskTilknytningTilSvalbard }
        val overlappSvalbard = personerMedOppholdsadressePåSvalbard.keys.intersect(personerSomHarDNummerOgGeografiskTilknytningTilSvalbard.keys)

        val fagsakerMedPersonerSomBorIFinnmarkNordTroms =
            (personerMedBostedsadresseIFinnmarkNordTroms.values + personerMedDeltBostedIFinnmarkNordTroms.values).flatMap { it.fagsakIder }.distinct()
        val fagsakerMedPersonerSomBorPåSvalbard =
            (personerMedOppholdsadressePåSvalbard.values + personerSomHarDNummerOgGeografiskTilknytningTilSvalbard.values).flatMap { it.fagsakIder }.distinct()

        task.metadata["bostedsadresseFinnmarkNordTroms"] = personerMedBostedsadresseIFinnmarkNordTroms.size
        task.metadata["deltBostedFinnmarkNordTroms"] = personerMedDeltBostedIFinnmarkNordTroms.size
        task.metadata["overlappFinnmarkNordTroms"] = overlappFinnmarkNordTroms.size

        task.metadata["oppholdsadresseSvalbard"] = personerMedOppholdsadressePåSvalbard.size
        task.metadata["dNummerGeografiskTilknytningSvalbard"] = personerSomHarDNummerOgGeografiskTilknytningTilSvalbard.size
        task.metadata["overlappSvalbard"] = overlappSvalbard.size

        task.metadata["fagsakerFinnmark"] = fagsakerMedPersonerSomBorIFinnmarkNordTroms.size
        task.metadata["fagsakerSvalbard"] = fagsakerMedPersonerSomBorPåSvalbard.size

        task.metadata["kjøretid"] = "${tid.inWholeMilliseconds} ms"
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
        val harBostedsadresseIFinnmarkNordTroms: Boolean,
        val harDeltBostedIFinnmarkNordTroms: Boolean,
        val harOppholdsadressePåSvalbard: Boolean,
        val harDNummerOgGeografiskTilknytningTilSvalbard: Boolean,
        val fagsakIder: List<Long>,
    )
}
