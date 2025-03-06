package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.årMnd
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RestTilkjentYtelseTest {
    @Test
    fun `Skal slå sammen etterfølgende andeler med samme kalkulert utbetalingsbeløp, ytelsetype og prosent`() {
        val aktør = randomAktør()

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = årMnd("2020-03"),
                    tom = årMnd("2020-12"),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    beløp = 1234,
                    prosent = BigDecimal.valueOf(100),
                    aktør = aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = årMnd("2021-01"),
                    tom = årMnd("2021-12"),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    beløp = 1234,
                    prosent = BigDecimal.valueOf(100),
                    aktør = aktør,
                ),
            )

        val restYtelsePerioder = andeler.tilRestYtelsePerioder()
        val forventetRestYtelsePeriode = listOf(RestYtelsePeriode(beløp = 1234, stønadFom = årMnd("2020-03"), stønadTom = årMnd("2021-12"), ytelseType = YtelseType.ORDINÆR_BARNETRYGD, skalUtbetales = true))
        Assertions.assertThat(restYtelsePerioder).containsAll(forventetRestYtelsePeriode).hasSize(forventetRestYtelsePeriode.size)
    }

    @Test
    fun `Skal ikke slå sammen etterfølgende andeler med forskjellig kalkulert utbetalingsbeløp, ytelsetype eller prosent`() {
        val aktør = randomAktør()

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = årMnd("2020-03"),
                    tom = årMnd("2020-12"),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    beløp = 1234,
                    prosent = BigDecimal.valueOf(100),
                    aktør = aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = årMnd("2021-01"),
                    tom = årMnd("2021-12"),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    beløp = 1234,
                    prosent = BigDecimal.valueOf(100),
                    aktør = aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = årMnd("2022-01"),
                    tom = årMnd("2022-12"),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    beløp = 0,
                    prosent = BigDecimal.valueOf(100),
                    aktør = aktør,
                ),
                lagAndelTilkjentYtelse(
                    fom = årMnd("2023-01"),
                    tom = årMnd("2023-12"),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    beløp = 0,
                    prosent = BigDecimal.valueOf(0),
                    aktør = aktør,
                ),
            )

        val restYtelsePerioder = andeler.tilRestYtelsePerioder()
        val forventetRestYtelsePerioder =
            listOf(
                RestYtelsePeriode(beløp = 1234, stønadFom = årMnd("2020-03"), stønadTom = årMnd("2020-12"), ytelseType = YtelseType.SMÅBARNSTILLEGG, skalUtbetales = true),
                RestYtelsePeriode(beløp = 1234, stønadFom = årMnd("2021-01"), stønadTom = årMnd("2021-12"), ytelseType = YtelseType.UTVIDET_BARNETRYGD, skalUtbetales = true),
                RestYtelsePeriode(beløp = 0, stønadFom = årMnd("2022-01"), stønadTom = årMnd("2022-12"), ytelseType = YtelseType.UTVIDET_BARNETRYGD, skalUtbetales = true),
                RestYtelsePeriode(beløp = 0, stønadFom = årMnd("2023-01"), stønadTom = årMnd("2023-12"), ytelseType = YtelseType.UTVIDET_BARNETRYGD, skalUtbetales = false),
            )
        Assertions.assertThat(restYtelsePerioder).containsAll(forventetRestYtelsePerioder).hasSize(forventetRestYtelsePerioder.size)
    }
}
