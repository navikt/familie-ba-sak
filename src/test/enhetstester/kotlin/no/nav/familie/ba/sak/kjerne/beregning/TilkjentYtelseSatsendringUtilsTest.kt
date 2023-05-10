package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseSatsendringUtils.lagAndelerMedNySatsForPersonOgYtelsetype
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class TilkjentYtelseSatsendringUtilsTest {

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
                person = barn
            )
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val ordinærTidslinje = AndelTilkjentYtelseTidslinje(ordinæreAndeler)

        val nyeAndeler = ordinærTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
            person = barn,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse
        )

        Assertions.assertEquals(2, nyeAndeler.size)
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[0]
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[1]
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
                person = barn
            )
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val ordinærTidslinje = AndelTilkjentYtelseTidslinje(ordinæreAndeler)

        val nyeAndeler = ordinærTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
            person = barn,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse
        )

        Assertions.assertEquals(2, nyeAndeler.size)
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[0]
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[1]
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
                person = barn
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 5),
                tom = YearMonth.of(2023, 5),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beløp = 838,
                prosent = BigDecimal(50),
                person = barn
            )
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val ordinærTidslinje = AndelTilkjentYtelseTidslinje(ordinæreAndeler)

        val nyeAndeler = ordinærTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
            person = barn,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse
        )

        Assertions.assertEquals(3, nyeAndeler.size)
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[0]
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[1]
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[2]
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
                person = søker
            )
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val utvidetTidslinje = AndelTilkjentYtelseTidslinje(utvidetAndeler)

        val nyeAndeler = utvidetTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            person = søker,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse
        )

        Assertions.assertEquals(2, nyeAndeler.size)
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[0]
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[1]
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
                person = søker
            )
        )

        val nyTilkjentYtelse = lagInitiellTilkjentYtelse(behandling = satsendringsbehandling)

        val småbarnstilleggTidslinje = AndelTilkjentYtelseTidslinje(småbarnstilleggAndel)

        val nyeAndeler = småbarnstilleggTidslinje.lagAndelerMedNySatsForPersonOgYtelsetype(
            ytelseType = YtelseType.SMÅBARNSTILLEGG,
            person = søker,
            behandlingId = satsendringsbehandling.id,
            tilkjentYtelse = nyTilkjentYtelse
        )

        Assertions.assertEquals(2, nyeAndeler.size)
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[0]
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
                tilkjentYtelse = nyTilkjentYtelse
            ),
            faktiskAndel = nyeAndeler[1]
        )
    }

    private fun assertAndel(forventetAndel: ForventetAndel, faktiskAndel: AndelTilkjentYtelse) {
        Assertions.assertEquals(forventetAndel.fom, faktiskAndel.stønadFom)
        Assertions.assertEquals(forventetAndel.tom, faktiskAndel.stønadTom)
        Assertions.assertEquals(forventetAndel.ytelseType, faktiskAndel.type)
        Assertions.assertEquals(forventetAndel.beløp, faktiskAndel.kalkulertUtbetalingsbeløp)
        Assertions.assertEquals(forventetAndel.sats, faktiskAndel.sats)
        Assertions.assertEquals(forventetAndel.prosent, faktiskAndel.prosent)
        Assertions.assertEquals(forventetAndel.behandlingId, faktiskAndel.behandlingId)
        Assertions.assertEquals(forventetAndel.tilkjentYtelse, faktiskAndel.tilkjentYtelse)
        Assertions.assertEquals(forventetAndel.aktør, faktiskAndel.aktør)
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
        val prosent: BigDecimal
    )
}
