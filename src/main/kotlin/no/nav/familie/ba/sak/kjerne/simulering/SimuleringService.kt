package no.nav.familie.ba.sak.kjerne.simulering

import io.micrometer.core.instrument.Metrics
import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.config.BehandlerRolle
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragGenerator
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.tilUtbetalingsoppdragDto
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.simulering.domene.Simulering
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottakerRepository
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class SimuleringService(
    private val økonomiKlient: ØkonomiKlient,
    private val beregningService: BeregningService,
    private val økonomiSimuleringMottakerRepository: ØkonomiSimuleringMottakerRepository,
    private val tilgangService: TilgangService,
    private val vedtakRepository: VedtakRepository,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    private val simulert = Metrics.counter("familie.ba.sak.oppdrag.simulert")

    fun hentSimuleringFraFamilieOppdrag(vedtak: Vedtak): DetaljertSimuleringResultat? {
        if (!beregningService.erEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(vedtak.behandling)) {
            return null
        }

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = vedtak.behandling.id)

        /**
         * SOAP integrasjonen støtter ikke full epost som MQ,
         * så vi bruker bare første 8 tegn av saksbehandlers epost for simulering.
         * Denne verdien brukes ikke til noe i simulering.
         */
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8)

        val utbetalingsoppdrag: Utbetalingsoppdrag =
            utbetalingsoppdragGenerator
                .lagUtbetalingsoppdrag(
                    saksbehandlerId = saksbehandlerId,
                    vedtak = vedtak,
                    tilkjentYtelse = tilkjentYtelse,
                    erSimulering = true,
                ).utbetalingsoppdrag
                .tilUtbetalingsoppdragDto()

        // Simulerer ikke mot økonomi når det ikke finnes utbetalingsperioder
        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) return null

        val detaljertSimuleringResultat = økonomiKlient.hentSimulering(utbetalingsoppdrag)

        simulert.increment()
        return detaljertSimuleringResultat
    }

    @Transactional
    fun lagreSimuleringPåBehandling(
        simuleringMottakere: List<SimuleringMottaker>,
        behandling: Behandling,
    ): List<ØkonomiSimuleringMottaker> {
        val vedtakSimuleringMottakere = simuleringMottakere.map { it.tilBehandlingSimuleringMottaker(behandling) }
        return økonomiSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
    }

    @Transactional
    fun slettSimuleringPåBehandling(behandlingId: Long) = økonomiSimuleringMottakerRepository.deleteByBehandlingId(behandlingId)

    fun hentSimuleringPåBehandling(behandlingId: Long): List<ØkonomiSimuleringMottaker> = økonomiSimuleringMottakerRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun oppdaterSimuleringPåBehandlingVedBehov(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        val behandlingErFerdigBesluttet =
            behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ||
                behandling.status == BehandlingStatus.AVSLUTTET

        val simulering = hentSimuleringPåBehandling(behandlingId)
        val restSimulering =
            vedtakSimuleringMottakereTilSimuleringDto(
                økonomiSimuleringMottakere = simulering,
            )

        return if (!behandlingErFerdigBesluttet && simuleringErUtdatert(restSimulering)) {
            oppdaterSimuleringPåBehandling(behandling)
        } else {
            simulering
        }
    }

    fun simuleringErUtdatert(simulering: Simulering) =
        simulering.tidSimuleringHentet == null ||
            (
                simulering.forfallsdatoNestePeriode != null &&
                    simulering.tidSimuleringHentet < simulering.forfallsdatoNestePeriode &&
                    simulering.forfallsdatoNestePeriode < LocalDate.now()
            )

    @Transactional
    fun oppdaterSimuleringPåBehandling(behandling: Behandling): List<ØkonomiSimuleringMottaker> {
        val aktivtVedtak =
            vedtakRepository.findByBehandlingAndAktivOptional(behandling.id)
                ?: throw Feil("Fant ikke aktivt vedtak på behandling${behandling.id}")
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette simulering",
        )

        val simulering: List<SimuleringMottaker> =
            hentSimuleringFraFamilieOppdrag(vedtak = aktivtVedtak)?.simuleringMottaker ?: emptyList()

        slettSimuleringPåBehandling(behandling.id)
        return lagreSimuleringPåBehandling(simulering, behandling)
    }

    fun hentEtterbetaling(behandlingId: Long): BigDecimal {
        val vedtakSimuleringMottakere = hentSimuleringPåBehandling(behandlingId)
        return hentEtterbetaling(vedtakSimuleringMottakere)
    }

    fun hentFeilutbetaling(behandlingId: Long): BigDecimal {
        val vedtakSimuleringMottakere = hentSimuleringPåBehandling(behandlingId)
        return hentFeilutbetaling(vedtakSimuleringMottakere)
    }

    fun hentEtterbetaling(økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>): BigDecimal =
        vedtakSimuleringMottakereTilSimuleringDto(
            økonomiSimuleringMottakere = økonomiSimuleringMottakere,
        ).etterbetaling

    fun hentFeilutbetaling(økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>): BigDecimal =
        vedtakSimuleringMottakereTilSimuleringDto(
            økonomiSimuleringMottakere,
        ).feilutbetaling

    fun harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling: Behandling): Boolean {
        if (!behandling.erManuellMigrering()) throw Feil("Avvik innenfor beløpsgrenser skal bare sjekkes for manuelle migreringsbehandlinger")

        val antallBarn = persongrunnlagService.hentSøkerOgBarnPåBehandling(behandling.id)?.barn()?.size ?: 0

        return sjekkOmBehandlingHarEtterbetalingInnenforBeløpsgrenser(behandling, antallBarn) &&
            sjekkOmBehandlingHarFeilutbetalingInnenforBeløpsgrenser(behandling, antallBarn)
    }

    fun harMigreringsbehandlingManuellePosteringer(behandling: Behandling): Boolean {
        if (!behandling.erManuellMigrering()) throw Feil("Sjekk for manuelle posteringer skal bare gjøres for manuelle migreringsbehandlinger. Fagsak: ${behandling.fagsak.id} Behandling: ${behandling.id}")

        return filterBortUrelevanteVedtakSimuleringPosteringer(hentSimuleringPåBehandling(behandling.id))
            .flatMap { it.økonomiSimuleringPostering }
            .any { it.erManuellPostering }
    }

    private fun sjekkOmBehandlingHarEtterbetalingInnenforBeløpsgrenser(
        behandling: Behandling,
        antallBarn: Int,
    ): Boolean {
        val finnesEtterBetaling = hentTotalEtterbetalingFørMars2023(behandling.id) != BigDecimal.ZERO
        if (!finnesEtterBetaling) return true

        val simuleringsperioderFørMars2023 = hentSimuleringsperioderFørMars2023(behandling.id)
        if (
            simuleringsperioderFørMars2023.harKunPositiveResultater() &&
            simuleringsperioderFørMars2023.harMaks1KroneIResultatPerBarn(antallBarn) &&
            simuleringsperioderFørMars2023.harTotaltAvvikUnderBeløpsgrense()
        ) {
            return true
        }

        return false
    }

    private fun sjekkOmBehandlingHarFeilutbetalingInnenforBeløpsgrenser(
        behandling: Behandling,
        antallBarn: Int,
    ): Boolean {
        val finnesFeilutbetaling = hentFeilutbetaling(behandling.id) != BigDecimal.ZERO
        if (!finnesFeilutbetaling) return true

        val simuleringsperioderFørMars2023 = hentSimuleringsperioderFørMars2023(behandling.id)
        if (
            simuleringsperioderFørMars2023.harKunNegativeResultater() &&
            simuleringsperioderFørMars2023.harMaks1KroneIResultatPerBarn(antallBarn) &&
            simuleringsperioderFørMars2023.harTotaltAvvikUnderBeløpsgrense()
        ) {
            return true
        }

        return false
    }

    private fun hentSimuleringsperioderFørMars2023(behandlingId: Long): List<SimuleringsPeriode> {
        val februar2023 = LocalDate.of(2023, 2, 1)

        return vedtakSimuleringMottakereTilSimuleringPerioder(
            økonomiSimuleringMottakere = hentSimuleringPåBehandling(behandlingId),
        ).filter {
            it.fom.isSameOrBefore(februar2023)
        }
    }

    private fun hentTotalEtterbetalingFørMars2023(behandlingId: Long) = hentTotalEtterbetaling(hentSimuleringsperioderFørMars2023(behandlingId), null)

    private fun List<SimuleringsPeriode>.harKunPositiveResultater() = all { it.resultat >= BigDecimal.ZERO }

    private fun List<SimuleringsPeriode>.harKunNegativeResultater() = all { it.resultat <= BigDecimal.ZERO }

    private fun List<SimuleringsPeriode>.harMaks1KroneIResultatPerBarn(antallBarn: Int) =
        all {
            it.resultat.abs() <= BigDecimal(antallBarn)
        }

    private fun List<SimuleringsPeriode>.harTotaltAvvikUnderBeløpsgrense() = sumOf { it.resultat }.abs() < BigDecimal(MANUELL_MIGRERING_BELØPSGRENSE_FOR_TOTALT_AVVIK)

    companion object {
        const val MANUELL_MIGRERING_BELØPSGRENSE_FOR_TOTALT_AVVIK = 100
        val logger = LoggerFactory.getLogger(SimuleringService::class.java)
    }
}
