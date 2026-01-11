package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SatsendringUtilTest {
    private val ugyldigSats = 1000

    private val søker = tilfeldigPerson(personType = PersonType.SØKER)
    private val personopplysningGrunnlag = lagPersonopplysningsgrunnlag(mutableSetOf(søker, person4År, person15År), 1)

    @ParameterizedTest()
    @MethodSource("listeMedBarn")
    fun `Skal returnere true dersom vi har siste sats for barn over og under 6 år`(barn: Person) {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.ORBA)
        val andelerMedSisteSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp,
                    ytelseType = SatsType.ORBA.tilYtelseType(),
                    person = barn,
                ),
            )

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere true dersom vi har siste sats for Småbarnstillegg`() {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.SMA)
        val andelerMedSisteSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    person = personopplysningGrunnlag.søker,
                ),
            )

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere true dersom vi har siste sats for Utvidet barnetrygd`() {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD)
        val andelerMedSisteSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = personopplysningGrunnlag.søker,
                ),
            )

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere false når andelen overlapper siste dag for TILLEGSORBA og satsendring for ORBA`() {
        // Arrange
        val andelerMedSisteSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2023, 7),
                    tom = YearMonth.of(2030, 1),
                    sats = 1766,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = person4År,
                ),
            )
        // Act & Assert
        assertFalse(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere true når det er en kombinasjon av andeler av forskjellige typer og siste sats er oppdatert`() {
        val barn1 = tilfeldigPerson(fødselsdato = LocalDate.of(2018, 2, 6))
        val barn2 = tilfeldigPerson(fødselsdato = LocalDate.of(2025, 3, 29))
        val personopplysningGrunnlag = lagPersonopplysningsgrunnlag(mutableSetOf(søker, barn1, barn2), 1)
        // Arrange
        val andelerMedSisteSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2026, 1),
                    sats = 2516,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = søker,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2026, 2),
                    tom = YearMonth.of(2030, 1),
                    sats = 2572,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = søker,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2024, 2),
                    tom = YearMonth.of(2024, 8),
                    sats = 1510,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn1,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2024, 9),
                    tom = YearMonth.of(2025, 4),
                    sats = 1766,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn1,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2025, 5),
                    tom = YearMonth.of(2026, 1),
                    sats = 1968,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn1,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2026, 2),
                    tom = YearMonth.of(2030, 1),
                    sats = 2012,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn2,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2025, 4),
                    tom = YearMonth.of(2025, 4),
                    sats = 1766,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn2,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2025, 5),
                    tom = YearMonth.of(2026, 1),
                    sats = 1968,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn2,
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(2026, 2),
                    tom = YearMonth.of(2035, 1),
                    sats = 2012,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn2,
                ),
            )
        // Act & Assert
        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @ParameterizedTest()
    @MethodSource("listeMedBarn")
    fun `Skal returnere true dersom vi har siste sats for barn over og under 6 år selv om alle perioder er fram i tid`(barn: Person) {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.ORBA)
        val andelerMedSisteSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    tom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    sats = sisteSats.beløp,
                    ytelseType = sisteSats.type.tilYtelseType(),
                    person = barn,
                ),
            )

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere true dersom vi har siste sats for utvidet selv om alle perioder er fram i tid`() {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD)
        val andelerMedSisteSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    tom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    sats = sisteSats.beløp,
                    ytelseType = sisteSats.type.tilYtelseType(),
                    person = personopplysningGrunnlag.søker,
                ),
            )

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere true dersom vi har siste sats for småbarnstilleg selv om alle perioder er fram i tid`() {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.SMA)
        val andelerMedSisteSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    tom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                    sats = sisteSats.beløp,
                    ytelseType = sisteSats.type.tilYtelseType(),
                    person = personopplysningGrunnlag.søker,
                ),
            )

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @ParameterizedTest()
    @MethodSource("listeMedBarn")
    fun `Skal returnere false dersom vi ikke har siste sats for barn over og under 6 år`(barn: Person) {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.ORBA)
        val andelerMedFeilSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp - 1,
                    ytelseType = sisteSats.type.tilYtelseType(),
                    person = barn,
                ),
            )

        assertFalse(andelerMedFeilSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere false dersom vi ikke har siste sats for småbarnstillegg`() {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.SMA)
        val andelerMedFeilSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp - 1,
                    ytelseType = sisteSats.type.tilYtelseType(),
                    person = personopplysningGrunnlag.søker,
                ),
            )

        assertFalse(andelerMedFeilSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere false dersom vi ikke har siste sats for utvidet`() {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD)
        val andelerMedFeilSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp - 1,
                    ytelseType = sisteSats.type.tilYtelseType(),
                    person = personopplysningGrunnlag.søker,
                ),
            )

        assertFalse(andelerMedFeilSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal ignorere andeler som kommer før siste sats`() {
        SatsType.entries
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerSomErFørSisteSats =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = sisteSats.gyldigFom.toYearMonth().minusMonths(100),
                            tom = sisteSats.gyldigFom.toYearMonth().minusMonths(1),
                            sats = sisteSats.beløp - 1,
                            ytelseType = it.tilYtelseType(),
                            person = if (it == SatsType.TILLEGG_ORBA) person4År else person15År,
                        ),
                    )

                assertTrue(andelerSomErFørSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
            }
    }

    @ParameterizedTest()
    @MethodSource("listeMedBarn")
    fun `Skal returnere true dersom vi ikke har siste sats, men de er redusert til 0 prosent`(person: Person) {
        val sisteSats = SatsService.finnSisteSatsFor(SatsType.ORBA)
        val andelerMedFeilSats =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = sisteSats.gyldigFom.toYearMonth(),
                    tom = sisteSats.gyldigTom.toYearMonth(),
                    sats = sisteSats.beløp - 1,
                    prosent = BigDecimal.ZERO,
                    ytelseType = sisteSats.type.tilYtelseType(),
                    person = person,
                ),
            )

        assertTrue(andelerMedFeilSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true på ytelse med rett sats når tom dato er på samme dato som satstidspunkt`() {
        val behandling = lagBehandling()
        val atySomGårUtPåSatstidspunktGyldig =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA).minusMonths(1),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = person15År,
                aktør = person15År.aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
            )

        assertThat(listOf(atySomGårUtPåSatstidspunktGyldig).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)

        val atySomGårUtPåSatstidspunktUgyldig =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA).minusMonths(1),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = person15År,
                aktør = person15År.aktør,
                periodeIdOffset = 1,
                beløp = ugyldigSats,
            )

        assertThat(listOf(atySomGårUtPåSatstidspunktUgyldig).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(false)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true hvis ingen aktive andel tilkjent ytelser`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA).minusMonths(10),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA).minusMonths(1),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = person15År,
                aktør = person15År.aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
            )

        assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true for ny sats når fom er på satstidspunktet`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA).plusYears(10),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = person15År,
                aktør = person15År.aktør,
                periodeIdOffset = 1,
                beløp = SatsService.finnSisteSatsFor(SatsType.ORBA).beløp,
            )

        assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false for gammel sats når fom er på satstidspunktet`() {
        val behandling = lagBehandling()
        val utgåttAndelTilkjentYtelse =
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = datoForSisteSatsendringForSatsType(SatsType.ORBA),
                tom = datoForSisteSatsendringForSatsType(SatsType.ORBA).plusYears(10),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                behandling = behandling,
                person = person15År,
                aktør = person15År.aktør,
                periodeIdOffset = 1,
                beløp = ugyldigSats,
            )

        assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(false)
    }

    @Test
    fun `Hvis denne testen feiler så er det skjedd en endring på aktive satser og testene som sjekker om en sats har eller ikke har oppdatert sats må utvides`() {
        assertThat(SatsService.finnAlleAktiveSisteSatser().map { it.type }).hasSize(5).containsOnly(SatsType.SMA, SatsType.UTVIDET_BARNETRYGD, SatsType.ORBA, SatsType.FINNMARKSTILLEGG, SatsType.SVALBARDTILLEGG)
        assertThat(SatsType.entries).hasSize(7).containsOnly(SatsType.SMA, SatsType.UTVIDET_BARNETRYGD, SatsType.ORBA, SatsType.TILLEGG_ORBA, SatsType.FINN_SVAL, SatsType.FINNMARKSTILLEGG, SatsType.SVALBARDTILLEGG)
    }

    private fun datoForSisteSatsendringForSatsType(satsType: SatsType) = SatsService.finnSisteSatsFor(satsType).gyldigFom.toYearMonth()

    private fun lagPersonopplysningsgrunnlag(
        personer: MutableSet<Person>,
        behandlingId: Long,
    ): PersonopplysningGrunnlag =
        PersonopplysningGrunnlag(
            personer = personer,
            behandlingId = behandlingId,
        )

    companion object {
        private val person4År = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(4))
        private val person15År = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(15))

        @JvmStatic
        private fun listeMedBarn(): List<Person> = listOf(person4År, person15År)
    }
}
