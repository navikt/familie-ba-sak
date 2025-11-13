package no.nav.familie.ba.sak.kjerne.endretutbetaling.domene

import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.endretutbetaling.beregnGyldigTom
import no.nav.familie.ba.sak.kjerne.endretutbetaling.beregnGyldigTomPerAktør
import no.nav.familie.ba.sak.kjerne.endretutbetaling.skalSplitteEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.splittEndretUbetalingAndel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class EndretUtbetalingAndelTest {
    @Test
    fun `Sjekk validering med tomme felt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)
        endretUtbetalingAndel.begrunnelse = ""

        assertThrows<RuntimeException> {
            endretUtbetalingAndel.validerUtfyltEndring()
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med tomt felt avtaletidpunkt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.personer = mutableSetOf(tilfeldigPerson())
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.begrunnelse = "begrunnelse"

        assertThrows<RuntimeException> {
            endretUtbetalingAndel.validerUtfyltEndring()
        }
    }

    @Test
    fun `Sjekk validering for delt bosted med ikke tomt felt avtaletidpunkt`() {
        val behandling = lagBehandling()
        val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = behandling.id)

        endretUtbetalingAndel.personer = mutableSetOf(tilfeldigPerson())
        endretUtbetalingAndel.prosent = BigDecimal(0)
        endretUtbetalingAndel.fom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.tom = YearMonth.of(2020, 10)
        endretUtbetalingAndel.årsak = Årsak.DELT_BOSTED
        endretUtbetalingAndel.søknadstidspunkt = LocalDate.now()
        endretUtbetalingAndel.avtaletidspunktDeltBosted = LocalDate.now()
        endretUtbetalingAndel.begrunnelse = "begrunnelse"

        assertTrue(endretUtbetalingAndel.validerUtfyltEndring())
    }

    @Test
    fun `Skal sette tom til siste måned med andel tilkjent ytelse hvis tom er null og det ikke finnes noen andre endringsperioder`() {
        val behandling = lagBehandling()
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(barn1, barn2),
                fom = YearMonth.now(),
                tom = null,
                årsak = Årsak.DELT_BOSTED,
            )

        val sisteTomPåAndelerBarn1 = YearMonth.now().plusMonths(10)
        val sisteTomPåAndelerBarn2 = YearMonth.now().plusMonths(9)
        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().plusMonths(5),
                    tom = sisteTomPåAndelerBarn1,
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().plusMonths(5),
                    tom = sisteTomPåAndelerBarn2,
                ),
            )

        val nyTom =
            beregnGyldigTom(
                andelTilkjentYtelser = andelTilkjentYtelser,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andreEndredeAndelerPåBehandling = emptyList(),
            )

        val forventetTom = minOf(sisteTomPåAndelerBarn1, sisteTomPåAndelerBarn2)
        assertEquals(forventetTom, nyTom)
    }

    @Test
    fun `Skal sette tom til måneden før neste endringsperiode`() {
        val behandling = lagBehandling()
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(barn1, barn2),
                fom = YearMonth.now(),
                tom = null,
                årsak = Årsak.DELT_BOSTED,
            )

        val fomPåEndretAndelBarn1 = YearMonth.now().plusMonths(5)
        val fomPåEndretAndelBarn2 = YearMonth.now().plusMonths(6)
        val andreEndretAndeler =
            listOf(
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    personer = setOf(barn1),
                    fom = fomPåEndretAndelBarn1,
                    tom = YearMonth.now().plusMonths(8),
                    årsak = Årsak.DELT_BOSTED,
                ),
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    personer = setOf(barn2),
                    fom = fomPåEndretAndelBarn2,
                    tom = YearMonth.now().plusMonths(8),
                    årsak = Årsak.DELT_BOSTED,
                ),
            )

        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.now().plusMonths(5),
                    tom = YearMonth.now().plusMonths(10),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now().plusMonths(4),
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.now().plusMonths(5),
                    tom = YearMonth.now().plusMonths(9),
                ),
            )

        val nyTom =
            beregnGyldigTom(
                andelTilkjentYtelser = andelTilkjentYtelser,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andreEndredeAndelerPåBehandling = andreEndretAndeler,
            )

        val forventetTom = andreEndretAndeler.minOf { it.fom!! }.minusMonths(1)
        assertEquals(forventetTom, nyTom)
    }

    @Test
    fun `Skal sette tom til siste måned med andel tilkjent ytelse per aktør hvis tom er null og det ikke finnes noen andre endringsperioder`() {
        val behandling = lagBehandling()
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val nyEndretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(barn1, barn2),
                fom = YearMonth.of(2025, 6),
                tom = null,
                årsak = Årsak.ALLEREDE_UTBETALT,
            )

        val eksisterendeEndretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                personer = setOf(barn1),
                fom = YearMonth.of(2025, 8),
                tom = YearMonth.of(2025, 10),
                årsak = Årsak.ENDRE_MOTTAKER,
            )

        val sisteTomPåAndelerBarn1 = YearMonth.of(2025, 11)
        val sisteTomPåAndelerBarn2 = YearMonth.of(2025, 12)
        val andelTilkjentYtelser =
            listOf(
                lagAndelTilkjentYtelse(
                    person = barn1,
                    fom = YearMonth.of(2025, 1),
                    tom = sisteTomPåAndelerBarn1,
                ),
                lagAndelTilkjentYtelse(
                    person = barn2,
                    fom = YearMonth.of(2025, 1),
                    tom = sisteTomPåAndelerBarn2,
                ),
            )

        val faktiskTomPerAktør =
            beregnGyldigTomPerAktør(
                endretUtbetalingAndel = nyEndretUtbetalingAndel,
                andelTilkjentYtelser = andelTilkjentYtelser,
                andreEndredeAndelerPåBehandling = listOf(eksisterendeEndretUtbetalingAndel),
            )

        val forventetTomPerAktør =
            mapOf(
                barn1.aktør to eksisterendeEndretUtbetalingAndel.fom?.minusMonths(1),
                barn2.aktør to sisteTomPåAndelerBarn2,
            )
        assertEquals(forventetTomPerAktør, faktiskTomPerAktør)
    }

    @Nested
    inner class SkalSplitteEndretUtbetalingAndel {
        private val endretUtbetalingAndel = EndretUtbetalingAndel(behandlingId = 0)

        @Test
        fun `skal returnere false hvis tom ikke er null`() {
            val endretUtbetalingAndel = endretUtbetalingAndel.copy(tom = YearMonth.of(2025, 12))

            assertThat(
                skalSplitteEndretUtbetalingAndel(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    gyldigTomDatoPerAktør = mockk(),
                ),
            ).isFalse()
        }

        @Test
        fun `skal returnere false hvis begge personer har samme gyldigTomDato`() {
            val person1 = tilfeldigPerson()
            val person2 = tilfeldigPerson()

            val gyldigTomEtterDagensDatoPerAktør =
                mapOf(
                    person1.aktør to YearMonth.of(2025, 6),
                    person2.aktør to YearMonth.of(2025, 6),
                )

            assertThat(
                skalSplitteEndretUtbetalingAndel(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    gyldigTomDatoPerAktør = gyldigTomEtterDagensDatoPerAktør,
                ),
            ).isFalse()
        }

        @Test
        fun `skal returnere true hvis tom-dato er null og gyldigTomDato inneholder flere datoer`() {
            val person1 = tilfeldigPerson()
            val person2 = tilfeldigPerson()

            val gyldigTomEtterDagensDatoPerAktør =
                mapOf(
                    person1.aktør to YearMonth.of(2025, 6),
                    person2.aktør to YearMonth.of(2025, 7),
                )

            assertThat(
                skalSplitteEndretUtbetalingAndel(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    gyldigTomDatoPerAktør = gyldigTomEtterDagensDatoPerAktør,
                ),
            ).isTrue()
        }
    }

    @Nested
    inner class SplittEndretUbetalingAndel {
        @Test
        fun `skal splitte andel med gyldig tom per aktør`() {
            val person1 = tilfeldigPerson()
            val person2 = tilfeldigPerson()
            val person3 = tilfeldigPerson()

            val endretUtbetalingAndel =
                EndretUtbetalingAndel(
                    id = 1,
                    behandlingId = 0,
                    personer = mutableSetOf(person1, person2, person3),
                    prosent = BigDecimal.ZERO,
                    fom = YearMonth.of(2025, 1),
                    tom = null,
                    årsak = Årsak.ENDRE_MOTTAKER,
                    avtaletidspunktDeltBosted = null,
                    søknadstidspunkt = LocalDate.of(2025, 1, 1),
                    begrunnelse = "Begrunnelse",
                )

            val gyldigTomEtterDagensDatoPerAktør =
                mapOf(
                    person1.aktør to YearMonth.of(2025, 10),
                    person2.aktør to YearMonth.of(2025, 12),
                    person3.aktør to YearMonth.of(2025, 12),
                )

            val splittedeAndeler =
                splittEndretUbetalingAndel(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    gyldigTomEtterDagensDatoPerAktør = gyldigTomEtterDagensDatoPerAktør,
                )

            assertThat(splittedeAndeler).hasSize(2)

            val (førsteAndel, andreAndel) = splittedeAndeler

            assertThat(førsteAndel)
                .usingRecursiveComparison()
                .ignoringFields("id", "fom", "tom", "personer", "opprettetTidspunkt", "endretTidspunkt")
                .isEqualTo(endretUtbetalingAndel)

            assertThat(førsteAndel.id).isEqualTo(0)
            assertThat(førsteAndel.fom).isEqualTo(YearMonth.of(2025, 1))
            assertThat(førsteAndel.tom).isEqualTo(YearMonth.of(2025, 10))
            assertThat(førsteAndel.personer).containsExactlyInAnyOrder(person1, person2, person3)

            assertThat(andreAndel)
                .usingRecursiveComparison()
                .ignoringFields("id", "fom", "tom", "personer", "opprettetTidspunkt", "endretTidspunkt")
                .isEqualTo(endretUtbetalingAndel)

            assertThat(andreAndel.id).isEqualTo(0)
            assertThat(andreAndel.fom).isEqualTo(YearMonth.of(2025, 11))
            assertThat(andreAndel.tom).isEqualTo(YearMonth.of(2025, 12))
            assertThat(andreAndel.personer).containsExactlyInAnyOrder(person2, person3)
        }
    }
}
