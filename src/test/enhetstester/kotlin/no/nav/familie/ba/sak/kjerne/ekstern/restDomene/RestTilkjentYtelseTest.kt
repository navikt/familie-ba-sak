package no.nav.familie.ba.sak.kjerne.ekstern.restDomene

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.ekstern.restDomene.RestYtelsePeriode
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonerMedAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.rest.BeregningOppsummering
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.time.YearMonth

class RestTilkjentYtelseTest {
    val barn1 = tilfeldigPerson()
    val søker = tilfeldigSøker()

    @Test
    fun `Skal kombinere regelverk og kompetanse inn i rest tilkjent ytelse`() {

        val startMåned = jan(2020)
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0, personer = mutableSetOf(barn1, søker))

        val andelerTilkjentYtelse =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2020, 2),
                    beløp = 1354,
                    aktør = barn1.aktør, ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 3),
                    tom = YearMonth.of(2020, 4),
                    beløp = 1054,
                    aktør = barn1.aktør
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 7),
                    tom = YearMonth.of(2020, 8),
                    beløp = 1354,
                    aktør = barn1.aktør
                ),
            )

        val regelverkTidslinje = "EENN  EENN".tilRegelverkTidslinje(startMåned)

        val kompetanser = KompetanseTidslinje("SP    TT  ".tilCharTidslinje(startMåned), listOf(barn1)).perioder()
            .mapNotNull { it.innhold?.copy(fom = it.fraOgMed.tilYearMonth(), tom = it.tilOgMed.tilYearMonth()) }

        val restPersonerMedAndeler = personopplysningGrunnlag.tilRestPersonerMedAndeler(
            andelerTilkjentYtelse,
            mapOf(barn1.aktør to regelverkTidslinje),
            kompetanser
        )

        val restAndelPåBarn = restPersonerMedAndeler.single { it.personIdent == barn1.aktør.aktivFødselsnummer() }

        Assertions.assertEquals(4, restAndelPåBarn.ytelsePerioder.size)
        Assertions.assertEquals(
            RestYtelsePeriode(
                beløp = 1354,
                stønadFom = YearMonth.of(2020, 1),
                stønadTom = YearMonth.of(2020, 1),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beregningOppsummering = BeregningOppsummering(
                    regelverk = Regelverk.EØS_FORORDNINGEN,
                    kompetentLand = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND
                ),
            ),
            restAndelPåBarn.ytelsePerioder[0]
        )
        Assertions.assertEquals(
            RestYtelsePeriode(
                beløp = 1354,
                stønadFom = YearMonth.of(2020, 2),
                stønadTom = YearMonth.of(2020, 2),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beregningOppsummering = BeregningOppsummering(
                    regelverk = Regelverk.EØS_FORORDNINGEN,
                    kompetentLand = KompetanseResultat.NORGE_ER_PRIMÆRLAND
                ),
            ),
            restAndelPåBarn.ytelsePerioder[1]
        )
        Assertions.assertEquals(
            RestYtelsePeriode(
                beløp = 1054,
                stønadFom = YearMonth.of(2020, 3),
                stønadTom = YearMonth.of(2020, 4),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beregningOppsummering = BeregningOppsummering(
                    regelverk = Regelverk.NASJONALE_REGLER,
                    kompetentLand = null
                ),
            ),
            restAndelPåBarn.ytelsePerioder[2]
        )
        Assertions.assertEquals(
            RestYtelsePeriode(
                beløp = 1354,
                stønadFom = YearMonth.of(2020, 7),
                stønadTom = YearMonth.of(2020, 8),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                beregningOppsummering = BeregningOppsummering(
                    regelverk = Regelverk.EØS_FORORDNINGEN,
                    kompetentLand = KompetanseResultat.TO_PRIMÆRLAND
                ),
            ),
            restAndelPåBarn.ytelsePerioder[3]
        )
    }
}
