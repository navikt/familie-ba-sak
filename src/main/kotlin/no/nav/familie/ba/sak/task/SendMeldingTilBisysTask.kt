package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.Metrics
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.eksterne.kontrakter.bisys.BarnEndretOpplysning
import no.nav.familie.eksterne.kontrakter.bisys.BarnetrygdBisysMelding
import no.nav.familie.eksterne.kontrakter.bisys.BarnetrygdEndretType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendMeldingTilBisysTask.TASK_STEP_TYPE,
    beskrivelse = "Send melding til Bisys om opphør eller reduksjon",
    maxAntallFeil = 3,
)
class SendMeldingTilBisysTask(
    private val kafkaProducer: KafkaProducer,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(SendMeldingTilBisysTask::class.java)
    private val meldingsTeller = Metrics.counter("familie.ba.sak.bisys.meldinger.sendt")

    @WithSpan
    override fun doTask(task: Task) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = task.payload.toLong())

        // Bisys vil kun ha rene manuelle opphør eller reduksjon
        if (behandling.resultat == Behandlingsresultat.OPPHØRT ||
            behandling.resultat == Behandlingsresultat.ENDRET_UTBETALING ||
            behandling.resultat == Behandlingsresultat.ENDRET_OG_OPPHØRT ||
            behandling.resultat == Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT ||
            behandling.resultat == Behandlingsresultat.INNVILGET_OG_OPPHØRT ||
            behandling.resultat == Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT ||
            behandling.resultat == Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT ||
            behandling.resultat == Behandlingsresultat.AVSLÅTT_OG_OPPHØRT ||
            behandling.resultat == Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT
        ) {
            val barnEndretOpplysning = finnBarnEndretOpplysning(behandling)
            val barnetrygdBisysMelding =
                BarnetrygdBisysMelding(
                    søker = behandling.fagsak.aktør.aktivFødselsnummer(),
                    barn = barnEndretOpplysning.map { it.value.minByOrNull { barnEndretOpplysning -> barnEndretOpplysning.fom }!! }, // Tar kun med den første endringen per aktør
                )

            if (barnetrygdBisysMelding.barn.isEmpty()) {
                logger.info("Behandling endret men ikke reduksjon eller opphør. Send ikke melding til bisys")
                return
            }

            logger.info("Sender melding til bisys om opphør eller reduksjon av barnetrygd.")

            kafkaProducer.sendBarnetrygdBisysMelding(
                behandling.id.toString(),
                barnetrygdBisysMelding,
            )
            meldingsTeller.increment()
        } else {
            logger.info("Sender ikke melding til bisys siden resultat ikke er opphør eller reduksjon.")
        }
    }

    fun finnBarnEndretOpplysning(behandling: Behandling): Map<String, List<BarnEndretOpplysning>> {
        val forrigeIverksatteBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling = behandling) ?: return emptyMap()

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val forrigeTilkjentYtelse = tilkjentYtelseRepository.findByBehandling(forrigeIverksatteBehandling.id)

        val andelTilkjentYtelseTidslinjerPerAktørOgType =
            tilkjentYtelse.andelerTilkjentYtelse
                .tilTidslinjerPerAktørOgType()

        val forrigeAndelTilkjentYtelseTidslinjerPerAktørOgType =
            forrigeTilkjentYtelse.andelerTilkjentYtelse
                .tilTidslinjerPerAktørOgType()

        val opphørEllerReduksjonPerAktør =
            forrigeAndelTilkjentYtelseTidslinjerPerAktørOgType
                .outerJoin(andelTilkjentYtelseTidslinjerPerAktørOgType) { forrigeAtyIPeriode, nyAtyIPeriode ->
                    when {
                        forrigeAtyIPeriode != null && nyAtyIPeriode == null ->
                            BarnetrygdEndretType.RO // Opphør
                        forrigeAtyIPeriode != null && nyAtyIPeriode != null && nyAtyIPeriode.prosent < forrigeAtyIPeriode.prosent ->
                            BarnetrygdEndretType.RR // Reduksjon
                        else -> null
                    }
                }.flatMap { (aktørOgType, tidslinje) ->
                    tidslinje.slåSammenLikePerioder().tilPerioderIkkeNull().map { endretPeriodeForAktør ->
                        BarnEndretOpplysning(
                            ident = aktørOgType.first.aktivFødselsnummer(),
                            fom = endretPeriodeForAktør.fom!!.toYearMonth(),
                            årsakskode = endretPeriodeForAktør.verdi,
                        )
                    }
                }.groupBy { it.ident }

        return opphørEllerReduksjonPerAktør
    }

    companion object {
        const val TASK_STEP_TYPE = "sendMeldingOmOpphørTilBisys"

        fun opprettTask(behandlingsId: Long): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = behandlingsId.toString(),
                properties =
                    Properties().apply {
                        this["behandlingId"] = behandlingsId.toString()
                    },
            )
    }
}
