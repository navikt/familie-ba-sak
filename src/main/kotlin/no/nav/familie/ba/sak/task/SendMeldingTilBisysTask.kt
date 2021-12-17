package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendMeldingTilBisysTask.TASK_STEP_TYPE,
    beskrivelse = "Iverksett vedtak mot oppdrag",
    maxAntallFeil = 3
)
class SendMeldingTilBisysTask(
    private val kafkaProducer: KafkaProducer,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val behandlingRepository: BehandlingRepository,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(SendMeldingTilBisysTask::class.java)

    override fun doTask(task: Task) {
        val behandling = behandlingRepository.finnBehandling(task.payload.toLong())

        // Bisys vil kun ha rene manuelle opphør
        if (behandling.resultat == BehandlingResultat.OPPHØRT || behandling.resultat == BehandlingResultat.ENDRET ||
            behandling.resultat == BehandlingResultat.ENDRET_OG_OPPHØRT
        ) {
            val endretPerioder = finnEndretPerioder(behandling)
            val barnetrygdBisysMelding = BarnetrygdBisysMelding(
                søker = behandling.fagsak.aktør.aktivFødselsnummer(),
                barn = endretPerioder.map {
                    val førstPeriod = it.value[0]
                    BarnEndretOpplysning(
                        ident = it.key,
                        årsakskode = førstPeriod.type,
                        fom = førstPeriod.fom,
                        tom = førstPeriod.tom
                    )
                }
            )

            kafkaProducer.sendBarnetrygdBisysMelding(
                behandling.id.toString(),
                barnetrygdBisysMelding
            )
        } else {
            logger.info("Sender ikke melding til bisys siden resultat ikke er opphørt.")
        }
    }

    fun finnEndretPerioder(behandling: Behandling): Map<String, List<EndretPeriode>> {
        val iverksatteBehandlinger =
            behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)
        val forrigeIverksatteBehandling = Behandlingutils.hentForrigeIverksatteBehandling(
            iverksatteBehandlinger = iverksatteBehandlinger,
            behandlingFørFølgende = behandling
        ) ?: error("Finnes ikke forrige behandling for behandling ${behandling.id}")

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val forrigeTilkjentYtelse = tilkjentYtelseRepository.findByBehandling(forrigeIverksatteBehandling.id)

        val endretPerioder: MutableMap<String, MutableList<EndretPeriode>> = mutableMapOf()

        forrigeTilkjentYtelse.andelerTilkjentYtelse.groupBy { it.personIdent }.entries.forEach { entry ->
            val nyAndelerTilkjentYtelse =
                tilkjentYtelse.andelerTilkjentYtelse.filter { it.personIdent == entry.key }
                    .sortedBy { it.stønadFom }
            entry.value.sortedBy { it.periode.fom }.forEach {
                var forblePeriode: MånedPeriode? = it.periode
                val prosent = it.prosent
                if (!endretPerioder.contains(it.personIdent)) {
                    endretPerioder[it.personIdent] = mutableListOf()
                }
                nyAndelerTilkjentYtelse.forEach {
                    val intersectPerioder = forblePeriode!!.intersect(it.periode)
                    if (intersectPerioder.first != null) {
                        endretPerioder[it.personIdent]!!.add(
                            EndretPeriode(
                                fom = forblePeriode!!.fom,
                                tom = it.periode.fom.minusMonths(1),
                                type = EndretType.RO
                            )
                        )
                    }
                    if (intersectPerioder.second != null && it.prosent < prosent) {
                        endretPerioder[it.personIdent]!!.add(
                            EndretPeriode(
                                fom = latest(it.periode.fom, forblePeriode!!.fom),
                                tom = earlist(it.periode.tom, forblePeriode!!.tom),
                                type = EndretType.RR
                            )
                        )
                    }
                    forblePeriode = intersectPerioder.third
                    if (forblePeriode == null) {
                        return@forEach
                    }
                }
                if (forblePeriode != null && !forblePeriode!!.erTom()) {
                    endretPerioder[it.personIdent]!!.add(
                        EndretPeriode(
                            fom = forblePeriode!!.fom,
                            tom = forblePeriode!!.tom,
                            type = EndretType.RO
                        )
                    )
                }
            }
        }
        return endretPerioder
    }

    companion object {
        const val TASK_STEP_TYPE = "sendMeldingOmOpphørTilBisys"

        fun opprettTask(behandlingsId: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = behandlingsId.toString(),
                properties = Properties().apply {
                    this["behandlingsId"] = behandlingsId.toString()
                }
            )
        }
    }
}

enum class EndretType {
    RO, // Revurdering og Opphør
    RR, // Revurdering og Reduksjon
    ØKT,
}

data class EndretPeriode(
    val fom: YearMonth,
    val tom: YearMonth,
    val type: EndretType,
)

inline fun earlist(yearMonth1: YearMonth, yearMonth2: YearMonth): YearMonth {
    return if (yearMonth1.isSameOrBefore(yearMonth2)) yearMonth1 else yearMonth2
}

inline fun latest(yearMonth1: YearMonth, yearMonth2: YearMonth): YearMonth {
    return if (yearMonth1.isSameOrAfter(yearMonth2)) yearMonth1 else yearMonth2
}

inline fun MånedPeriode.intersect(periode: MånedPeriode): Triple<MånedPeriode?, MånedPeriode?, MånedPeriode?> {
    val overlappetFom = latest(this.fom, periode.fom)
    val overlappetTom = earlist(this.tom, periode.tom)
    return Triple(
        if (this.fom.isSameOrAfter(periode.fom)) null else MånedPeriode(this.fom, periode.fom.minusMonths(1)),
        if (overlappetTom.isBefore(overlappetFom)) null else MånedPeriode(overlappetFom, overlappetTom),
        if (this.tom.isSameOrBefore(periode.tom)) null else MånedPeriode(periode.tom.plusMonths(1), this.tom)
    )
}

inline fun MånedPeriode.erTom() = fom.isAfter(tom)

data class BarnEndretOpplysning(
    val ident: String,
    val årsakskode: EndretType,
    val fom: YearMonth,
    val tom: YearMonth
)

data class BarnetrygdBisysMelding(
    val søker: String,
    val barn: List<BarnEndretOpplysning>
)
