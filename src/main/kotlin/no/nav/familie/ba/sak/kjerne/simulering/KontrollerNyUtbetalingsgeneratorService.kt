package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.kjerne.simulering.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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

        kontrollerNyUtbetalingsgenerator(vedtak = vedtak, simuleringResultatGammel = simuleringResultatGammel)
    }

    fun kontrollerNyUtbetalingsgenerator(
        vedtak: Vedtak,
        simuleringResultatGammel: DetaljertSimuleringResultat,
        erSimulering: Boolean = false,
    ) {
        if (!featureToggleService.isEnabled("familie.ba.sak.kontroller-ny-utbetalingsgenerator")) return

        val behandling = vedtak.behandling

        val beregnetUtbetalingsoppdrag = økonomiService.genererUtbetalingsoppdrag(
            vedtak = vedtak,
            saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
            erSimulering = erSimulering,
        )
        val simuleringResultatNy =
            økonomiKlient.hentSimulering(beregnetUtbetalingsoppdrag.utbetalingsoppdrag)

        val simuleringsPerioderGammel = simuleringResultatGammel.tilSorterteSimuleringsPerioder(behandling)

        val simuleringsPerioderNy = simuleringResultatNy.tilSorterteSimuleringsPerioder(behandling)

        if (simuleringsPerioderGammel.size != simuleringsPerioderNy.size) {
            logger.warn("Behandling ${behandling.id} har diff i simuleringsresultat ved bruk av ny utbetalingsgenerator - ulikt antall simuleringsperioder. Gammel gir ${simuleringsPerioderGammel.size} perioder, mens ny gir ${simuleringsPerioderNy.size}")
            loggSimuleringsPerioderMedDiff(
                simuleringsPerioderGammel = simuleringsPerioderGammel,
                simuleringsPerioderNy = simuleringsPerioderNy,
            )
            return
        }

        val simuleringsPerioderTidslinjeGammel = simuleringsPerioderGammel.tilTidslinje()
        val simuleringsPerioderTidslinjeNy = simuleringsPerioderNy.tilTidslinje()

        val månederMedUliktResultat =
            simuleringsPerioderTidslinjeGammel
                .kombinerMed(simuleringsPerioderTidslinjeNy) { gammel, ny ->
                    KombinertSimuleringsResultat(
                        erUlike = gammel?.resultat == ny?.resultat,
                        gammel = gammel,
                        ny = ny,
                    )
                }
                .perioder()
                .filter { !it.innhold!!.erUlike }

        if (månederMedUliktResultat.isNotEmpty()) {
            logger.warn("Behandling ${behandling.id}  har diff i simuleringsresultat ved bruk av ny utbetalingsgenerator - følgende måneder har ulikt resultat: [${månederMedUliktResultat.joinToString { "${it.fraOgMed} - ${it.tilOgMed}: Gammel ${it.innhold!!.gammel?.resultat} vs Ny ${it.innhold.ny?.resultat}" }}]")
            loggSimuleringsPerioderMedDiff(
                simuleringsPerioderGammel = simuleringsPerioderGammel,
                simuleringsPerioderNy = simuleringsPerioderNy,
            )
        }
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
        val erUlike: Boolean,
        val gammel: SimuleringsPeriode?,
        val ny: SimuleringsPeriode?,
    )
}
