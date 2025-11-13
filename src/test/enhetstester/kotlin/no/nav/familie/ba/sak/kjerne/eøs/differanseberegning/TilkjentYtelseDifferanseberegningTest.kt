package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.util.DeltBostedBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.TilkjentYtelseBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.oppdaterTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.byggTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Merk at operasjoner som tilsynelatende lager en ny instans av TilkjentYtelse, faktisk returner samme.
 * Det skyldes at JPA krever muterbare objekter.
 * Ikke-muterbarhet krever en omskrivning av koden. F.eks å koble vekk EndretUtbetalingPeriode fra AndelTilkjentYtelse
 */
class TilkjentYtelseDifferanseberegningTest {
    @Test
    fun `skal gjøre differanseberegning på en tilkjent ytelse med endringsperioder`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val behandling = lagBehandling()
        val behandlingId = BehandlingId(behandling.id)
        val startMåned = barnsFødselsdato.toYearMonth()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, startMåned)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", BOSATT_I_RIKET)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", LOVLIG_OPPHOLD)
                .forPerson(barn1, startMåned)
                .medVilkår("+>", UNDER_18_ÅR, GIFT_PARTNERSKAP)
                .medVilkår("E>", BOSATT_I_RIKET, LOVLIG_OPPHOLD)
                .medVilkår("é>", BOR_MED_SØKER)
                .forPerson(barn2, startMåned)
                .medVilkår("+>", UNDER_18_ÅR, GIFT_PARTNERSKAP)
                .medVilkår("E>", BOSATT_I_RIKET, LOVLIG_OPPHOLD)
                .medVilkår("é>", BOR_MED_SØKER)
                .byggPerson()

        val tilkjentYtelse = vilkårsvurderingBygger.byggTilkjentYtelse()

        assertEquals(6, tilkjentYtelse.andelerTilkjentYtelse.size)

        DeltBostedBuilder(startMåned, tilkjentYtelse)
            .medDeltBosted(" //////000000000011111>", barn1, barn2)
            .oppdaterTilkjentYtelse()

        val forventetTilkjentYtelseMedDelt =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(barn1, barn2)
                .medOrdinær(" $$$$$$", prosent = 50, utenEndretUtbetaling = { it / 2 }) { it / 2 }
                .medOrdinær("       $$$$$$$$$$", prosent = 0, utenEndretUtbetaling = { it / 2 }) { 0 }
                .medOrdinær("                 $$$$$$", prosent = 100, utenEndretUtbetaling = { it / 2 }) { it }
                .bygg()

        assertEquals(10, tilkjentYtelse.andelerTilkjentYtelse.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseMedDelt.andelerTilkjentYtelse,
            tilkjentYtelse.andelerTilkjentYtelse,
        )

        val utenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
                .medBeløp(" 44555666>", "EUR", "fr", barn1, barn2)
                .bygg()

        val valutakurser =
            ValutakursBuilder(startMåned, behandlingId)
                .medKurs(" 888899999>", "EUR", barn1, barn2)
                .bygg()

        val forventetTilkjentYtelseMedDiff =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(barn1, barn2)
                .medOrdinær(" $$", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 32 }, utenEndretUtbetaling = { it / 2 }) { it / 2 - 32 }
                .medOrdinær("   $$", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 40 }, utenEndretUtbetaling = { it / 2 }) { it / 2 - 40 }
                .medOrdinær("     $", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 45 }, utenEndretUtbetaling = { it / 2 }) { it / 2 - 45 }
                .medOrdinær("      $", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 54 }, utenEndretUtbetaling = { it / 2 }) { it / 2 - 54 }
                .medOrdinær("       $$$$$$$$$$", 0, nasjonalt = { 0 }, differanse = { it / 2 - 54 }, utenEndretUtbetaling = { it / 2 }) { 0 }
                .medOrdinær("                 $$$$$$", 100, nasjonalt = { it }, differanse = { it / 2 - 54 }, utenEndretUtbetaling = { it / 2 }) { it - 54 }
                .bygg()

        val andelerMedDifferanse =
            beregnDifferanse(
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
            )

        assertEquals(16, andelerMedDifferanse.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse,
            andelerMedDifferanse,
        )
    }

    @Test
    fun `skal fjerne differanseberegning når utenlandsk periodebeløp eller valutakurs nullstilles`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val behandling = lagBehandling()
        val behandlingId = BehandlingId(behandling.id)
        val startMåned = barnsFødselsdato.toYearMonth()

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder(behandling)
                .forPerson(søker, startMåned)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", BOSATT_I_RIKET)
                .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", LOVLIG_OPPHOLD)
                .forPerson(barn1, startMåned)
                .medVilkår("+>", UNDER_18_ÅR, GIFT_PARTNERSKAP)
                .medVilkår("E>", BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
                .byggPerson()

        val tilkjentYtelse = vilkårsvurderingBygger.byggTilkjentYtelse()

        val forventetTilkjentYtelseKunSats =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(barn1)
                .medOrdinær(" $$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { null }, differanse = { null })
                .bygg()

        assertEquals(3, tilkjentYtelse.andelerTilkjentYtelse.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseKunSats.andelerTilkjentYtelse,
            tilkjentYtelse.andelerTilkjentYtelse,
        )

        val utenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
                .medBeløp(" 44555666>", "EUR", "fr", barn1)
                .bygg()

        val valutakurser =
            ValutakursBuilder(startMåned, behandlingId)
                .medKurs(" 888899999>", "EUR", barn1)
                .bygg()

        val forventetTilkjentYtelseMedDiff =
            TilkjentYtelseBuilder(startMåned, behandling)
                .forPersoner(barn1)
                .medOrdinær(" $$                    ", nasjonalt = { it }, differanse = { it - 32 }) { it - 32 }
                .medOrdinær("   $$                  ", nasjonalt = { it }, differanse = { it - 40 }) { it - 40 }
                .medOrdinær("     $                 ", nasjonalt = { it }, differanse = { it - 45 }) { it - 45 }
                .medOrdinær("      $$$$$$$$$$$$$$$$$", nasjonalt = { it }, differanse = { it - 54 }) { it - 54 }
                .bygg()

        val andelerMedDiff =
            beregnDifferanse(
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
            )

        assertEquals(6, andelerMedDiff.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse,
            andelerMedDiff,
        )

        val blanktUtenlandskPeridebeløp =
            UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
                .medBeløp(" >", null, null, barn1)
                .bygg()

        val andelerUtenDiff =
            beregnDifferanse(
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
                utenlandskePeriodebeløp = blanktUtenlandskPeridebeløp,
                valutakurser = valutakurser,
            )

        assertEquals(3, andelerUtenDiff.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseKunSats.andelerTilkjentYtelse,
            andelerUtenDiff,
        )

        val andelerMedDiffIgjen =
            beregnDifferanse(
                andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
                utenlandskePeriodebeløp = utenlandskePeriodebeløp,
                valutakurser = valutakurser,
            )

        assertEquals(6, andelerMedDiffIgjen.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse,
            andelerMedDiffIgjen,
        )
    }

    @Test
    fun `Skal trekke fra finnmarkstillegg andel og ikke ordinær andel ved differanseberegning hvis det er nok med finnmarkstillegget`() {
        // Arrange
        val barnsFødselsdato = 13.jan(2020)
        val barn = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val andelerForBarn =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    beløp = 1000,
                    person = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    ytelseType = YtelseType.FINNMARKSTILLEGG,
                    beløp = 500,
                    person = barn,
                ),
            )

        val utenlandskPeriodeBeløpIPeriode =
            listOf(
                UtenlandskPeriodebeløp(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    barnAktører = setOf(barn.aktør),
                    beløp = BigDecimal(200),
                    valutakode = "SEK",
                    intervall = Intervall.MÅNEDLIG,
                    utbetalingsland = "NOR",
                    kalkulertMånedligBeløp = BigDecimal.valueOf(200),
                ),
            )

        val valutakursIPeriode =
            listOf(
                Valutakurs(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    barnAktører = setOf(barn.aktør),
                    valutakode = "SEK",
                    vurderingsform = Vurderingsform.AUTOMATISK,
                    kurs = BigDecimal(1),
                ),
            )

        // Act
        val andelerEtterDifferanseBeregning =
            beregnDifferanse(
                andelerForBarn,
                utenlandskePeriodebeløp = utenlandskPeriodeBeløpIPeriode,
                valutakurser = valutakursIPeriode,
            )

        // Assert
        val ordinærAndel = andelerEtterDifferanseBeregning.single { it.type == YtelseType.ORDINÆR_BARNETRYGD }
        val finnmarkstilleggAndel = andelerEtterDifferanseBeregning.single { it.type == YtelseType.FINNMARKSTILLEGG }

        assertThat(finnmarkstilleggAndel.kalkulertUtbetalingsbeløp).isEqualTo(300)
        assertThat(ordinærAndel.kalkulertUtbetalingsbeløp).isEqualTo(1000)
    }

    @Test
    fun `Skal trekke videre fra ordinær andel ved differanseberegning hvis det ikke er nok med finnmarkstillegget`() {
        // Arrange
        val barnsFødselsdato = 13.jan(2020)
        val barn = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato)

        val andelerForBarn =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    beløp = 1000,
                    person = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    ytelseType = YtelseType.FINNMARKSTILLEGG,
                    beløp = 500,
                    person = barn,
                ),
            )

        val utenlandskPeriodeBeløpIPeriode =
            listOf(
                UtenlandskPeriodebeløp(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    barnAktører = setOf(barn.aktør),
                    beløp = BigDecimal(700),
                    valutakode = "SEK",
                    intervall = Intervall.MÅNEDLIG,
                    utbetalingsland = "NOR",
                    kalkulertMånedligBeløp = BigDecimal.valueOf(700),
                ),
            )

        val valutakursIPeriode =
            listOf(
                Valutakurs(
                    fom = YearMonth.of(2025, 10),
                    tom = YearMonth.of(2025, 12),
                    barnAktører = setOf(barn.aktør),
                    valutakode = "SEK",
                    vurderingsform = Vurderingsform.AUTOMATISK,
                    kurs = BigDecimal(1),
                ),
            )

        // Act
        val andelerEtterDifferanseBeregning =
            beregnDifferanse(
                andelerForBarn,
                utenlandskePeriodebeløp = utenlandskPeriodeBeløpIPeriode,
                valutakurser = valutakursIPeriode,
            )

        // Assert
        val ordinærAndel = andelerEtterDifferanseBeregning.single { it.type == YtelseType.ORDINÆR_BARNETRYGD }
        val finnmarkstilleggAndel = andelerEtterDifferanseBeregning.single { it.type == YtelseType.FINNMARKSTILLEGG }

        assertThat(finnmarkstilleggAndel.kalkulertUtbetalingsbeløp).isEqualTo(0)
        assertThat(ordinærAndel.kalkulertUtbetalingsbeløp).isEqualTo(800)
    }
}
