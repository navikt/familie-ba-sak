package no.nav.familie.ba.sak.kjerne.simulering

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.simulering.domene.RestSimulering
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØknomiSimuleringMottakerRepository
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class SimuleringService(
    private val økonomiKlient: ØkonomiKlient,
    private val økonomiService: ØkonomiService,
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val beregningService: BeregningService,
    private val øknomiSimuleringMottakerRepository: ØknomiSimuleringMottakerRepository,
    private val tilgangService: TilgangService,
    private val featureToggleService: FeatureToggleService,
    private val vedtakRepository: VedtakRepository,
    private val behandlingRepository: BehandlingRepository
) {
    private val simulert = Metrics.counter("familie.ba.sak.oppdrag.simulert")

    fun hentSimuleringFraFamilieOppdrag(vedtak: Vedtak): DetaljertSimuleringResultat? {

        // TODO Fjern dette når vi har logget det vi skal
        if (vedtak.behandling.id == 2199950L) {
            val skalSimuleres = vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET ||
                vedtak.behandling.resultat == Behandlingsresultat.AVSLÅTT ||
                beregningService.innvilgetSøknadUtenUtbetalingsperioderGrunnetEndringsPerioder(behandling = vedtak.behandling)

            secureLogger.info("behandling skalSimuleres=$skalSimuleres for behandling 2199950")
        }

        if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET ||
            vedtak.behandling.resultat == Behandlingsresultat.AVSLÅTT ||
            beregningService.innvilgetSøknadUtenUtbetalingsperioderGrunnetEndringsPerioder(behandling = vedtak.behandling)
        ) {
            return null
        }

        /**
         * SOAP integrasjonen støtter ikke full epost som MQ,
         * så vi bruker bare første 8 tegn av saksbehandlers epost for simulering.
         * Denne verdien brukes ikke til noe i simulering.
         */

        val utbetalingsoppdrag = økonomiService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            vedtak = vedtak,
            saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
            andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForSimuleringFactory(),
            erSimulering = true
        )

        // TODO Fjern dette når vi har logget det vi skal
        if (vedtak.behandling.id == 2199950L) {
            secureLogger.info(
                "utbetalingsoppdrag =\n " +
                    "$utbetalingsoppdrag\n " +
                    "for behandling 2199950"
            )
        }

        if (featureToggleService.isEnabled(FeatureToggleConfig.KAN_GENERERE_UTBETALINGSOPPDRAG_NY)) {
            val tilkjentYtelse = utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
                andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForSimuleringFactory(),
                erSimulering = true
            )
            val gammelUtbetalingsoppdragIString = objectMapper.writeValueAsString(utbetalingsoppdrag)
            secureLogger.info("Generert utbetalingsoppdrag på gamle måte=$gammelUtbetalingsoppdragIString")
            secureLogger.info("Generert utbetalingsoppdrag på ny måte=${tilkjentYtelse.utbetalingsoppdrag}")
        }
        // Simulerer ikke mot økonomi når det ikke finnes utbetalingsperioder
        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) return null

        simulert.increment()
        return økonomiKlient.hentSimulering(utbetalingsoppdrag)
    }

    @Transactional
    fun lagreSimuleringPåBehandling(
        simuleringMottakere: List<SimuleringMottaker>,
        beahndling: Behandling
    ): List<ØkonomiSimuleringMottaker> {
        val vedtakSimuleringMottakere = simuleringMottakere.map { it.tilBehandlingSimuleringMottaker(beahndling) }
        return øknomiSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
    }

    @Transactional
    fun slettSimuleringPåBehandling(behandlingId: Long) =
        øknomiSimuleringMottakerRepository.deleteByBehandlingId(behandlingId)

    fun hentSimuleringPåBehandling(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        return øknomiSimuleringMottakerRepository.findByBehandlingId(behandlingId)
    }

    fun oppdaterSimuleringPåBehandlingVedBehov(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val behandlingErFerdigBesluttet =
            behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ||
                behandling.status == BehandlingStatus.AVSLUTTET

        val simulering = hentSimuleringPåBehandling(behandlingId)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(
            økonomiSimuleringMottakere = simulering,
            erManuellPosteringTogglePå = featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ),
            erMigreringsbehandling = behandling.erMigrering()
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
            handling = "opprette simulering"
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
            erMigreringsbehandling = false // ikke relevant når vi henter etterbetaling
        ).etterbetaling
    }

    fun hentFeilutbetaling(økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>): BigDecimal {
        return vedtakSimuleringMottakereTilRestSimulering(
            økonomiSimuleringMottakere,
            featureToggleService.isEnabled(FeatureToggleConfig.ER_MANUEL_POSTERING_TOGGLE_PÅ),
            erMigreringsbehandling = false // ikke relevant når vi henter feilutbetaling
        ).feilutbetaling
    }
}
