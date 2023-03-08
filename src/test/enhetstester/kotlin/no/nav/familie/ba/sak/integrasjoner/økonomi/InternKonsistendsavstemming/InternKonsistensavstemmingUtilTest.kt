package no.nav.familie.ba.sak.integrasjoner.økonomi.InternKonsistendsavstemming

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InternKonsistensavstemmingUtilTest {

    @Test
    fun `Skal ignorere forskjeller før første utbetalingsoppdragsperiode`() {
        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2021-12"),
                tom = YearMonth.parse("2021-12"),
                beløp = 1654,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2022-01"),
                tom = YearMonth.parse("2023-02"),
                beløp = 1676,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2023-03"),
                tom = YearMonth.parse("2027-10"),
                beløp = 1723,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2027-11"),
                tom = YearMonth.parse("2039-10"),
                beløp = 1083,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            )
        )
        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(mockUtbetalingsoppdrag)

        Assertions.assertFalse(erForskjellMellomAndelerOgOppdrag(andeler, utbetalingsoppdrag, 0L))
    }

    @Test
    fun `skal registrere at det er forskjell det er diff etter første periode i utbetalingsoppdraget`() {
        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2021-12"),
                tom = YearMonth.parse("2021-12"),
                beløp = 1654,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2022-01"),
                tom = YearMonth.parse("2023-02"),
                beløp = 1676,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2023-03"),
                tom = YearMonth.parse("2027-10"),
                beløp = 1723,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2027-11"),
                tom = YearMonth.parse("2039-10"),
                beløp = 1083,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse("2039-11"),
                tom = YearMonth.parse("2040-10"),
                beløp = 1083,
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD
            )
        )
        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(mockUtbetalingsoppdrag)

        Assertions.assertTrue(erForskjellMellomAndelerOgOppdrag(andeler, utbetalingsoppdrag, 0L))
    }
}

private val mockUtbetalingsoppdrag = """
    {
      "kodeEndring": "ENDR",
      "fagSystem": "BA",
      "saksnummer": "1",
      "aktoer": "1",
      "saksbehandlerId": "VL",
      "avstemmingTidspunkt": "2023-02-08T16:12:56.200284803",
      "utbetalingsperiode": [
        {
          "erEndringPåEksisterendePeriode": true,
          "opphør": {
            "opphørDatoFom": "2022-01-01"
          },
          "periodeId": 2,
          "forrigePeriodeId": 1,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2027-11-01",
          "vedtakdatoTom": "2039-10-31",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 3,
          "forrigePeriodeId": 2,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2022-01-01",
          "vedtakdatoTom": "2023-02-28",
          "sats": 1676,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 4,
          "forrigePeriodeId": 3,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2023-03-01",
          "vedtakdatoTom": "2027-10-31",
          "sats": 1723,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 5,
          "forrigePeriodeId": 4,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2027-11-01",
          "vedtakdatoTom": "2039-10-31",
          "sats": 1083,
          "satsType": "MND",
          "utbetalesTil": "1",
          "behandlingId": 1,
          "utbetalingsgrad": null
        }
      ],
      "gOmregning": false
    }
""".trimIndent()
