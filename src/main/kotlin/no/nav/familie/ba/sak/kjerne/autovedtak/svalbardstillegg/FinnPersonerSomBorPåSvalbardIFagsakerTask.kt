package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.Feil
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
    taskStepType = FinnPersonerSomBorPåSvalbardIFagsakerTask.TASK_STEP_TYPE,
    beskrivelse = "Finn personer som bor på Svalbard i fagsaker",
    maxAntallFeil = 1,
)
class FinnPersonerSomBorPåSvalbardIFagsakerTask(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val systemOnlyPdlRestClient: SystemOnlyPdlRestClient,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val fagsakIder = task.payload.split(",").map { it.toLong() }

        if (fagsakIder.size > 250) {
            throw Feil("For mange fagsaker i tasken, maks 250 er tillatt, fikk ${fagsakIder.size}")
        }

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
                                        type = YtelseType.ORDINÆR_BARNETRYGD.name,
                                    )

                            fagsakId.toString() to
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
                        val søkerHarDNummer = Fødselsnummer(søkerOgBarna.søkerIdent).erDNummer
                        val antallBarnSomBorPåSvalbard = søkerOgBarna.barnIdenter.count { it in personerSomBorPåSvalbard }
                        val antallBarnSomHarDNummer = søkerOgBarna.barnIdenter.count { Fødselsnummer(it).erDNummer }

                        val personerSomBorPåSvalbard =
                            when {
                                søkerBorPåSvalbard && antallBarnSomBorPåSvalbard > 0 -> "Søker og $antallBarnSomBorPåSvalbard barn"
                                søkerBorPåSvalbard -> "Søker"
                                antallBarnSomBorPåSvalbard > 0 -> "$antallBarnSomBorPåSvalbard barn"
                                else -> "Ingen"
                            }

                        val personerSomHarDNummer =
                            when {
                                søkerHarDNummer && antallBarnSomHarDNummer > 0 -> "Søker og $antallBarnSomHarDNummer barn"
                                søkerHarDNummer -> "Søker"
                                antallBarnSomHarDNummer > 0 -> "$antallBarnSomHarDNummer barn"
                                else -> "Ingen"
                            }

                        fagsakId to "$personerSomBorPåSvalbard,$personerSomHarDNummer"
                    }.toMap()
            }

        fagsakerMedPersonerSomBorPåSvalbard.forEach { (fagsakId, personer) ->
            logger.info("Fagsak $fagsakId har følgende personer som bor på Svalbard: $personer")
            task.metadata[fagsakId] = personer
        }

        task.metadata["Kjøretid"] = "${tid.inWholeMilliseconds} ms"
    }

    companion object {
        const val TASK_STEP_TYPE = "finnPersonerSomBorPåSvalbardIFagsakerTask"

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
