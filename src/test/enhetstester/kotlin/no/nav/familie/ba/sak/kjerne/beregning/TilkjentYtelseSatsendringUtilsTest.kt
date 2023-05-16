package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseSatsendringUtils.lagAndelerMedNySatsForPersonOgYtelsetype
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class TilkjentYtelseSatsendringUtilsTest {

    @Test
    fun `skal oppdatere satsen på ordinære andeler for barna, og utvidet og småbarnstillegg for søker`() {
        val barnOver6 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2010, 1, 1))
        val barnUnder6 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 8, 1))
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(2000, 1, 1))
        val satsendringsbehandling = lagBehandling()

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 527,
                prosent = BigDecimal(50),
                person = barnOver6,
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1676,
                prosent = BigDecimal(100),
                person = barnUnder6,
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                beløp = 1054,
                prosent = BigDecimal(100),
                person = søker,
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2023, 1),
                tom = YearMonth.of(2023, 4),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                beløp = 660,
                prosent = BigDecimal(100),
                person = barnUnder6,
            ),
        )

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(satsendringsbehandling.id, søker, barnOver6, barnUnder6)

        val nyTilkjentYtelse = TilkjentYtelseSatsendringUtils.beregnTilkjentYtelseMedNySatsForSatsendring(
            andelerFraForrigeBehandling = forrigeAndeler,
            behandling = satsendringsbehandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
        )

        val nyeAndeler = nyTilkjentYtelse.andelerTilkjentYtelse

        assertThat(nyeAndeler).hasSize(8)

        val andelerPerAktørOgType = nyeAndeler.groupBy { Pair(it.aktør, it.type) }
        andelerPerAktørOgType.map { (_, v) -> assertThat(v).hasSize(2) }
    }

    @Test
    fun `skal oppdatere satsen på ordinær andel for barn over 6 år`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2010, 1, 1))
        val satsendringsbehandling = lagBehandling()
        val ordinæreAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1054,
                prosent = BigDecimal(100),
                person = barn,
            ),
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val ordinærTidslinje = AndelTilkjentYtelseTidslinje(ordinæreAndeler)

        val nyeAndeler = ordinærTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
            person = barn,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse,
        )

        assertThat(nyeAndeler).hasSize(2)
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 2),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1054,
                prosent = BigDecimal(100),
                sats = 1054,
                aktør = barn.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[0],
        )
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2023, 3),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1083,
                prosent = BigDecimal(100),
                sats = 1083,
                aktør = barn.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[1],
        )
    }

    @Test
    fun `skal oppdatere satsen på ordinær andel for barn under 6 år`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 2, 1))
        val satsendringsbehandling = lagBehandling()
        val ordinæreAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1676,
                prosent = BigDecimal(100),
                person = barn,
            ),
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val ordinærTidslinje = AndelTilkjentYtelseTidslinje(ordinæreAndeler)

        val nyeAndeler = ordinærTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
            person = barn,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse,
        )

        assertThat(nyeAndeler).hasSize(2)
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 2),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1676,
                prosent = BigDecimal(100),
                sats = 1676,
                aktør = barn.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[0],
        )
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2023, 3),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1723,
                prosent = BigDecimal(100),
                sats = 1723,
                aktør = barn.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[1],
        )
    }

    @Test
    fun `skal oppdatere satsen på ordinære andeler hvor prosent er 50`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 2, 1))
        val satsendringsbehandling = lagBehandling()
        val ordinæreAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 2),
                tom = YearMonth.of(2022, 4),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1676,
                prosent = BigDecimal(100),
                person = barn,
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 838,
                prosent = BigDecimal(50),
                person = barn,
            ),
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val ordinærTidslinje = AndelTilkjentYtelseTidslinje(ordinæreAndeler)

        val nyeAndeler = ordinærTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
            person = barn,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse,
        )

        assertThat(nyeAndeler).hasSize(3)
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2022, 2),
                tom = YearMonth.of(2022, 4),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 1676,
                prosent = BigDecimal(100),
                sats = 1676,
                aktør = barn.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[0],
        )
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 2),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 838,
                prosent = BigDecimal(50),
                sats = 1676,
                aktør = barn.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[1],
        )
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2023, 3),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 862,
                prosent = BigDecimal(50),
                sats = 1723,
                aktør = barn.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[2],
        )
    }

    @Test
    fun `skal oppdatere satsen på utvidet andel`() {
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(2001, 1, 1))
        val satsendringsbehandling = lagBehandling()
        val utvidetAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                beløp = 1054,
                prosent = BigDecimal(100),
                person = søker,
            ),
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val utvidetTidslinje = AndelTilkjentYtelseTidslinje(utvidetAndeler)

        val nyeAndeler = utvidetTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            person = søker,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse,
        )

        assertThat(nyeAndeler).hasSize(2)
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 2),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                beløp = 1054,
                prosent = BigDecimal(100),
                sats = 1054,
                aktør = søker.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[0],
        )
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2023, 3),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                beløp = 2489,
                prosent = BigDecimal(100),
                sats = 2489,
                aktør = søker.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[1],
        )
    }

    @Test
    fun `skal oppdatere satsen på smpbarnstillegg andel`() {
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(2001, 1, 1))
        val satsendringsbehandling = lagBehandling()
        val småbarnstilleggAndel = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                beløp = 660,
                prosent = BigDecimal(100),
                person = søker,
            ),
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val småbarnstilleggTidslinje = AndelTilkjentYtelseTidslinje(småbarnstilleggAndel)

        val nyeAndeler = småbarnstilleggTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.SMÅBARNSTILLEGG,
            person = søker,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse,
        )

        assertThat(nyeAndeler).hasSize(2)
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 2),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                beløp = 660,
                prosent = BigDecimal(100),
                sats = 660,
                aktør = søker.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[0],
        )
        assertAndel(
            forventetAndel = ForventetAndel(
                fom = YearMonth.of(2023, 3),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                beløp = 678,
                prosent = BigDecimal(100),
                sats = 678,
                aktør = søker.aktør,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            ),
            faktiskAndel = nyeAndeler[1],
        )
    }

    @Test
    fun `skal kaste feil hvis det finnes andeler som har feil ytelsetype`() {
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(2001, 1, 1))
        val satsendringsbehandling = lagBehandling()
        val småbarnstilleggAndel = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                beløp = 660,
                prosent = BigDecimal(100),
                person = søker,
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 4),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                beløp = 1054,
                prosent = BigDecimal(100),
                person = søker,
            ),
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val småbarnstilleggTidslinje = AndelTilkjentYtelseTidslinje(småbarnstilleggAndel)

        assertThrows<Feil> {
            småbarnstilleggTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            )
        }
    }

    @Test
    fun `skal kaste feil hvis det finnes andeler som er knyttet til feil person`() {
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(2001, 1, 1))
        val satsendringsbehandling = lagBehandling()
        val småbarnstilleggAndel = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                beløp = 660,
                prosent = BigDecimal(100),
                person = lagPerson(type = PersonType.SØKER),
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 4),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                beløp = 1054,
                prosent = BigDecimal(100),
                person = søker,
            ),
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val småbarnstilleggTidslinje = AndelTilkjentYtelseTidslinje(småbarnstilleggAndel)

        assertThrows<Feil> {
            småbarnstilleggTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
                behandlingId = satsendringsbehandling.id,
                tilkjentYtelse = nyTilkjentYtelse,
            )
        }
    }

    @Test
    fun `skal returnere tom liste hvis det ikke finnes noen forrige andeler`() {
        val søker = lagPerson(type = PersonType.SØKER, fødselsdato = LocalDate.of(2001, 1, 1))
        val satsendringsbehandling = lagBehandling()
        val forrigeAndeler = emptyList<AndelTilkjentYtelse>()
        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val forrigeAndelerTidslinje = AndelTilkjentYtelseTidslinje(forrigeAndeler)

        val nyeAndeler = forrigeAndelerTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            person = søker,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse,
        )

        assertThat(nyeAndeler).isEmpty()
    }

    private fun assertAndel(forventetAndel: ForventetAndel, faktiskAndel: AndelTilkjentYtelse) {
        assertThat(faktiskAndel.stønadFom).isEqualTo(forventetAndel.fom)
        assertThat(faktiskAndel.stønadTom).isEqualTo(forventetAndel.tom)
        assertThat(faktiskAndel.type).isEqualTo(forventetAndel.ytelseType)
        assertThat(faktiskAndel.kalkulertUtbetalingsbeløp).isEqualTo(forventetAndel.beløp)
        assertThat(faktiskAndel.sats).isEqualTo(forventetAndel.sats)
        assertThat(faktiskAndel.prosent).isEqualTo(forventetAndel.prosent)
        assertThat(faktiskAndel.behandlingId).isEqualTo(forventetAndel.behandlingId)
        assertThat(faktiskAndel.tilkjentYtelse).isEqualTo(forventetAndel.tilkjentYtelse)
        assertThat(faktiskAndel.aktør).isEqualTo(forventetAndel.aktør)
    }

    private data class ForventetAndel(
        val fom: YearMonth,
        val tom: YearMonth,
        val ytelseType: YtelseType,
        val beløp: Int,
        val behandlingId: Long,
        val tilkjentYtelse: TilkjentYtelse,
        val aktør: Aktør,
        val sats: Int,
        val prosent: BigDecimal,
    )
}
