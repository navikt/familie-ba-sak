package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.rangeTo
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.common.tilMånedÅrMedium
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.datagenerator.lagValutakurs
import no.nav.familie.ba.sak.datagenerator.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class BrevUtilsTest {
    @Test
    fun `hent dokumenttittel dersom denne skal overstyres for behandlingen`() {
        // Arrange
        val revurdering = lagBehandling().copy(type = BehandlingType.REVURDERING)

        // Assert
        assertNull(hentOverstyrtDokumenttittel(lagBehandling().copy(type = BehandlingType.FØRSTEGANGSBEHANDLING)))
        assertNull(hentOverstyrtDokumenttittel(revurdering))

        Assertions.assertEquals(
            "Vedtak om endret barnetrygd - barn 18 år",
            hentOverstyrtDokumenttittel(revurdering.copy(opprettetÅrsak = BehandlingÅrsak.OMREGNING_18ÅR)),
        )
        Assertions.assertEquals(
            "Vedtak om endret barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)),
        )
        Assertions.assertEquals(
            "Vedtak om fortsatt barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.FORTSATT_INNVILGET)),
        )
        assertNull(hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.OPPHØRT)))
    }

    @Test
    fun `Skal gi riktig dato for opphørstester`() {
        // Arrange
        val sisteFom = LocalDate.now().minusMonths(2)

        val opphørsperioder =
            listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().minusYears(1).plusMonths(2),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    fom = LocalDate.now().minusMonths(5),
                    tom = LocalDate.now().minusMonths(4),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    fom = sisteFom,
                    tom = LocalDate.now(),
                    type = Vedtaksperiodetype.OPPHØR,
                ),
            )

        // Act & Assert
        Assertions.assertEquals(sisteFom.tilMånedÅr(), hentVirkningstidspunktForDødsfallbrev(opphørsperioder, 0L))
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal gi utbetalingsinfo for alle måneder etter endringstidspunktet for ett barn hvor søker har utvidet og småbarnstillegg`() {
        // Arrange
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 6, 15))

        val endringstidspunkt = LocalDate.now().minusMonths(7)

        val andelerTilkjentYtelse =
            listOf(
                // Søker har utvidet barnetrygd og småbarnstillegg de siste 12 månedene.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør,
                    kalkulertUtbetalingsbeløp = sats(YtelseType.UTVIDET_BARNETRYGD),
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    aktør = søker.aktør,
                    kalkulertUtbetalingsbeløp = sats(YtelseType.SMÅBARNSTILLEGG),
                ),
                // Barn har barnetrygd de siste 12 månedene, og fra og med 7 måneder siden har vi kjørt månedlig valutajustering.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(8).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(7).toYearMonth(),
                    tom = LocalDate.now().minusMonths(7).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 900,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(6).toYearMonth(),
                    tom = LocalDate.now().minusMonths(6).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 800,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(5).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 700,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 200,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                // Barnet mottar det samme beløpet fra det andre landet i hele perioden.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn.aktør), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Fra og med 7 måneder siden ble kursen endret hver måned.
                lagValutakurs(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(8).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(7), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(7).toYearMonth(), tom = LocalDate.now().minusMonths(7).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(7), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(6).toYearMonth(), tom = LocalDate.now().minusMonths(6).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(6), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(5).toYearMonth(), tom = LocalDate.now().minusMonths(5).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(5), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(4), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(3), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(2), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(1), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        // Act
        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
                personerIBehandling = listOf(søker, barn),
            )

        // Assert
        // Skal inneholde de siste 8 månedene.
        assertThat(utbetalingerPerMndEøs.size).isEqualTo(8)
        assertThat(utbetalingerPerMndEøs.keys).isEqualTo(setAvMånedÅrMediumForPeriode(7, 0))

        // Hver mnd skal inneholde 1 utbetaling for barn og 2 utbetalinger for søker per måned.
        assertThat(utbetalingerPerMndEøs.all { it.value.utbetalinger.size == 3 }).isTrue
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal gi bare utbetalingsinfo for relevante andeler som ikke er satt til 0 pga endret utbetaling`() {
        // Arrange
        val barn = lagPerson()

        val endringstidspunkt = LocalDate.now().minusMonths(7)

        val andelerTilkjentYtelse =
            listOf(
                // Barn har barnetrygd de siste 12 månedene, og fra og med 7 måneder siden har vi kjørt månedlig valutajustering. Endretutbetaling andel som reduserer utbetaling til 0 finnes på de 4 siste månedene.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(8).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(7).toYearMonth(),
                    tom = LocalDate.now().minusMonths(7).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 900,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(6).toYearMonth(),
                    tom = LocalDate.now().minusMonths(6).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 800,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(5).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 700,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 200,
                ),
            )

        val endretUtbetalingAndeler =
            listOf(
                lagEndretUtbetalingAndel(personer = setOf(barn), fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().toYearMonth(), årsak = Årsak.ENDRE_MOTTAKER, prosent = BigDecimal(0)),
            )

        val utenlandskePeriodebeløp =
            listOf(
                // Barnet mottar det samme beløpet fra det andre landet i hele perioden.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn.aktør), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Fra og med 7 måneder siden ble kursen endret hver måned.
                lagValutakurs(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(8).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(7), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(7).toYearMonth(), tom = LocalDate.now().minusMonths(7).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(7), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(6).toYearMonth(), tom = LocalDate.now().minusMonths(6).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(6), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(5).toYearMonth(), tom = LocalDate.now().minusMonths(5).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(5), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(4), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(3), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(2), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now().minusMonths(1), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        // Act
        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = endretUtbetalingAndeler,
                personerIBehandling = listOf(barn),
            )

        // Assert
        // Skal inneholde de de 3 første månedene.
        assertThat(utbetalingerPerMndEøs.size).isEqualTo(3)
        assertThat(utbetalingerPerMndEøs.keys).isEqualTo(setAvMånedÅrMediumForPeriode(7, 5))

        // Hver mnd skal inneholde 1 utbetaling for barn per måned.
        assertThat(utbetalingerPerMndEøs.all { it.value.utbetalinger.size == 1 }).isTrue
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal gi utbetalingsinfo for alle måneder etter endringstidspunktet for ett primærlandsbarn og ett sekundærlandsbarn`() {
        // Arrange
        val sekundærlandsbarnPerson = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2018, 3, 10))
        val primærlandsbarnPerson = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 20))
        val sekundærlandsbarn = sekundærlandsbarnPerson.aktør
        val primærlandsbarn = primærlandsbarnPerson.aktør

        val endringstidspunkt = LocalDate.now().minusMonths(4)

        val andelerTilkjentYtelse =
            listOf(
                // Sekundærlandsbarn har barnetrygd de siste 12 månedene, og fra og med 5 måneder siden har vi kjørt månedlig valutajustering.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 200,
                ),
                // Primærlandsbarn har barntrygd de siste 12 månedene
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = primærlandsbarn,
                    kalkulertUtbetalingsbeløp = 1054,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                // Sekundærlandsbarnet mottar to forskjellige beløp fra det andre landet i perioden.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn), beløp = BigDecimal.valueOf(550), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(550)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Fra og med 5 måneder siden ble kursen endret hver måned.
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(4), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(3), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(2), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(1), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        // Act
        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
                personerIBehandling = listOf(sekundærlandsbarnPerson, primærlandsbarnPerson),
            )

        // Assert
        // Skal inneholde de siste 8 månedene.
        assertThat(utbetalingerPerMndEøs.size).isEqualTo(5)
        assertThat(utbetalingerPerMndEøs.keys).isEqualTo(setAvMånedÅrMediumForPeriode(4, 0))

        // Hver måned skal inneholde 1 utbetaling for sekundærlandsbarn og 1 utbetaling for primærlandsbarn.
        assertThat(utbetalingerPerMndEøs.all { it.value.utbetalinger.size == 2 }).isTrue
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal gi utbetalingsinfo for alle måneder etter endringstidspunktet for to sekundærlandsbarn hvor det ene barnet mottar barnetrygd fra midt i endringsperioden`() {
        // Arrange
        val sekundærlandsbarnPerson1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2017, 5, 1))
        val sekundærlandsbarnPerson2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 11, 25))
        val sekundærlandsbarn = sekundærlandsbarnPerson1.aktør
        val sekundærlandsbarn2 = sekundærlandsbarnPerson2.aktør

        val endringstidspunkt = LocalDate.now().minusMonths(4)

        val andelerTilkjentYtelse =
            listOf(
                // Sekundærlandsbarn har barnetrygd de siste 12 månedene, og fra og med 5 måneder siden har vi kjørt månedlig valutajustering.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 200,
                ),
                // Det andre sekundærlandsbarnet har barntrygd de siste 3 månedene
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = sekundærlandsbarn2,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = sekundærlandsbarn2,
                    kalkulertUtbetalingsbeløp = 900,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = sekundærlandsbarn2,
                    kalkulertUtbetalingsbeløp = 800,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                // Sekundærlandsbarnet mottar to forskjellige beløp fra det andre landet i perioden.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
                // Sekundærlandsbarn 1 og 2 mottar det samme beløpet fra det andre landet
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn, sekundærlandsbarn2), beløp = BigDecimal.valueOf(550), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(550)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Fra og med 5 måneder siden ble kursen endret hver måned.
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(4), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn), valutakursdato = LocalDate.now().minusMonths(3), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(sekundærlandsbarn, sekundærlandsbarn2), valutakursdato = LocalDate.now().minusMonths(2), valutakode = "SEK", kurs = BigDecimal.valueOf(1.3)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(sekundærlandsbarn, sekundærlandsbarn2), valutakursdato = LocalDate.now().minusMonths(1), valutakode = "SEK", kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn, sekundærlandsbarn2), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        // Act
        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
                personerIBehandling = listOf(sekundærlandsbarnPerson1, sekundærlandsbarnPerson2),
            )

        // Assert
        // Skal inneholde de siste 8 månedene.
        assertThat(utbetalingerPerMndEøs.size).isEqualTo(5)
        assertThat(utbetalingerPerMndEøs.keys).isEqualTo(setAvMånedÅrMediumForPeriode(fraAntallMndSiden = 4, tilAndtalMndSiden = 0))

        // De første 2 månedene skal kun inneholde utbetaling for det første sekundærlandsbarnet.
        assertThat(utbetalingerPerMndEøs.filterKeys { setAvMånedÅrMediumForPeriode(fraAntallMndSiden = 4, tilAndtalMndSiden = 3).contains(it) }.values.all { it.utbetalinger.size == 1 }).isTrue

        // De siste 3 månedene skal inneholde utbetalinger for begge sekundærlandsbarna.
        assertThat(utbetalingerPerMndEøs.filterKeys { setAvMånedÅrMediumForPeriode(fraAntallMndSiden = 2, tilAndtalMndSiden = 0).contains(it) }.values.all { it.utbetalinger.size == 2 }).isTrue
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal kaste feil dersom utbetalt fra annet land, valutakode eller valutakurs er null`() {
        // Arrange
        val sekundærlandsbarnPerson = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2016, 2, 14))
        val sekundærlandsbarn = sekundærlandsbarnPerson.aktør

        val endringstidspunkt = LocalDate.now().minusMonths(4)

        val andelerTilkjentYtelse =
            listOf(
                // Sekundærlandsbarn har barnetrygd de siste 12 månedene, og fra og med 5 måneder siden har vi kjørt månedlig valutajustering.
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(12).toYearMonth(),
                    tom = LocalDate.now().minusMonths(5).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(4).toYearMonth(),
                    tom = LocalDate.now().minusMonths(4).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 600,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(3).toYearMonth(),
                    tom = LocalDate.now().minusMonths(3).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 500,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(2).toYearMonth(),
                    tom = LocalDate.now().minusMonths(2).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 400,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().minusMonths(1).toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 300,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = sekundærlandsbarn,
                    kalkulertUtbetalingsbeløp = 200,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                // Sekundærlandsbarnet har to perioder med utenlandsk periodebeløp men disse mangler beløp, intervall og valutakode.
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(12).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
            )

        val valutakurser =
            listOf(
                // Barnets valutakurser de siste månedene. Mangler kurs.
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(sekundærlandsbarn)),
            )

        // Act & Assert
        assertThrows<Feil> {
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
                personerIBehandling = listOf(sekundærlandsbarnPerson),
            )
        }
    }

    @Test
    fun `skalHenteUtbetalingerEøs - skal returne true når det finnes valutakurser etter endringstidspunktet`() {
        // Arrange
        val endringstidspunkt = LocalDate.now().minusMonths(2)
        val barn = randomAktør()

        val valutakurser =
            listOf(
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.3)),
            )

        // Act & Assert
        assertThat(skalHenteUtbetalingerEøs(endringstidspunkt = endringstidspunkt, valutakurser = valutakurser)).isTrue
    }

    @Test
    fun `skalHenteUtbetalingerEøs - skal returne false når det ikke finnes valutakurser etter endringstidspunktet`() {
        // Arrange
        val endringstidspunkt = LocalDate.now().minusMonths(2)
        val barn = randomAktør()

        val valutakurser =
            listOf(
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
            )

        // Act & Assert
        assertThat(skalHenteUtbetalingerEøs(endringstidspunkt = endringstidspunkt, valutakurser = valutakurser)).isFalse
    }

    @Test
    fun `skalHenteUtbetalingerEøs - skal returnere false uavhengig av valutakurs dersom endringstidspunktet er satt til tidenes ende`() {
        // Arrange
        val endringstidspunkt = TIDENES_ENDE
        val barn = randomAktør()

        val valutakurser =
            listOf(
                lagValutakurs(fom = LocalDate.now().minusMonths(4).toYearMonth(), tom = LocalDate.now().minusMonths(4).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(3).toYearMonth(), tom = LocalDate.now().minusMonths(3).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().minusMonths(2).toYearMonth(), tom = LocalDate.now().minusMonths(2).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.1)),
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().minusMonths(1).toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.2)),
                lagValutakurs(fom = LocalDate.now().toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn), kurs = BigDecimal.valueOf(1.3)),
            )

        // Act & Assert
        assertThat(skalHenteUtbetalingerEøs(endringstidspunkt = endringstidspunkt, valutakurser = valutakurser)).isFalse
    }

    @Test
    fun `hentLandOgStartdatoForUtbetalingstabell - skal finne alle utenlandsperiode beløp som gjelder etter endringstidspunktet`() {
        // Arrange
        val endringstidspunkt = YearMonth.now()

        val utenlandskPeriodebeløp =
            listOf(
                lagUtenlandskPeriodebeløp(
                    fom = YearMonth.now().minusMonths(5),
                    tom = YearMonth.now().minusMonths(3),
                    utbetalingsland = "NL",
                    valutakode = "EUR",
                    beløp = BigDecimal.valueOf(1000),
                    intervall = Intervall.MÅNEDLIG,
                    barnAktører =
                        setOf(
                            randomAktør(),
                        ),
                ),
                lagUtenlandskPeriodebeløp(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(2),
                    utbetalingsland = "SE",
                    valutakode = "SEK",
                    beløp = BigDecimal.valueOf(1000),
                    intervall = Intervall.MÅNEDLIG,
                    barnAktører =
                        setOf(
                            randomAktør(),
                        ),
                ),
                lagUtenlandskPeriodebeløp(
                    fom = YearMonth.now().plusMonths(3),
                    utbetalingsland = "DK",
                    valutakode = "DKK",
                    beløp = BigDecimal.valueOf(1000),
                    intervall = Intervall.MÅNEDLIG,
                    barnAktører =
                        setOf(
                            randomAktør(),
                        ),
                ),
            )

        val landkoder =
            mapOf(
                "SE" to "Sverige",
                "DK" to "Danmark",
            )

        // Act
        val utbetalingstabellAutomatiskValutajustering = hentLandOgStartdatoForUtbetalingstabell(endringstidspunkt = endringstidspunkt, landkoder = landkoder, utenlandskPeriodebeløp = utenlandskPeriodebeløp)

        // Assert
        assertThat(utbetalingstabellAutomatiskValutajustering).isNotNull
        assertThat(utbetalingstabellAutomatiskValutajustering.utbetalingerEosLand?.first()).isEqualTo("Sverige og Danmark")
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal vise fødselsdato for barn`() {
        // Arrange
        val barnFødselsdato = LocalDate.of(2020, 6, 15)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val endringstidspunkt = LocalDate.now().minusMonths(1)

        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = barn.aktør,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn.aktør), beløp = BigDecimal.valueOf(500), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(500)),
            )
        val valutakurser =
            listOf(
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn.aktør), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        // Act
        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
                personerIBehandling = listOf(barn),
            )

        // Assert
        assertThat(utbetalingerPerMndEøs).isNotEmpty
        val førsteMåned = utbetalingerPerMndEøs.values.first()
        assertThat(førsteMåned.utbetalinger).hasSize(1)
        assertThat(førsteMåned.utbetalinger.first().fødselsdato).isEqualTo(barnFødselsdato.tilKortString())
    }

    @Test
    fun `hentUtbetalingerPerMndEøs - Skal vise riktig fødselsdato for flere barn`() {
        // Arrange
        val barn1Fødselsdato = LocalDate.of(2018, 3, 10)
        val barn2Fødselsdato = LocalDate.of(2021, 12, 25)
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = barn1Fødselsdato)
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = barn2Fødselsdato)

        val endringstidspunkt = LocalDate.now().minusMonths(1)

        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = barn1.aktør,
                    kalkulertUtbetalingsbeløp = 1000,
                ),
                lagAndelTilkjentYtelse(
                    fom = LocalDate.now().minusMonths(1).toYearMonth(),
                    tom = LocalDate.now().toYearMonth(),
                    aktør = barn2.aktør,
                    kalkulertUtbetalingsbeløp = 800,
                ),
            )
        val utenlandskePeriodebeløp =
            listOf(
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn1.aktør), beløp = BigDecimal.valueOf(300), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(300)),
                UtenlandskPeriodebeløp(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn2.aktør), beløp = BigDecimal.valueOf(300), valutakode = "SEK", intervall = Intervall.MÅNEDLIG, kalkulertMånedligBeløp = BigDecimal.valueOf(300)),
            )
        val valutakurser =
            listOf(
                lagValutakurs(fom = LocalDate.now().minusMonths(1).toYearMonth(), tom = LocalDate.now().toYearMonth(), barnAktører = setOf(barn1.aktør, barn2.aktør), valutakursdato = LocalDate.now(), valutakode = "SEK", kurs = BigDecimal.valueOf(1)),
            )

        // Act
        val utbetalingerPerMndEøs =
            hentUtbetalingerPerMndEøs(
                endringstidspunkt = endringstidspunkt,
                andelTilkjentYtelserForBehandling = andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
                endretutbetalingAndeler = emptyList(),
                personerIBehandling = listOf(barn1, barn2),
            )

        // Assert
        assertThat(utbetalingerPerMndEøs).isNotEmpty
        val førsteMåned = utbetalingerPerMndEøs.values.first()
        assertThat(førsteMåned.utbetalinger).hasSize(2)

        val fødselsdatoer = førsteMåned.utbetalinger.map { it.fødselsdato }.toSet()
        assertThat(fødselsdatoer).containsExactlyInAnyOrder(
            barn1Fødselsdato.tilKortString(),
            barn2Fødselsdato.tilKortString(),
        )
    }
}

private fun setAvMånedÅrMediumForPeriode(
    fraAntallMndSiden: Long,
    tilAndtalMndSiden: Long,
): Set<String> =
    LocalDate
        .now()
        .minusMonths(fraAntallMndSiden)
        .toYearMonth()
        .rangeTo(LocalDate.now().minusMonths(tilAndtalMndSiden).toYearMonth())
        .map { it.tilMånedÅrMedium() }
        .toSet()
