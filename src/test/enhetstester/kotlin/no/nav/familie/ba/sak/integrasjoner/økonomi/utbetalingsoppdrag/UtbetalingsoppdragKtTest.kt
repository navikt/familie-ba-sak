package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Opphør
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class UtbetalingsoppdragKtTest {
    @Nested
    inner class TilUtbetalingsoppdragDtoTest {
        @Test
        fun `skal mappe til restUtbetalingsoppdrag uten utbetalingsperiode`() {
            // Arrange
            val utbetalingsoppdrag =
                Utbetalingsoppdrag(
                    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                    fagSystem = "BA",
                    saksnummer = "123",
                    aktoer = "12345678901",
                    saksbehandlerId = "1",
                    utbetalingsperiode = emptyList(),
                )

            // Act
            val restUtbetalingsoppdrag = utbetalingsoppdrag.tilUtbetalingsoppdragDto()

            // Assert
            assertThat(restUtbetalingsoppdrag.kodeEndring).isEqualTo(no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY)
            assertThat(restUtbetalingsoppdrag.fagSystem).isEqualTo(utbetalingsoppdrag.fagSystem)
            assertThat(restUtbetalingsoppdrag.saksnummer).isEqualTo(utbetalingsoppdrag.saksnummer)
            assertThat(restUtbetalingsoppdrag.aktoer).isEqualTo(utbetalingsoppdrag.aktoer)
            assertThat(restUtbetalingsoppdrag.saksbehandlerId).isEqualTo(utbetalingsoppdrag.saksbehandlerId)
            assertThat(restUtbetalingsoppdrag.avstemmingTidspunkt).isEqualTo(utbetalingsoppdrag.avstemmingTidspunkt)
            assertThat(restUtbetalingsoppdrag.utbetalingsperiode).isEmpty()
            assertThat(restUtbetalingsoppdrag.gOmregning).isEqualTo(utbetalingsoppdrag.gOmregning)
        }

        @Test
        fun `skal mappe til restUtbetalingsoppdrag med en utbetalingsperiode`() {
            // Arrange
            val dagensDato = LocalDate.of(2024, 8, 1)

            val utbetalingsperiode =
                Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = false,
                    opphør = Opphør(dagensDato),
                    periodeId = 0L,
                    forrigePeriodeId = null,
                    datoForVedtak = dagensDato,
                    klassifisering = "asdf",
                    vedtakdatoFom = dagensDato.minusMonths(1),
                    vedtakdatoTom = dagensDato.plusMonths(1),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    behandlingId = 0L,
                    sats = BigDecimal(220),
                    utbetalesTil = "12345678901",
                )

            val utbetalingsoppdrag =
                Utbetalingsoppdrag(
                    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                    fagSystem = "BA",
                    saksnummer = "123",
                    aktoer = "12345678901",
                    saksbehandlerId = "1",
                    utbetalingsperiode = listOf(utbetalingsperiode),
                )

            // Act
            val utbetalingsoppdragDto = utbetalingsoppdrag.tilUtbetalingsoppdragDto()

            // Assert
            assertThat(utbetalingsoppdragDto.kodeEndring).isEqualTo(no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY)
            assertThat(utbetalingsoppdragDto.fagSystem).isEqualTo(utbetalingsoppdrag.fagSystem)
            assertThat(utbetalingsoppdragDto.saksnummer).isEqualTo(utbetalingsoppdrag.saksnummer)
            assertThat(utbetalingsoppdragDto.aktoer).isEqualTo(utbetalingsoppdrag.aktoer)
            assertThat(utbetalingsoppdragDto.saksbehandlerId).isEqualTo(utbetalingsoppdrag.saksbehandlerId)
            assertThat(utbetalingsoppdragDto.avstemmingTidspunkt).isEqualTo(utbetalingsoppdrag.avstemmingTidspunkt)
            assertThat(utbetalingsoppdragDto.utbetalingsperiode).hasSize(1)
            assertThat(utbetalingsoppdragDto.utbetalingsperiode).allSatisfy {
                assertThat(it.erEndringPåEksisterendePeriode).isEqualTo(utbetalingsperiode.erEndringPåEksisterendePeriode)
                assertThat(it.opphør!!.opphørDatoFom).isEqualTo(utbetalingsperiode.opphør!!.opphørDatoFom)
                assertThat(it.periodeId).isEqualTo(utbetalingsperiode.periodeId)
                assertThat(it.forrigePeriodeId).isEqualTo(utbetalingsperiode.forrigePeriodeId)
                assertThat(it.datoForVedtak).isEqualTo(utbetalingsperiode.datoForVedtak)
                assertThat(it.klassifisering).isEqualTo(utbetalingsperiode.klassifisering)
                assertThat(it.vedtakdatoFom).isEqualTo(utbetalingsperiode.vedtakdatoFom)
                assertThat(it.vedtakdatoTom).isEqualTo(utbetalingsperiode.vedtakdatoTom)
                assertThat(it.sats).isEqualTo(utbetalingsperiode.sats)
                assertThat(it.satsType).isEqualTo(no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.MND)
                assertThat(it.utbetalesTil).isEqualTo(utbetalingsperiode.utbetalesTil)
                assertThat(it.behandlingId).isEqualTo(utbetalingsperiode.behandlingId)
                assertThat(it.utbetalingsgrad).isEqualTo(utbetalingsperiode.utbetalingsgrad)
            }
            assertThat(utbetalingsoppdragDto.gOmregning).isEqualTo(utbetalingsoppdrag.gOmregning)
        }

        @Test
        fun `skal mappe til restUtbetalingsoppdrag med to utbetalingsperioder`() {
            // Arrange
            val dagensDato = LocalDate.of(2024, 8, 1)

            val utbetalingsperiode1 =
                Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = false,
                    opphør = Opphør(dagensDato),
                    periodeId = 0L,
                    forrigePeriodeId = null,
                    datoForVedtak = dagensDato,
                    klassifisering = "asdf",
                    vedtakdatoFom = dagensDato.minusMonths(1),
                    vedtakdatoTom = dagensDato.plusMonths(1),
                    satsType = Utbetalingsperiode.SatsType.MND,
                    behandlingId = 0L,
                    sats = BigDecimal(220),
                    utbetalesTil = "12345678901",
                )

            val utbetalingsperiode2 =
                Utbetalingsperiode(
                    erEndringPåEksisterendePeriode = true,
                    opphør = Opphør(dagensDato.minusMonths(1)),
                    periodeId = 1L,
                    forrigePeriodeId = 0L,
                    datoForVedtak = dagensDato.plusDays(1),
                    klassifisering = "fdsa",
                    vedtakdatoFom = dagensDato.minusMonths(2),
                    vedtakdatoTom = dagensDato.plusMonths(2),
                    satsType = Utbetalingsperiode.SatsType.DAG,
                    behandlingId = 1L,
                    sats = BigDecimal(150),
                    utbetalesTil = "12345678902",
                )

            val utbetalingsoppdrag =
                Utbetalingsoppdrag(
                    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                    fagSystem = "BA",
                    saksnummer = "123",
                    aktoer = "12345678901",
                    saksbehandlerId = "1",
                    utbetalingsperiode = listOf(utbetalingsperiode1, utbetalingsperiode2),
                )

            // Act
            val restUtbetalingsoppdrag = utbetalingsoppdrag.tilUtbetalingsoppdragDto()

            // Assert
            assertThat(restUtbetalingsoppdrag.kodeEndring).isEqualTo(no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY)
            assertThat(restUtbetalingsoppdrag.fagSystem).isEqualTo(utbetalingsoppdrag.fagSystem)
            assertThat(restUtbetalingsoppdrag.saksnummer).isEqualTo(utbetalingsoppdrag.saksnummer)
            assertThat(restUtbetalingsoppdrag.aktoer).isEqualTo(utbetalingsoppdrag.aktoer)
            assertThat(restUtbetalingsoppdrag.saksbehandlerId).isEqualTo(utbetalingsoppdrag.saksbehandlerId)
            assertThat(restUtbetalingsoppdrag.avstemmingTidspunkt).isEqualTo(utbetalingsoppdrag.avstemmingTidspunkt)
            assertThat(restUtbetalingsoppdrag.utbetalingsperiode).hasSize(2)
            assertThat(restUtbetalingsoppdrag.utbetalingsperiode).anySatisfy {
                assertThat(it.erEndringPåEksisterendePeriode).isEqualTo(utbetalingsperiode1.erEndringPåEksisterendePeriode)
                assertThat(it.opphør!!.opphørDatoFom).isEqualTo(utbetalingsperiode1.opphør!!.opphørDatoFom)
                assertThat(it.periodeId).isEqualTo(utbetalingsperiode1.periodeId)
                assertThat(it.forrigePeriodeId).isEqualTo(utbetalingsperiode1.forrigePeriodeId)
                assertThat(it.datoForVedtak).isEqualTo(utbetalingsperiode1.datoForVedtak)
                assertThat(it.klassifisering).isEqualTo(utbetalingsperiode1.klassifisering)
                assertThat(it.vedtakdatoFom).isEqualTo(utbetalingsperiode1.vedtakdatoFom)
                assertThat(it.vedtakdatoTom).isEqualTo(utbetalingsperiode1.vedtakdatoTom)
                assertThat(it.sats).isEqualTo(utbetalingsperiode1.sats)
                assertThat(it.satsType).isEqualTo(no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.MND)
                assertThat(it.utbetalesTil).isEqualTo(utbetalingsperiode1.utbetalesTil)
                assertThat(it.behandlingId).isEqualTo(utbetalingsperiode1.behandlingId)
                assertThat(it.utbetalingsgrad).isEqualTo(utbetalingsperiode1.utbetalingsgrad)
            }
            assertThat(restUtbetalingsoppdrag.utbetalingsperiode).anySatisfy {
                assertThat(it.erEndringPåEksisterendePeriode).isEqualTo(utbetalingsperiode2.erEndringPåEksisterendePeriode)
                assertThat(it.opphør!!.opphørDatoFom).isEqualTo(utbetalingsperiode2.opphør!!.opphørDatoFom)
                assertThat(it.periodeId).isEqualTo(utbetalingsperiode2.periodeId)
                assertThat(it.forrigePeriodeId).isEqualTo(utbetalingsperiode2.forrigePeriodeId)
                assertThat(it.datoForVedtak).isEqualTo(utbetalingsperiode2.datoForVedtak)
                assertThat(it.klassifisering).isEqualTo(utbetalingsperiode2.klassifisering)
                assertThat(it.vedtakdatoFom).isEqualTo(utbetalingsperiode2.vedtakdatoFom)
                assertThat(it.vedtakdatoTom).isEqualTo(utbetalingsperiode2.vedtakdatoTom)
                assertThat(it.sats).isEqualTo(utbetalingsperiode2.sats)
                assertThat(it.satsType).isEqualTo(no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.DAG)
                assertThat(it.utbetalesTil).isEqualTo(utbetalingsperiode2.utbetalesTil)
                assertThat(it.behandlingId).isEqualTo(utbetalingsperiode2.behandlingId)
                assertThat(it.utbetalingsgrad).isEqualTo(utbetalingsperiode2.utbetalingsgrad)
            }
            assertThat(restUtbetalingsoppdrag.gOmregning).isEqualTo(utbetalingsoppdrag.gOmregning)
        }
    }
}
