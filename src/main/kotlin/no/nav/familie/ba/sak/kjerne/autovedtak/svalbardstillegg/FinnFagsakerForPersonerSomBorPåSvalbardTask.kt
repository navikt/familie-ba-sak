package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.erSvalbard
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.oppholdsadresseErPåSvalbardPåDato
import no.nav.familie.ba.sak.integrasjoner.pdl.logger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.time.measureTimedValue

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnFagsakerForPersonerSomBorPåSvalbardTask.TASK_STEP_TYPE,
    beskrivelse = "Finn fagsaker for personer som bor på Svalbard",
    maxAntallFeil = 1,
)
class FinnFagsakerForPersonerSomBorPåSvalbardTask(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val systemOnlyPdlRestClient: SystemOnlyPdlRestClient,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val fagsakIder = task.payload.split(",").map { it.toLong() }

        val (fagsakerMedPersonerSomBorPåSvalbard, tid) =
            measureTimedValue {
                val fagsakMedSøkerOgBarna =
                    fagsakIder
                        .mapNotNull { fagsakId ->
                            val sisteIverksatteBehandlingId =
                                behandlingHentOgPersisterService
                                    .hentIdForSisteBehandlingSomErIverksatt(fagsakId)
                                    ?: return@mapNotNull null

                            val søkerIdent = fagsakService.hentAktør(fagsakId).aktivFødselsnummer()
                            val barnMedLøpendeOrdinær =
                                andelTilkjentYtelseRepository
                                    .finnIdentForAktørerMedLøpendeAndelerTilkjentYtelseForBehandlingAvType(
                                        behandlingId = sisteIverksatteBehandlingId,
                                        type = YtelseType.ORDINÆR_BARNETRYGD,
                                    )

                            fagsakId to
                                SøkerOgBarnIdenter(
                                    søkerIdent = søkerIdent,
                                    barnIdenter = barnMedLøpendeOrdinær,
                                )
                        }.toMap()

                val personerSomBorPåSvalbard =
                    fagsakMedSøkerOgBarna.values
                        .flatMap { it.barnIdenter + it.søkerIdent }
                        .distinct()
                        .chunked(1000)
                        .flatMap { identer ->
                            systemOnlyPdlRestClient
                                .hentBostedsadresseDeltBostedOgOppholdsadresseForPersoner(identer)
                                .filter { (ident, adresser) ->
                                    val harOppholdsadressePåSvalbard =
                                        adresser
                                            .oppholdsadresse
                                            .oppholdsadresseErPåSvalbardPåDato(LocalDate.now())

                                    val harDNummerOgGeografiskTilknytningTilSvalbard =
                                        Fødselsnummer(ident).erDNummer &&
                                            systemOnlyPdlRestClient
                                                .hentGeografiskTilknytning(ident)
                                                .erSvalbard()

                                    harOppholdsadressePåSvalbard || harDNummerOgGeografiskTilknytningTilSvalbard
                                }.keys
                        }.toSet()

                fagsakMedSøkerOgBarna
                    .mapNotNull { (fagsakId, søkerOgBarna) ->
                        val søkerBorPåSvalbard = søkerOgBarna.søkerIdent in personerSomBorPåSvalbard
                        val minstEttBarnBorPåSvalbard = søkerOgBarna.barnIdenter.any { it in personerSomBorPåSvalbard }

                        val minstÉnPersonBorPåSvalbard = søkerBorPåSvalbard || minstEttBarnBorPåSvalbard
                        val søkerOgMinstEttBarnBorPåSvalbard = søkerBorPåSvalbard && minstEttBarnBorPåSvalbard

                        if (minstÉnPersonBorPåSvalbard) {
                            fagsakId to søkerOgMinstEttBarnBorPåSvalbard
                        } else {
                            null
                        }
                    }.toMap()
            }

        val fagsakerDerMinstÉnPersonBorPåSvalbard = fagsakerMedPersonerSomBorPåSvalbard.keys.joinToString(",") { it.toString() }
        val fagsakerDerSøkerOgMinstEttBarnBorPåSvalbard = fagsakerMedPersonerSomBorPåSvalbard.filterValues { it }.keys.joinToString(",") { it.toString() }

        logger.info("Fagsaker der minst én person bor på Svalbard: $fagsakerDerMinstÉnPersonBorPåSvalbard")
        logger.info("Fagsaker der søker og minst ett barn bor på Svalbard: $fagsakerDerSøkerOgMinstEttBarnBorPåSvalbard")

        task.metadata["fagsakerDerMinstÉnPersonBorPåSvalbard"] = fagsakerDerMinstÉnPersonBorPåSvalbard
        task.metadata["fagsakerDerSøkerOgMinstEttBarnBorPåSvalbard"] = fagsakerDerSøkerOgMinstEttBarnBorPåSvalbard
        task.metadata["Kjøretid"] = "${tid.inWholeSeconds} sekunder"
    }

    companion object {
        const val TASK_STEP_TYPE = "finnFagsakerForPersonerSomBorPåSvalbardTask"

        fun opprettTask(
            identer: List<Long>,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = identer.joinToString(","),
            )
    }

    private data class SøkerOgBarnIdenter(
        val søkerIdent: String,
        val barnIdenter: List<String>,
    )
}
