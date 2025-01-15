package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import lagBehandling
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.felles.utbetalingsgenerator.domain.Opphør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KlassifiseringKorrigererTest {
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val unleashNextMedContextService = mockk<UnleashNextMedContextService>()
    private val klassifiseringKorrigerer =
        KlassifiseringKorrigerer(
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            unleashNextMedContextService = unleashNextMedContextService,
        )

    @Test
    fun `skal returnere originalt utbetalingsoppdrag dersom toggle er av for fagsak`() {
        // Arrange
        val behandling = lagBehandling()
        val beregnetUtbetalingsoppdragLongId =
            lagBeregnetUtbetalingsoppdragLongId(
                utbetalingsperioder =
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                            fom = LocalDate.now().førsteDagIInneværendeMåned(),
                            tom = LocalDate.now().sisteDagIMåned(),
                        ),
                    ),
                andeler = emptyList(),
            )

        every { unleashNextMedContextService.isEnabled(FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD, behandling.id) } returns false

        // Act
        val justertUtbetalingsoppdrag =
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
                beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdragLongId,
                behandling = behandling,
            )

        // Assert
        assertThat(justertUtbetalingsoppdrag).isEqualTo(beregnetUtbetalingsoppdragLongId)
    }

    @Test
    fun `skal returnere originalt utbetalingsoppdrag dersom fagsak er over på ny klassekode`() {
        // Arrange
        val behandling = lagBehandling()
        val beregnetUtbetalingsoppdragLongId =
            lagBeregnetUtbetalingsoppdragLongId(
                utbetalingsperioder =
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                            fom = LocalDate.now().førsteDagIInneværendeMåned(),
                            tom = LocalDate.now().sisteDagIMåned(),
                        ),
                    ),
                andeler = emptyList(),
            )

        every { unleashNextMedContextService.isEnabled(FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD, behandling.id) } returns true
        every { tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(behandling.fagsak.id) } returns true

        // Act
        val justertUtbetalingsoppdrag =
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
                beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdragLongId,
                behandling = behandling,
            )

        // Assert
        assertThat(justertUtbetalingsoppdrag).isEqualTo(beregnetUtbetalingsoppdragLongId)
    }

    @Test
    fun `skal returnere originalt utbetalingsoppdrag dersom behandling ikke inneholder et opphør av utvidet barnetrygd`() {
        // Arrange
        val behandling = lagBehandling()
        val beregnetUtbetalingsoppdragLongId =
            lagBeregnetUtbetalingsoppdragLongId(
                utbetalingsperioder =
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                            fom = LocalDate.now().førsteDagIInneværendeMåned(),
                            tom = LocalDate.now().sisteDagIMåned(),
                        ),
                    ),
                andeler = emptyList(),
            )

        every { unleashNextMedContextService.isEnabled(FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD, behandling.id) } returns true
        every { tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(behandling.fagsak.id) } returns false

        // Act
        val justertUtbetalingsoppdrag =
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
                beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdragLongId,
                behandling = behandling,
            )

        // Assert
        assertThat(justertUtbetalingsoppdrag).isEqualTo(beregnetUtbetalingsoppdragLongId)
    }

    @Test
    fun `skal erstatte klassekode for periode med opphør av utvidet barnetrygd når toggle er på og fagsak ikke er over på ny klassekode`() {
        // Arrange
        val behandling = lagBehandling()
        val beregnetUtbetalingsoppdragLongId =
            lagBeregnetUtbetalingsoppdragLongId(
                utbetalingsperioder =
                    listOf(
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 0,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                            fom = LocalDate.now().førsteDagIInneværendeMåned(),
                            tom = LocalDate.now().sisteDagIMåned(),
                            opphør = Opphør(opphørDatoFom = LocalDate.now()),
                        ),
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 2,
                            forrigePeriodeId = null,
                            ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                            fom = LocalDate.now().førsteDagIInneværendeMåned(),
                            tom = LocalDate.now().sisteDagIMåned(),
                            opphør = Opphør(opphørDatoFom = LocalDate.now()),
                        ),
                        lagUtbetalingsperiode(
                            behandlingId = behandling.id,
                            periodeId = 1,
                            forrigePeriodeId = 0,
                            ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD,
                            fom = LocalDate.now().plusMonths(1).førsteDagIInneværendeMåned(),
                            tom = LocalDate.now().plusMonths(1).sisteDagIMåned(),
                        ),
                    ),
                andeler = emptyList(),
            )

        val forventetUtbetalingsoppdrag =
            beregnetUtbetalingsoppdragLongId.copy(
                utbetalingsoppdrag =
                    beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.copy(
                        utbetalingsperiode =
                            beregnetUtbetalingsoppdragLongId.utbetalingsoppdrag.utbetalingsperiode.map {
                                if (it.opphør != null) it.copy(klassifisering = YtelsetypeBA.UTVIDET_BARNETRYGD_GAMMEL.klassifisering) else it
                            },
                    ),
            )

        every { unleashNextMedContextService.isEnabled(FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD, behandling.id) } returns true
        every { tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(behandling.fagsak.id) } returns false

        // Act
        val justertUtbetalingsoppdrag =
            klassifiseringKorrigerer.korrigerKlassifiseringVedBehov(
                beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdragLongId,
                behandling = behandling,
            )

        // Assert
        assertThat(justertUtbetalingsoppdrag).isNotEqualTo(beregnetUtbetalingsoppdragLongId)
        assertThat(justertUtbetalingsoppdrag).isEqualTo(forventetUtbetalingsoppdrag)
    }
}
