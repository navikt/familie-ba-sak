package no.nav.familie.ba.sak.kjerne.simulering

import io.micrometer.core.instrument.Metrics
import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsperiodeMal
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.domene.RestSimulering
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottakerRepository
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class SimuleringService(
    private val økonomiKlient: ØkonomiKlient,
    private val økonomiService: ØkonomiService,
    private val beregningService: BeregningService,
    private val økonomiSimuleringMottakerRepository: ØkonomiSimuleringMottakerRepository,
    private val tilgangService: TilgangService,
    private val featureToggleService: FeatureToggleService,
    private val vedtakRepository: VedtakRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
) {
    private val simulert = Metrics.counter("familie.ba.sak.oppdrag.simulert")

    fun hentSimuleringFraFamilieOppdrag(vedtak: Vedtak): DetaljertSimuleringResultat? {
        val forrigeIverksatteBehandling =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(vedtak.behandling)
        if (forrigeIverksatteBehandling !== null && !beregningService.erEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(
                vedtak.behandling,
            )
        ) {
            return null
        }

        /**
         * SOAP integrasjonen støtter ikke full epost som MQ,
         * så vi bruker bare første 8 tegn av saksbehandlers epost for simulering.
         * Denne verdien brukes ikke til noe i simulering.
         */

        var utbetalingsoppdrag = økonomiService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            vedtak = vedtak,
            saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
            andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForSimuleringFactory(),
            erSimulering = true,
        )

        /**
         * Dersom det ikke finnes utbetalingsperioder og behandlingen har andeler, betyr det at vi kun har andeler med kalkulert utbetalingsbeløp lik 0 kr og det ikke finnes noen tidligere behandling.
         * Dersom det er en manuell migreringsbehandling trenger vi allikevel å kjøre en simulering for å se om det er avvik mellom Infotrygd og BA-sak.
         */
        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(vedtak.behandling.id)
        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty() && andelerTilkjentYtelse.isNotEmpty() && vedtak.behandling.erManuellMigrering()) {
            utbetalingsoppdrag = Utbetalingsoppdrag(
                saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = FAGSYSTEM,
                saksnummer = vedtak.behandling.fagsak.id.toString(),
                aktoer = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
                utbetalingsperiode = AndelTilkjentYtelseForSimuleringFactory()
                    .pakkInnForUtbetaling(
                        andelerTilkjentYtelse.map { it.copy(kalkulertUtbetalingsbeløp = 1) },
                    )
                    .mapIndexed { index, andel ->
                        val forrigeIndex = if (index == 0) {
                            null
                        } else {
                            index - 1
                        }
                        UtbetalingsperiodeMal(vedtak, false)
                            .lagPeriodeFraAndel(
                                andel = andel,
                                periodeIdOffset = index,
                                forrigePeriodeIdOffset = forrigeIndex,
                                opphørKjedeFom = null,
                            )
                    },
            )
        }

        simulert.increment()
        return økonomiKlient.hentSimulering(utbetalingsoppdrag)
    }

    @Transactional
    fun lagreSimuleringPåBehandling(
        simuleringMottakere: List<SimuleringMottaker>,
        beahndling: Behandling,
    ): List<ØkonomiSimuleringMottaker> {
        val vedtakSimuleringMottakere = simuleringMottakere.map { it.tilBehandlingSimuleringMottaker(beahndling) }
        return økonomiSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
    }

    @Transactional
    fun slettSimuleringPåBehandling(behandlingId: Long) =
        økonomiSimuleringMottakerRepository.deleteByBehandlingId(behandlingId)

    fun hentSimuleringPåBehandling(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        return økonomiSimuleringMottakerRepository.findByBehandlingId(behandlingId)
    }

    fun oppdaterSimuleringPåBehandlingVedBehov(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = behandlingId)
        val behandlingErFerdigBesluttet =
            behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ||
                behandling.status == BehandlingStatus.AVSLUTTET

        val simulering = hentSimuleringPåBehandling(behandlingId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(
            økonomiSimuleringMottakere = simulering,
            erManuellPosteringTogglePå = featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ),
        )

        return if (!behandlingErFerdigBesluttet && simuleringErUtdatert(restSimulering)) {
            oppdaterSimuleringPåBehandling(behandling)
        } else {
            simulering
        }
    }

    private fun simuleringErUtdatert(simulering: RestSimulering) =
        simulering.tidSimuleringHentet == null ||
            (
                simulering.forfallsdatoNestePeriode != null &&
                    simulering.tidSimuleringHentet < simulering.forfallsdatoNestePeriode &&
                    LocalDate.now() > simulering.forfallsdatoNestePeriode
                )

    @Transactional
    fun oppdaterSimuleringPåBehandling(behandling: Behandling): List<ØkonomiSimuleringMottaker> {
        val aktivtVedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandling.id)
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

    fun hentEtterbetaling(økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>): BigDecimal {
        return vedtakSimuleringMottakereTilRestSimulering(
            økonomiSimuleringMottakere = økonomiSimuleringMottakere,
            erManuellPosteringTogglePå = featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ),
        ).etterbetaling
    }

    fun hentFeilutbetaling(økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>): BigDecimal {
        return vedtakSimuleringMottakereTilRestSimulering(
            økonomiSimuleringMottakere,
            featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ),
        ).feilutbetaling
    }

    fun harMigreringsbehandlingAvvikInnenforBeløpsgrenser(behandling: Behandling): Boolean {
        if (!behandling.erManuellMigrering()) throw Feil("Avvik innenfor beløpsgrenser skal bare sjekkes for manuelle migreringsbehandlinger")

        val antallBarn = persongrunnlagService.hentBarna(behandling.id).size

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
            erManuelPosteringTogglePå = featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ),
        ).filter {
            it.fom.isSameOrBefore(februar2023)
        }
    }

    private fun hentTotalEtterbetalingFørMars2023(behandlingId: Long) =
        hentTotalEtterbetaling(hentSimuleringsperioderFørMars2023(behandlingId), null)

    private fun List<SimuleringsPeriode>.harKunPositiveResultater() = all { it.resultat >= BigDecimal.ZERO }

    private fun List<SimuleringsPeriode>.harKunNegativeResultater() = all { it.resultat <= BigDecimal.ZERO }

    private fun List<SimuleringsPeriode>.harMaks1KroneIResultatPerBarn(antallBarn: Int) = all {
        it.resultat.abs() <= BigDecimal(antallBarn)
    }

    private fun List<SimuleringsPeriode>.harTotaltAvvikUnderBeløpsgrense() =
        sumOf { it.resultat }.abs() < BigDecimal(MANUELL_MIGRERING_BELØPSGRENSE_FOR_TOTALT_AVVIK)

    companion object {
        const val MANUELL_MIGRERING_BELØPSGRENSE_FOR_TOTALT_AVVIK = 100
    }
}
