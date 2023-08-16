package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.kjerne.simulering.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilForrigeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class KontrollerNyUtbetalingsgeneratorService(
    private val featureToggleService: FeatureToggleService,
    private val økonomiService: ØkonomiService,
    private val økonomiKlient: ØkonomiKlient,
) {

    private val logger = LoggerFactory.getLogger(KontrollerNyUtbetalingsgeneratorService::class.java)

    fun kontrollerNyUtbetalingsgenerator(vedtak: Vedtak, utbetalingsoppdrag: Utbetalingsoppdrag) {
        if (!featureToggleService.isEnabled("familie.ba.sak.kontroller-ny-utbetalingsgenerator")) return

        val simuleringResultatGammel = økonomiKlient.hentSimulering(utbetalingsoppdrag)

        kontrollerNyUtbetalingsgenerator(
            vedtak = vedtak,
            simuleringResultatGammel = simuleringResultatGammel,
            utbetalingsoppdragGammel = utbetalingsoppdrag,
        )
    }

    fun kontrollerNyUtbetalingsgenerator(
        vedtak: Vedtak,
        simuleringResultatGammel: DetaljertSimuleringResultat,
        utbetalingsoppdragGammel: Utbetalingsoppdrag,
        erSimulering: Boolean = false,
    ): List<DiffFeilType> {
        if (!featureToggleService.isEnabled("familie.ba.sak.kontroller-ny-utbetalingsgenerator")) return emptyList()

        val diffFeilTyper = mutableListOf<DiffFeilType>()

        val behandling = vedtak.behandling

        val beregnetUtbetalingsoppdrag = økonomiService.genererUtbetalingsoppdrag(
            vedtak = vedtak,
            saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
            erSimulering = erSimulering,
        )

        logger.info("Behandling ${behandling.id} har følgende oppdaterte andeler: ${beregnetUtbetalingsoppdrag.andeler}")

        logger.info("Behandling ${behandling.id} får følgende utbetalingsoppdrag med gammel generator: $utbetalingsoppdragGammel")
        logger.info("Behandling ${behandling.id} får følgende utbetalingsoppdrag med ny generator: ${beregnetUtbetalingsoppdrag.utbetalingsoppdrag}")

        val simuleringResultatNy =
            økonomiKlient.hentSimulering(beregnetUtbetalingsoppdrag.utbetalingsoppdrag)

        val simuleringsPerioderGammel = simuleringResultatGammel.tilSorterteSimuleringsPerioder(behandling)

        val simuleringsPerioderNy = simuleringResultatNy.tilSorterteSimuleringsPerioder(behandling)

        val simuleringsPerioderGammelTidslinje: Tidslinje<SimuleringsPeriode, Måned> =
            simuleringsPerioderGammel.tilTidslinje()

        val simuleringsPerioderNyTidslinje: Tidslinje<SimuleringsPeriode, Måned> =
            simuleringsPerioderNy.tilTidslinje()

        validerAtSimuleringsPerioderGammelHarResultatLik0ForPerioderFørSimuleringsPerioderNy(
            simuleringsPerioderGammelTidslinje,
            simuleringsPerioderNyTidslinje,
            behandling,
        )?.let {
            diffFeilTyper.add(it)
        }
        validerAtSimuleringsPerioderGammelHarResultatLikSimuleringsPerioderNyEtterFomTilNy(
            simuleringsPerioderGammelTidslinje,
            simuleringsPerioderNyTidslinje,
            behandling,
        )?.let {
            diffFeilTyper.add(it)
        }

        if (diffFeilTyper.isNotEmpty()) {
            loggSimuleringsPerioderMedDiff(simuleringsPerioderGammel, simuleringsPerioderNy)
        }

        return diffFeilTyper
    }

    private fun validerAtSimuleringsPerioderGammelHarResultatLikSimuleringsPerioderNyEtterFomTilNy(
        simuleringsPerioderGammelTidslinje: Tidslinje<SimuleringsPeriode, Måned>,
        simuleringsPerioderNyTidslinje: Tidslinje<SimuleringsPeriode, Måned>,
        behandling: Behandling,
    ): DiffFeilType? {
        // Tidslinje over simuleringsperioder fra gammel simulering som starter samtidig som simulering fra ny generator
        val simuleringsPerioderTidslinjeGammelFraNy =
            simuleringsPerioderGammelTidslinje.beskjær(
                simuleringsPerioderNyTidslinje.fraOgMed()!!,
                simuleringsPerioderGammelTidslinje.tilOgMed()!!,
            )

        // Tidslinjene skal ha samme resultat for alle perioder
        val månederMedUliktResultat =
            simuleringsPerioderTidslinjeGammelFraNy
                .kombinerMed(simuleringsPerioderNyTidslinje) { gammel, ny ->
                    KombinertSimuleringsResultat(
                        erLike = gammel?.resultat == ny?.resultat,
                        gammel = gammel,
                        ny = ny,
                    )
                }
                .perioder()
                .filter { !it.innhold!!.erLike }

        if (månederMedUliktResultat.isNotEmpty()) {
            logger.warn("Behandling ${behandling.id}  har diff i simuleringsresultat ved bruk av ny utbetalingsgenerator - følgende måneder har ulikt resultat: [${månederMedUliktResultat.joinToString { "${it.fraOgMed} - ${it.tilOgMed}: Gammel ${it.innhold!!.gammel?.resultat} vs Ny ${it.innhold.ny?.resultat}" }}]")
            return DiffFeilType.UliktResultatISammePeriode
        }
        return null
    }

    // Tidslinje over simuleringsperioder som kommer før simuleringsperiodene til simulering fra ny generator.
    // Fordi vi opphører mer bakover i tiden med den gamle generatoren vil vi kunne få flere simuleringsperioder som kommer før simuleringsperiodene vi får fra ny generator.
    // Disse periodene skal ha et resultat som er lik 0, ellers er det noe feil.
    private fun validerAtSimuleringsPerioderGammelHarResultatLik0ForPerioderFørSimuleringsPerioderNy(
        simuleringsPerioderGammelTidslinje: Tidslinje<SimuleringsPeriode, Måned>,
        simuleringsPerioderNyTidslinje: Tidslinje<SimuleringsPeriode, Måned>,
        behandling: Behandling,
    ): DiffFeilType? {
        val perioderFraGammelFørNyMedResultatUlik0 = simuleringsPerioderGammelTidslinje
            .beskjær(
                simuleringsPerioderGammelTidslinje.fraOgMed()!!,
                simuleringsPerioderNyTidslinje.fraOgMed()!!.tilForrigeMåned(),
            ).perioder()
            .filter { it.innhold!!.resultat != BigDecimal.ZERO }

        if (perioderFraGammelFørNyMedResultatUlik0.isNotEmpty()) {
            logger.warn("Behandling ${behandling.id}  har diff i simuleringsresultat ved bruk av ny utbetalingsgenerator - simuleringsperioder før simuleringsperioder fra ny generator gir resultat ulik 0. [${perioderFraGammelFørNyMedResultatUlik0.joinToString() { it.toString() }}]")
            return DiffFeilType.TidligerePerioderIGammelUlik0
        }
        return null
    }

    private fun loggSimuleringsPerioderMedDiff(
        simuleringsPerioderGammel: List<SimuleringsPeriode>,
        simuleringsPerioderNy: List<SimuleringsPeriode>,
    ) {
        logger.warn("Simuleringsperioder med diff - Gammel: [${simuleringsPerioderGammel.joinToString() { "${it.fom} - ${it.tom}: ${it.resultat}" }}] Ny: [${simuleringsPerioderNy.joinToString() { "${it.fom} - ${it.tom}: ${it.resultat}" }}]")
    }

    private fun DetaljertSimuleringResultat.tilSorterteSimuleringsPerioder(behandling: Behandling): List<SimuleringsPeriode> =
        vedtakSimuleringMottakereTilSimuleringPerioder(
            this.simuleringMottaker.map {
                it.tilBehandlingSimuleringMottaker(behandling)
            },
            true,
        ).sortedBy { it.fom }

    data class KombinertSimuleringsResultat(
        val erLike: Boolean,
        val gammel: SimuleringsPeriode?,
        val ny: SimuleringsPeriode?,
    )
}

enum class DiffFeilType {
    TidligerePerioderIGammelUlik0,
    UliktResultatISammePeriode,
}
