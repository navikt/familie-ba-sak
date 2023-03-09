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
    fun `skal at det er forskjellig avsluttningsdato mellom andelene og utbetalingsoppdraget`() {
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

    @Test
    fun `skal ikke si det er forskjell ved riktig utbetalingsoppdrag når det er flere kjeder`() {
        val andelStringer = listOf(
            "2021-05,2021-08,1354,ORDINÆR_BARNETRYGD",
            "2021-09,2021-12,1654,ORDINÆR_BARNETRYGD",

            "2022-01,2023-02,1676,ORDINÆR_BARNETRYGD",
            "2023-03,2024-11,1723,ORDINÆR_BARNETRYGD",
            "2024-12,2036-11,1083,ORDINÆR_BARNETRYGD",

            "2021-05,2023-02,1054,UTVIDET_BARNETRYGD",
            "2023-03,2036-11,2489,UTVIDET_BARNETRYGD"
        )

        val andeler = andelStringer.map { it.split(",") }.map {
            lagAndelTilkjentYtelse(
                fom = YearMonth.parse(it[0]),
                tom = YearMonth.parse(it[1]),
                beløp = it[2].toInt(),
                ytelseType = YtelseType.valueOf(it[3])
            )
        }

        val utbetalingsoppdrag = objectMapper.readValue<Utbetalingsoppdrag>(utbetalingsoppdragMockMedUtvidet)

        Assertions.assertFalse(erForskjellMellomAndelerOgOppdrag(andeler, utbetalingsoppdrag, 0L))
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

private val utbetalingsoppdragMockMedUtvidet = """
    {
      "kodeEndring": "ENDR",
      "fagSystem": "BA",
      "saksnummer": "200028561",
      "aktoer": "02416938515",
      "saksbehandlerId": "VL",
      "avstemmingTidspunkt": "2023-02-08T15:57:38.341011606",
      "utbetalingsperiode": [
        {
          "erEndringPåEksisterendePeriode": true,
          "opphør": {
            "opphørDatoFom": "2022-01-01"
          },
          "periodeId": 3,
          "forrigePeriodeId": 2,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2024-12-01",
          "vedtakdatoTom": "2036-11-30",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": true,
          "opphør": {
            "opphørDatoFom": "2021-05-01"
          },
          "periodeId": 4,
          "forrigePeriodeId": null,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2021-05-01",
          "vedtakdatoTom": "2036-11-30",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 5,
          "forrigePeriodeId": 3,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2022-01-01",
          "vedtakdatoTom": "2023-02-28",
          "sats": 1676,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 6,
          "forrigePeriodeId": 5,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2023-03-01",
          "vedtakdatoTom": "2024-11-30",
          "sats": 1723,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 7,
          "forrigePeriodeId": 6,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2024-12-01",
          "vedtakdatoTom": "2036-11-30",
          "sats": 1083,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 8,
          "forrigePeriodeId": 4,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2021-05-01",
          "vedtakdatoTom": "2023-02-28",
          "sats": 1054,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        },
        {
          "erEndringPåEksisterendePeriode": false,
          "opphør": null,
          "periodeId": 9,
          "forrigePeriodeId": 8,
          "datoForVedtak": "2023-02-08",
          "klassifisering": "BATR",
          "vedtakdatoFom": "2023-03-01",
          "vedtakdatoTom": "2036-11-30",
          "sats": 2489,
          "satsType": "MND",
          "utbetalesTil": "02416938515",
          "behandlingId": 100134370,
          "utbetalingsgrad": null
        }
      ],
      "gOmregning": false
    }
""".trimIndent()