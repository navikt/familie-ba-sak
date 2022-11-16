package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth

internal class BehandlingsresultatServiceTest {

    private val featureToggleService = mockk<FeatureToggleService>()

    private val service = BehandlingsresultatService(
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        featureToggleService
    )

    @Test
    fun `endra fom eller tom for utvida barnetrygd gir behandlingsresultat endret`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SJEKK_OM_UTVIDET_ER_ENDRET_BEHANDLINGSRESULTAT) } returns true
        val søkerAktør = Aktør("1234567890123")
        val ytelsePersonSøker = YtelsePerson(
            søkerAktør,
            YtelseType.UTVIDET_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE, KravOpprinnelse.TIDLIGERE),
            setOf(YtelsePersonResultat.OPPHØRT),
            YearMonth.of(2022, Month.APRIL)
        )
        val ytelsePersonBarn = YtelsePerson(
            Aktør("1234567890124"),
            YtelseType.ORDINÆR_BARNETRYGD,
            listOf(KravOpprinnelse.TIDLIGERE),
            setOf(),
            YearMonth.of(2037, Month.MAY)
        )
        val andelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2022, Month.APRIL),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )
        val forrigeAndelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )
        val behandlingsresultat = service.utledBehandlingsresultat(
            ytelsePersonerMedResultat = listOf(ytelsePersonSøker, ytelsePersonBarn),
            andelerMedEndringer = listOf(andelMedEndring),
            forrigeAndelerMedEndringer = listOf(forrigeAndelMedEndring),
            behandling = lagBehandling()
        )
        assertThat(behandlingsresultat).isEqualTo(Behandlingsresultat.ENDRET_UTBETALING)
    }

    @Test
    fun `samme fom og tom for utvida barnetrygd gir behandlingsresultat fortsatt innvilget`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SJEKK_OM_UTVIDET_ER_ENDRET_BEHANDLINGSRESULTAT) } returns true

        val søkerAktør = Aktør("1234567890123")
        val ytelsePersonSøker = YtelsePerson(
            søkerAktør,
            YtelseType.UTVIDET_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE, KravOpprinnelse.TIDLIGERE),
            setOf(YtelsePersonResultat.OPPHØRT),
            YearMonth.of(2022, Month.APRIL)
        )
        val ytelsePersonBarn = YtelsePerson(
            Aktør("1234567890124"),
            YtelseType.ORDINÆR_BARNETRYGD,
            listOf(KravOpprinnelse.TIDLIGERE),
            setOf(),
            YearMonth.of(2037, Month.MAY)
        )
        val andelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )
        val forrigeAndelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )

        val behandlingsresultat = service.utledBehandlingsresultat(
            ytelsePersonerMedResultat = listOf(ytelsePersonSøker, ytelsePersonBarn),
            andelerMedEndringer = listOf(andelMedEndring),
            forrigeAndelerMedEndringer = listOf(forrigeAndelMedEndring),
            behandling = lagBehandling()
        )
        assertThat(behandlingsresultat).isEqualTo(Behandlingsresultat.FORTSATT_INNVILGET)
    }

    @Test
    fun `utvida barnetrygd nå, men ingenting før gir behandlingsresultat innvilget`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SJEKK_OM_UTVIDET_ER_ENDRET_BEHANDLINGSRESULTAT) } returns true

        val søkerAktør = Aktør("1234567890123")
        val ytelsePersonSøker = YtelsePerson(
            søkerAktør,
            YtelseType.UTVIDET_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE),
            setOf(YtelsePersonResultat.INNVILGET),
            YearMonth.of(2022, Month.APRIL)
        )
        val ytelsePersonBarn = YtelsePerson(
            Aktør("1234567890124"),
            YtelseType.ORDINÆR_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE),
            setOf(YtelsePersonResultat.INNVILGET),
            YearMonth.of(2037, Month.MAY)
        )
        val andelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )

        val behandlingsresultat = service.utledBehandlingsresultat(
            ytelsePersonerMedResultat = listOf(ytelsePersonSøker, ytelsePersonBarn),
            andelerMedEndringer = listOf(andelMedEndring),
            forrigeAndelerMedEndringer = listOf(),
            behandling = lagBehandling()
        )
        assertThat(behandlingsresultat).isEqualTo(Behandlingsresultat.INNVILGET)
    }

    @Test
    fun `utvida barnetrygd før, men alt opphørt nå gir behandlingsresultat innvilget`() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SJEKK_OM_UTVIDET_ER_ENDRET_BEHANDLINGSRESULTAT) } returns true

        val søkerAktør = Aktør("1234567890123")
        val ytelsePersonSøker = YtelsePerson(
            søkerAktør,
            YtelseType.UTVIDET_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE),
            setOf(YtelsePersonResultat.OPPHØRT),
            YearMonth.of(2022, Month.OCTOBER)
        )
        val ytelsePersonBarn = YtelsePerson(
            Aktør("1234567890124"),
            YtelseType.ORDINÆR_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE),
            setOf(YtelsePersonResultat.OPPHØRT),
            YearMonth.of(2022, Month.OCTOBER)
        )
        val forrigeAndelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )

        val behandlingsresultat = service.utledBehandlingsresultat(
            ytelsePersonerMedResultat = listOf(ytelsePersonSøker, ytelsePersonBarn),
            andelerMedEndringer = listOf(),
            forrigeAndelerMedEndringer = listOf(forrigeAndelMedEndring),
            behandling = lagBehandling()
        )
        assertThat(behandlingsresultat).isEqualTo(Behandlingsresultat.OPPHØRT)
    }
}
