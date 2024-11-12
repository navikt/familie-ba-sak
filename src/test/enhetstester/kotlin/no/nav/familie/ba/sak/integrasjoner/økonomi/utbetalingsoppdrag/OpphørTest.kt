package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.simulering.lagBehandling
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag
import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class OpphørTest {
    @Test
    fun `skal opprette opphør med opphørsdato når det er rent opphør på utbetalingsperiodene og det ikke er revurdering`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val behandling = lagBehandling()

        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = "BA",
                saksnummer = "123",
                aktoer = "12345678901",
                saksbehandlerId = "1",
                utbetalingsperiode =
                    listOf(
                        Utbetalingsperiode(
                            erEndringPåEksisterendePeriode = false,
                            opphør =
                                no.nav.familie.felles.utbetalingsgenerator.domain
                                    .Opphør(dagensDato),
                            periodeId = 0L,
                            forrigePeriodeId = null,
                            datoForVedtak = dagensDato,
                            klassifisering = "asdf",
                            vedtakdatoFom = dagensDato.minusMonths(1),
                            vedtakdatoTom = dagensDato.plusMonths(1),
                            satsType = Utbetalingsperiode.SatsType.MND,
                            behandlingId = behandling.id,
                            sats = BigDecimal(220),
                            utbetalesTil = "12345678901",
                        ),
                    ),
            )

        // Act
        val opphør = Opphør.opprettFor(utbetalingsoppdrag, behandling)

        // Assert
        assertThat(opphør.erRentOpphør).isTrue()
        assertThat(opphør.opphørsdato).isEqualTo(LocalDate.of(2024, 11, 1))
    }

    @Test
    fun `skal opprette opphør uten opphørsdato når det ikke er rent opphør på utbetalingsperiodene og det ikke er revurdering`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val behandling = lagBehandling()

        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = "BA",
                saksnummer = "123",
                aktoer = "12345678901",
                saksbehandlerId = "1",
                utbetalingsperiode =
                    listOf(
                        Utbetalingsperiode(
                            erEndringPåEksisterendePeriode = false,
                            opphør = null,
                            periodeId = 0L,
                            forrigePeriodeId = null,
                            datoForVedtak = dagensDato,
                            klassifisering = "asdf",
                            vedtakdatoFom = dagensDato.minusMonths(1),
                            vedtakdatoTom = dagensDato.plusMonths(1),
                            satsType = Utbetalingsperiode.SatsType.MND,
                            behandlingId = behandling.id,
                            sats = BigDecimal(220),
                            utbetalesTil = "12345678901",
                        ),
                    ),
            )

        // Act
        val opphør = Opphør.opprettFor(utbetalingsoppdrag, behandling)

        // Assert
        assertThat(opphør.erRentOpphør).isFalse()
        assertThat(opphør.opphørsdato).isNull()
    }

    @Test
    fun `skal opprette opphør uten opphørsdato når utbetalingsperiodene er tom og det ikke er revurdering`() {
        // Arrange
        val behandling = lagBehandling()

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
        val opphør = Opphør.opprettFor(utbetalingsoppdrag, behandling)

        // Assert
        assertThat(opphør.erRentOpphør).isFalse()
        assertThat(opphør.opphørsdato).isNull()
    }

    @Test
    fun `skal opprette opphør når det er rent opphør på utbetalingsperiodene for revurdering`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
            )

        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = "BA",
                saksnummer = "123",
                aktoer = "12345678901",
                saksbehandlerId = "1",
                utbetalingsperiode =
                    listOf(
                        Utbetalingsperiode(
                            erEndringPåEksisterendePeriode = false,
                            opphør =
                                no.nav.familie.felles.utbetalingsgenerator.domain
                                    .Opphør(dagensDato),
                            periodeId = 0L,
                            forrigePeriodeId = null,
                            datoForVedtak = dagensDato,
                            klassifisering = "asdf",
                            vedtakdatoFom = dagensDato.minusMonths(1),
                            vedtakdatoTom = dagensDato.plusMonths(1),
                            satsType = Utbetalingsperiode.SatsType.MND,
                            behandlingId = behandling.id,
                            sats = BigDecimal(220),
                            utbetalesTil = "12345678901",
                        ),
                    ),
            )

        // Act
        val opphør = Opphør.opprettFor(utbetalingsoppdrag, behandling)

        // Assert
        assertThat(opphør.erRentOpphør).isTrue()
        assertThat(opphør.opphørsdato).isEqualTo(LocalDate.of(2024, 11, 1))
    }

    @Test
    fun `skal opprette opphør uten opphørsdato når det ikke er rent opphør på utbetalingsperiodene og det er revurdering`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
            )

        val utbetalingsoppdrag =
            Utbetalingsoppdrag(
                kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                fagSystem = "BA",
                saksnummer = "123",
                aktoer = "12345678901",
                saksbehandlerId = "1",
                utbetalingsperiode =
                    listOf(
                        Utbetalingsperiode(
                            erEndringPåEksisterendePeriode = false,
                            opphør = null,
                            periodeId = 0L,
                            forrigePeriodeId = null,
                            datoForVedtak = dagensDato,
                            klassifisering = "asdf",
                            vedtakdatoFom = dagensDato.minusMonths(1),
                            vedtakdatoTom = dagensDato.plusMonths(1),
                            satsType = Utbetalingsperiode.SatsType.MND,
                            behandlingId = behandling.id,
                            sats = BigDecimal(220),
                            utbetalesTil = "12345678901",
                        ),
                    ),
            )

        // Act
        val opphør = Opphør.opprettFor(utbetalingsoppdrag, behandling)

        // Assert
        assertThat(opphør.erRentOpphør).isFalse()
        assertThat(opphør.opphørsdato).isNull()
    }

    @Test
    fun `skal opprette opphør uten opphørsdato når utbetalingsperiodene er tom og det er revurdering`() {
        // Arrange
        val behandling =
            lagBehandling(
                behandlingType = BehandlingType.REVURDERING,
            )

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
        val opphør = Opphør.opprettFor(utbetalingsoppdrag, behandling)

        // Assert
        assertThat(opphør.erRentOpphør).isFalse()
        assertThat(opphør.opphørsdato).isNull()
    }
}
