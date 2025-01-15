package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SatsendringUtilTest {
    private val ugyldigSats = 1000

    private val person4År = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(4))
    private val person15År = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(15))
    private val personopplysningGrunnlag = lagPersonopplysningsgrunnlag(mutableSetOf(person4År, person15År), 1)

    @Test
    fun `Skal returnere true dersom vi har siste sats`() {
        val andelerMedSisteSats =
            SatsType.entries
                .filter { it != SatsType.FINN_SVAL }
                .map {
                    val sisteSats = SatsService.finnSisteSatsFor(it)
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth(),
                        tom = sisteSats.gyldigTom.toYearMonth(),
                        sats = sisteSats.beløp,
                        ytelseType = it.tilYtelseType(),
                        person = if (it == SatsType.TILLEGG_ORBA) person4År else person15År,
                    )
                }

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere true dersom vi har siste sats selv om alle perioder er fram i tid`() {
        val andelerMedSisteSats =
            SatsType.entries
                .filter { it != SatsType.FINN_SVAL }
                .map {
                    val sisteSats = SatsService.finnSisteSatsFor(it)
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        fom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                        tom = sisteSats.gyldigFom.toYearMonth().plusYears(1),
                        sats = sisteSats.beløp,
                        ytelseType = it.tilYtelseType(),
                        person = if (it == SatsType.TILLEGG_ORBA) person4År else person15År,
                    )
                }

        assertTrue(andelerMedSisteSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
    }

    @Test
    fun `Skal returnere false dersom vi ikke har siste sats`() {
        SatsType.entries
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerMedFeilSats =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = sisteSats.gyldigFom.toYearMonth(),
                            tom = sisteSats.gyldigTom.toYearMonth(),
                            sats = sisteSats.beløp - 1,
                            ytelseType = it.tilYtelseType(),
                            person = if (it == SatsType.TILLEGG_ORBA) person4År else person15År,
                        ),
                    )

                assertFalse(andelerMedFeilSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
            }
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

    @Test
    fun `Skal ikke returnere false dersom vi ikke har siste sats, men de er redusert til 0 prosent`() {
        SatsType.entries
            .filter { it != SatsType.FINN_SVAL }
            .forEach {
                val sisteSats = SatsService.finnSisteSatsFor(it)
                val andelerMedFeilSats =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = sisteSats.gyldigFom.toYearMonth(),
                            tom = sisteSats.gyldigTom.toYearMonth(),
                            sats = sisteSats.beløp - 1,
                            prosent = BigDecimal.ZERO,
                            ytelseType = it.tilYtelseType(),
                            person = if (it == SatsType.TILLEGG_ORBA) person4År else person15År,
                        ),
                    )

                assertTrue(andelerMedFeilSats.erOppdatertMedSisteSatser(personopplysningGrunnlag))
            }
    }

    @Test
    fun `harAlleredeSatsendring skal returnere true hvis den har siste satsendring`() {
        val behandling = lagBehandling()
        val atyMedBareSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.SMA,
                behandling,
                YtelseType.SMÅBARNSTILLEGG,
                person = person4År,
            )

        Assertions.assertThat(atyMedBareSmåbarnstillegg.erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)

        val atyMedBareUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.UTVIDET_BARNETRYGD,
                behandling,
                YtelseType.UTVIDET_BARNETRYGD,
                person = person15År,
            )

        Assertions.assertThat(atyMedBareUtvidet.erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)

        val atyMedBareOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
                person = person15År,
            )

        Assertions.assertThat(atyMedBareOrba.erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)

        val atyMedBareTilleggOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.TILLEGG_ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
                person = person4År,
            )

        Assertions.assertThat(atyMedBareTilleggOrba.erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)

        Assertions
            .assertThat(
                (atyMedBareTilleggOrba + atyMedBareOrba + atyMedBareUtvidet + atyMedBareSmåbarnstillegg)
                    .erOppdatertMedSisteSatser(personopplysningGrunnlag),
            ).isEqualTo(true)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false hvis den har gammel satsendring`() {
        val behandling = lagBehandling()
        val atyMedUgyldigSatsSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.SMA,
                behandling,
                YtelseType.SMÅBARNSTILLEGG,
                ugyldigSats,
                person15År,
            )

        Assertions.assertThat(atyMedUgyldigSatsSmåbarnstillegg.erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(false)

        val atyMedUglydligSatsUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.UTVIDET_BARNETRYGD,
                behandling,
                YtelseType.UTVIDET_BARNETRYGD,
                ugyldigSats,
                person15År,
            )

        Assertions.assertThat(atyMedUglydligSatsUtvidet.erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(false)

        val atyMedUgyldigSatsBareOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
                ugyldigSats,
                person15År,
            )

        Assertions.assertThat(atyMedUgyldigSatsBareOrba.erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(false)

        val atyMedUgyldigSatsTilleggOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.TILLEGG_ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
                ugyldigSats,
                person4År,
            )

        Assertions.assertThat(atyMedUgyldigSatsTilleggOrba.erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(false)
    }

    @Test
    fun `harAlleredeSatsendring skal returnere false en av satsene ikke er ny`() {
        val behandling = lagBehandling()
        val atyMedUgyldigSatsSmåbarnstillegg =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.SMA,
                behandling,
                YtelseType.SMÅBARNSTILLEGG,
                ugyldigSats,
                person = person15År,
            )

        val atyMedGyldigUtvidet =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.UTVIDET_BARNETRYGD,
                behandling,
                YtelseType.UTVIDET_BARNETRYGD,
                person = person15År,
            )

        val atyMedBGyldigOrba =
            lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
                SatsType.ORBA,
                behandling,
                YtelseType.ORDINÆR_BARNETRYGD,
                person = person15År,
            )

        Assertions
            .assertThat(
                (atyMedBGyldigOrba + atyMedGyldigUtvidet + atyMedUgyldigSatsSmåbarnstillegg).erOppdatertMedSisteSatser(personopplysningGrunnlag),
            ).isEqualTo(false)
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

        Assertions.assertThat(listOf(atySomGårUtPåSatstidspunktGyldig).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)

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

        Assertions.assertThat(listOf(atySomGårUtPåSatstidspunktUgyldig).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(false)
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

        Assertions.assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)
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

        Assertions.assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(true)
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

        Assertions.assertThat(listOf(utgåttAndelTilkjentYtelse).erOppdatertMedSisteSatser(personopplysningGrunnlag)).isEqualTo(false)
    }

    private fun lagAndelTilkjentYtelseMedEndreteUtbetalingerIPeriodenRundtSisteSatsenring(
        satsType: SatsType,
        behandling: Behandling,
        ytelseType: YtelseType,
        beløp: Int? = null,
        person: Person,
    ) = listOf(
        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom =
                SatsService
                    .finnSisteSatsFor(satsType)
                    .gyldigFom
                    .minusMonths(1)
                    .toYearMonth(),
            tom =
                SatsService
                    .finnSisteSatsFor(satsType)
                    .gyldigFom
                    .plusMonths(1)
                    .toYearMonth(),
            ytelseType = ytelseType,
            behandling = behandling,
            person = person,
            aktør = person.aktør,
            periodeIdOffset = 1,
            beløp = beløp ?: SatsService.finnSisteSatsFor(satsType).beløp,
        ),
    )

    private fun datoForSisteSatsendringForSatsType(satsType: SatsType) = SatsService.finnSisteSatsFor(satsType).gyldigFom.toYearMonth()

    private fun lagPersonopplysningsgrunnlag(
        personer: MutableSet<Person>,
        behandlingId: Long,
    ): PersonopplysningGrunnlag =
        PersonopplysningGrunnlag(
            personer = personer,
            behandlingId = behandlingId,
        )
}
