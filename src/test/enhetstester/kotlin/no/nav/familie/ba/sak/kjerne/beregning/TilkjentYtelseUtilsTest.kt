package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.tilyyyyMMdd
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils.beregnTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class TilkjentYtelseUtilsTest {

    @Test
    fun `Barn som fyller 6 år i det vilkårene er oppfylt får andel måneden etter`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnSeksårsdag,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = lagBehandling()
        )

        assertEquals(1, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(
            MånedPeriode(
                barnSeksårsdag.nesteMåned(),
                barnFødselsdato.plusYears(18).forrigeMåned()
            ),
            MånedPeriode(andelTilkjentYtelse.stønadFom, andelTilkjentYtelse.stønadTom)
        )
    }

    @Test
    fun `Barn som fyller 6 år i det vilkårene ikke lenger er oppfylt får andel den måneden også`() {
        val barnSeksårsdag = LocalDate.of(2022, 2, 2)
        val barnFødselsdato = barnSeksårsdag.minusYears(6)

        val vilkårOppfyltFom = barnSeksårsdag.minusMonths(2)
        val vilkårOppfyltTom = barnSeksårsdag.plusDays(2)
        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = vilkårOppfyltFom,
                vilkårOppfyltTom = vilkårOppfyltTom,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = lagBehandling()
        )

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelTilkjentYtelseFør6År = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(
            MånedPeriode(vilkårOppfyltFom.nesteMåned(), barnSeksårsdag.forrigeMåned()),
            MånedPeriode(andelTilkjentYtelseFør6År.stønadFom, andelTilkjentYtelseFør6År.stønadTom)
        )
        assertEquals(1676, andelTilkjentYtelseFør6År.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()
        assertEquals(
            MånedPeriode(barnSeksårsdag.toYearMonth(), barnSeksårsdag.toYearMonth()),
            MånedPeriode(andelTilkjentYtelseEtter6År.stønadFom, andelTilkjentYtelseEtter6År.stønadTom)
        )
        assertEquals(1054, andelTilkjentYtelseEtter6År.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Det skal utbetales for inneværende måned hvis barn dør minst 1 måned før 18 års datoen`() {
        val barnFødselsDato = LocalDate.of(2012, 2, 2)
        val barnDødsfallsDato = LocalDate.of(2030, 1, 1)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsDato,
                vilkårOppfyltFom = barnFødselsDato,
                vilkårOppfyltTom = barnDødsfallsDato,
                barnDødsfallDato = barnDødsfallsDato,
                under18ÅrVilkårOppfyltTom = barnDødsfallsDato
            )

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = lagBehandling()
        )

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelFør6År = tilkjentYtelse.andelerTilkjentYtelse.first()
        val andelEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()

        assertEquals(YearMonth.of(2014, 2), andelFør6År.stønadFom)
        assertEquals(YearMonth.of(2019, 2), andelFør6År.stønadTom)

        assertEquals(YearMonth.of(2019, 3), andelEtter6År.stønadFom)
        assertEquals(YearMonth.of(2030, 1), andelEtter6År.stønadTom)
    }

    @Test
    fun `Det skal ikke utbetales for inneværende måned hvis barn dør samme måned som 18 års datoen`() {
        val barnFødselsDato = LocalDate.of(2012, 2, 20)
        val barnDødsfallsDato = LocalDate.of(2030, 2, 2)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsDato,
                vilkårOppfyltFom = barnFødselsDato,
                vilkårOppfyltTom = barnDødsfallsDato,
                barnDødsfallDato = barnDødsfallsDato,
                under18ÅrVilkårOppfyltTom = barnDødsfallsDato
            )

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = lagBehandling()
        )

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelFør6År = tilkjentYtelse.andelerTilkjentYtelse.first()
        val andelEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()

        assertEquals(YearMonth.of(2014, 2), andelFør6År.stønadFom)
        assertEquals(YearMonth.of(2019, 2), andelFør6År.stønadTom)

        assertEquals(YearMonth.of(2019, 3), andelEtter6År.stønadFom)
        assertEquals(YearMonth.of(2030, 1), andelEtter6År.stønadTom)
    }

    @Test
    fun `1 barn får normal utbetaling med satsendring fra september 2020, september 2021 og januar 2022`() {
        val barnFødselsdato = LocalDate.of(2021, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val (vilkårsvurdering, personopplysningGrunnlag) = genererVilkårsvurderingOgPersonopplysningGrunnlag(
            barnFødselsdato = barnFødselsdato,
            vilkårOppfyltFom = barnFødselsdato,
            under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = lagBehandling()
        )
            .andelerTilkjentYtelse
            .toList()
            .sortedBy { it.stønadFom }

        assertEquals(4, andeler.size)

        val andelTilkjentYtelseFør6ÅrSeptember2020 = andeler[0]
        assertEquals(
            MånedPeriode(barnFødselsdato.nesteMåned(), YearMonth.of(2021, 8)),
            MånedPeriode(
                andelTilkjentYtelseFør6ÅrSeptember2020.stønadFom,
                andelTilkjentYtelseFør6ÅrSeptember2020.stønadTom
            )
        )
        assertEquals(1354, andelTilkjentYtelseFør6ÅrSeptember2020.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseFør6ÅrSeptember2021 = andeler[1]
        assertEquals(
            MånedPeriode(YearMonth.of(2021, 9), YearMonth.of(2021, 12)),
            MånedPeriode(
                andelTilkjentYtelseFør6ÅrSeptember2021.stønadFom,
                andelTilkjentYtelseFør6ÅrSeptember2021.stønadTom
            )
        )
        assertEquals(1654, andelTilkjentYtelseFør6ÅrSeptember2021.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseFør6ÅrJanuar2022 = andeler[2]
        assertEquals(
            MånedPeriode(YearMonth.of(2022, 1), barnSeksårsdag.forrigeMåned()),
            MånedPeriode(
                andelTilkjentYtelseFør6ÅrJanuar2022.stønadFom,
                andelTilkjentYtelseFør6ÅrJanuar2022.stønadTom
            )
        )
        assertEquals(1676, andelTilkjentYtelseFør6ÅrJanuar2022.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseEtter6År = andeler[3]
        assertEquals(
            MånedPeriode(barnSeksårsdag.toYearMonth(), barnFødselsdato.plusYears(18).forrigeMåned()),
            MånedPeriode(andelTilkjentYtelseEtter6År.stønadFom, andelTilkjentYtelseEtter6År.stønadTom)
        )
        assertEquals(1054, andelTilkjentYtelseEtter6År.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Halvt beløp av grunnsats utbetales ved delt bosted`() {
        val barnFødselsdato = LocalDate.of(2021, 2, 2)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = lagBehandling()
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        val andelTilkjentYtelseFør6ÅrSeptember2020 = andeler[0]
        assertEquals(677, andelTilkjentYtelseFør6ÅrSeptember2020.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseFør6ÅrSeptember2021 = andeler[1]
        assertEquals(827, andelTilkjentYtelseFør6ÅrSeptember2021.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseFør6ÅrJanuar2022 = andeler[2]
        assertEquals(838, andelTilkjentYtelseFør6ÅrJanuar2022.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseEtter6År = andeler[3]
        assertEquals(527, andelTilkjentYtelseEtter6År.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for back-to-back perioder på barnet innenfor en måned`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 17),
            backToBackFom = LocalDate.of(2019, 8, 18)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = oppdatertVilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = oppdatertVilkårsvurdering.behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 8), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 9), andeler[2].stønadFom)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for back-to-back perioder på søker innenfor en måned`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 17),
            backToBackFom = LocalDate.of(2019, 8, 18)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = oppdatertVilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = oppdatertVilkårsvurdering.behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 8), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 9), andeler[2].stønadFom)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for back-to-back perioder på barn i månedskifte`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 31),
            backToBackFom = LocalDate.of(2019, 9, 1)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = oppdatertVilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = oppdatertVilkårsvurdering.behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 9), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 10), andeler[2].stønadFom)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for back-to-back perioder på søker i månedskifte`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 31),
            backToBackFom = LocalDate.of(2019, 9, 1)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = oppdatertVilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = oppdatertVilkårsvurdering.behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 9), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 10), andeler[2].stønadFom)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for perioder på barn som ikke er back-to-back over månedskiftet`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 31),
            backToBackFom = LocalDate.of(2019, 9, 2)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = oppdatertVilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = oppdatertVilkårsvurdering.behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 8), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 10), andeler[2].stønadFom)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for perioder på søker som ikke er back-to-back over månedskiftet`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 31),
            backToBackFom = LocalDate.of(2019, 9, 2)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = oppdatertVilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = oppdatertVilkårsvurdering.behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 8), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 10), andeler[2].stønadFom)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for perioder på barn som slutter og starter første og siste dag i mnd`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 1),
            backToBackFom = LocalDate.of(2019, 8, 31)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = oppdatertVilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = oppdatertVilkårsvurdering.behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 8), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 9), andeler[2].stønadFom)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for perioder på søker som slutter og starter første og siste dag i mnd`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererVilkårsvurderingOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true,
                under18ÅrVilkårOppfyltTom = barnFødselsdato.plusYears(18)
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 1),
            backToBackFom = LocalDate.of(2019, 8, 31)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelseGammel(
            vilkårsvurdering = oppdatertVilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = oppdatertVilkårsvurdering.behandling
        )
            .andelerTilkjentYtelse.toList()
            .sortedBy { it.stønadFom }

        assertEquals(YearMonth.of(2019, 8), andeler[1].stønadTom)
        assertEquals(YearMonth.of(2019, 9), andeler[2].stønadFom)
    }

    private fun oppdaterBosattIRiketMedBack2BackPerioder(
        vilkårsvurdering: Vilkårsvurdering,
        personResultat: PersonResultat,
        barnFødselsdato: LocalDate,
        backToBackTom: LocalDate? = null,
        backToBackFom: LocalDate? = null
    ): Vilkårsvurdering {
        personResultat.setSortedVilkårResultater(
            personResultat.vilkårResultater.filter { it.vilkårType != Vilkår.BOSATT_I_RIKET }
                .toSet() +
                setOf(
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = barnFødselsdato,
                        periodeTom = backToBackTom,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id
                    ),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = backToBackFom,
                        periodeTom = null,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id
                    )
                )
        )

        vilkårsvurdering.personResultater =
            vilkårsvurdering.personResultater.filter { it.aktør != personResultat.aktør }.toSet() + setOf(
            personResultat
        )

        return vilkårsvurdering
    }

    private fun genererVilkårsvurderingOgPersonopplysningGrunnlag(
        barnFødselsdato: LocalDate,
        vilkårOppfyltFom: LocalDate,
        vilkårOppfyltTom: LocalDate? = barnFødselsdato.plusYears(18),
        barnDødsfallDato: LocalDate? = null,
        erDeltBosted: Boolean = false,
        under18ÅrVilkårOppfyltTom: LocalDate?
    ): Pair<Vilkårsvurdering, PersonopplysningGrunnlag> {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerAktørId = tilAktør(søkerFnr)
        val barnAktørId = tilAktør(barnFnr)

        val behandling = lagBehandling()

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søkerAktørId,
            behandling = behandling,
            resultat = Resultat.OPPFYLT,
            søkerPeriodeFom = LocalDate.of(2014, 1, 1),
            søkerPeriodeTom = null
        )

        val barnResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnAktørId)
        barnResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = vilkårOppfyltFom,
                    periodeTom = vilkårOppfyltTom,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødselsdato,
                    periodeTom = under18ÅrVilkårOppfyltTom,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    behandlingId = behandling.id
                ),
                VilkårResultat(
                    personResultat = barnResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnFødselsdato,
                    periodeTom = null,
                    begrunnelse = "",
                    behandlingId = behandling.id,
                    utdypendeVilkårsvurderinger = listOfNotNull(
                        if (erDeltBosted) UtdypendeVilkårsvurdering.DELT_BOSTED else null
                    )
                )
            )
        )

        vilkårsvurdering.personResultater = setOf(vilkårsvurdering.personResultater.first(), barnResultat)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)

        val barn = Person(
            aktør = tilAktør(barnFnr),
            type = PersonType.BARN,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = barnFødselsdato,
            navn = "Barn",
            kjønn = Kjønn.MANN
        )
            .apply {
                sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this))
                barnDødsfallDato?.let { dødsfall = lagDødsfall(this, it.tilyyyyMMdd(), null) }
            }
        val søker = Person(
            aktør = tilAktør(søkerFnr),
            type = PersonType.SØKER,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = barnFødselsdato.minusYears(20),
            navn = "Barn",
            kjønn = Kjønn.MANN
        )
            .apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }
        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.add(barn)

        return Pair(vilkårsvurdering, personopplysningGrunnlag)
    }

    @Test
    fun `endret utbetalingsandel skal overstyre andel`() {
        val person = lagPerson()
        val behandling = lagBehandling()
        val fom = YearMonth.of(2018, 1)
        val tom = YearMonth.of(2019, 1)
        val utbetalinsandeler = listOf(
            lagAndelTilkjentYtelse(
                fom = fom,
                tom = tom,
                person = person,
                behandling = behandling
            )
        )

        val endretProsent = BigDecimal.ZERO

        val endretUtbetalingAndeler = listOf(
            lagEndretUtbetalingAndel(
                person = person,
                fom = fom,
                tom = tom,
                prosent = endretProsent,
                behandlingId = behandling.id
            )
        )

        val andelerTIlkjentYtelse = oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            utbetalinsandeler,
            endretUtbetalingAndeler
        )

        assertEquals(1, andelerTIlkjentYtelse.size)
        assertEquals(endretProsent, andelerTIlkjentYtelse.single().prosent)
        assertEquals(1, andelerTIlkjentYtelse.single().endretUtbetalingAndeler.size)
    }

    @Test
    fun `endret utbetalingsandel koble endrede andeler til riktig endret utbetalingandel`() {
        val person = lagPerson()
        val behandling = lagBehandling()
        val fom1 = YearMonth.of(2018, 1)
        val tom1 = YearMonth.of(2018, 11)

        val fom2 = YearMonth.of(2019, 1)
        val tom2 = YearMonth.of(2019, 11)

        val utbetalinsandeler = listOf(
            lagAndelTilkjentYtelse(
                fom = fom1,
                tom = tom1,
                person = person,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = fom2,
                tom = tom2,
                person = person,
                behandling = behandling
            )
        )

        val endretProsent = BigDecimal.ZERO

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = person,
            fom = fom1,
            tom = tom2,
            prosent = endretProsent,
            behandlingId = behandling.id
        )

        val endretUtbetalingAndeler = listOf(
            endretUtbetalingAndel,
            lagEndretUtbetalingAndel(
                person = person,
                fom = tom2.nesteMåned(),
                prosent = endretProsent,
                behandlingId = behandling.id
            )
        )

        val andelerTIlkjentYtelse = oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            utbetalinsandeler,
            endretUtbetalingAndeler
        )

        assertEquals(2, andelerTIlkjentYtelse.size)
        andelerTIlkjentYtelse.forEach { assertEquals(endretProsent, it.prosent) }
        andelerTIlkjentYtelse.forEach { assertEquals(1, it.endretUtbetalingAndeler.size) }
        andelerTIlkjentYtelse.forEach {
            assertEquals(
                endretUtbetalingAndel.id,
                it.endretUtbetalingAndeler.single().id
            )
        }
    }

    val søker = lagPerson(type = PersonType.SØKER)
    val januar2019 = YearMonth.of(2019, 1)
    val februar2019 = YearMonth.of(2019, 2)
    val mars2019 = YearMonth.of(2019, 3)
    val april2019 = YearMonth.of(2019, 4)
    val juli2019 = YearMonth.of(2019, 7)
    val august2019 = YearMonth.of(2019, 8)
    val november2019 = YearMonth.of(2019, 11)
    val desember2019 = YearMonth.of(2019, 12)
    val august2020 = YearMonth.of(2020, 8)
    val januar2022 = YearMonth.of(2022, 1)
    val februar2022 = YearMonth.of(2022, 2)
    val mars2022 = YearMonth.of(2022, 3)
    val april2022 = YearMonth.of(2022, 4)
    val mai2022 = YearMonth.of(2022, 5)
    val juni2022 = YearMonth.of(2022, 6)
    val juli2022 = YearMonth.of(2022, 7)
    val august2022 = YearMonth.of(2022, 8)
    val november2022 = YearMonth.of(2022, 11)
    val desember2022 = YearMonth.of(2022, 12)

    // Scenario: Far søker om delt bosted. Mor har tidligere mottatt fult utvidet og ordinær barnetrygd
    @Test
    fun `Skal støtte endret utbetaling som delvis overlapper delt bosted på søker og barn og småbarnstillegg på søker`() {
        val relevantPeriode = MånedPeriode(januar2022, desember2022)
        val relevantEndringsPeriode = MånedPeriode(april2022, juli2022)
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val barn18ÅrMånedMinusEn = barnFødtAugust2019.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth()
        val barnFyller3ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(3).toYearMonth()
        val barnFyller6ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(6).minusMonths(1).toYearMonth()

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = søker,
                    skalUtbetales = false,
                    årsak = Årsak.DELT_BOSTED,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                ),
                EndretAndel(
                    person = barnFødtAugust2019,
                    skalUtbetales = false,
                    årsak = Årsak.DELT_BOSTED,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = mars2022.toLocalDate(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                    aktør = barnFødtAugust2019.aktør
                )
            ),
            barna = listOf(barnFødtAugust2019),
            overgangsstønadPerioder = listOf(MånedPeriode(januar2022, november2022))
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }
        assertEquals(6, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(2, utvidetAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2022,
                tom = juli2022,
                prosent = BigDecimal.ZERO,
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = utvidetAndeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2022,
                tom = barn18ÅrMånedMinusEn,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = utvidetAndeler.last()
        )

        assertEquals(2, småbarnstilleggAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2022,
                tom = juli2022,
                prosent = BigDecimal.ZERO
            ),
            faktiskAndel = småbarnstilleggAndeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2022,
                tom = barnFyller3ÅrDato,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler.last()
        )

        // BARN
        assertEquals(2, barnasAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = relevantEndringsPeriode.fom,
                tom = relevantEndringsPeriode.tom,
                prosent = BigDecimal.ZERO,
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = barnasAndeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2022,
                tom = barnFyller6ÅrDato,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = barnasAndeler.last()
        )
    }

    // Scenario: Far søker om delt bosted. Mor har tidligere mottatt fult, men har ikke mottatt utvidet.
    @Test
    fun `Skal støtte endret utbetaling som kun gjelder barn på delt bosted utbetaling`() {
        val relevantPeriode = MånedPeriode(januar2022, desember2022)
        val relevantEndringsPeriode = MånedPeriode(april2022, juli2022)
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val barn18ÅrMånedMinusEn = barnFødtAugust2019.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth()
        val barnFyller3ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(3).toYearMonth()
        val barnFyller6ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(6).minusMonths(1).toYearMonth()

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = barnFødtAugust2019,
                    skalUtbetales = false,
                    årsak = Årsak.DELT_BOSTED,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = mars2022.toLocalDate(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                    aktør = barnFødtAugust2019.aktør
                )
            ),
            barna = listOf(barnFødtAugust2019),
            overgangsstønadPerioder = listOf(MånedPeriode(januar2022, november2022))
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }
        assertEquals(4, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2022,
                tom = barn18ÅrMånedMinusEn,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = utvidetAndeler.single()
        )

        assertEquals(1, småbarnstilleggAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2022,
                tom = barnFyller3ÅrDato,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler.single()
        )

        // BARN
        assertEquals(2, barnasAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2022,
                tom = juli2022,
                prosent = BigDecimal.ZERO,
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = barnasAndeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2022,
                tom = barnFyller6ÅrDato,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = barnasAndeler.last()
        )
    }

    // Scenario: Mor har tidligere mottatt barnetrygden. Far har nå søkt om delt bosted og mors barnetrygd skal også deles.
    @Test
    fun `Skal gi riktig resultat når barnetrygden går over til å være delt, kun småbarnstillegg og utvidet blir delt i første periode`() {
        val relevantPeriode = MånedPeriode(januar2022, desember2022)
        val relevantEndringsPeriode = MånedPeriode(juni2022, juli2022)
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val barn18ÅrMånedMinusEn = barnFødtAugust2019.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth()
        val barnFyller3ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(3).toYearMonth()
        val barnFyller6ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(6).minusMonths(1).toYearMonth()

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = barnFødtAugust2019,
                    skalUtbetales = true,
                    årsak = Årsak.DELT_BOSTED,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = februar2022.toLocalDate(),
                    tom = april2022.toLocalDate().sisteDagIMåned(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2019.aktør
                ),
                AtypiskVilkår(
                    fom = mai2022.toLocalDate(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2019.aktør,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = mai2022.toLocalDate(),
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            ),
            overgangsstønadPerioder = listOf(MånedPeriode(januar2022, november2022)),
            barna = listOf(barnFødtAugust2019)
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }
        assertEquals(5, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = juni2022,
                tom = barn18ÅrMånedMinusEn,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = utvidetAndeler.single()
        )

        assertEquals(1, småbarnstilleggAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = juni2022,
                tom = barnFyller3ÅrDato,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler.single()
        )

        // BARN
        assertEquals(3, barnasAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = mai2022,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = barnasAndeler[0]
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = juni2022,
                tom = juli2022,
                prosent = BigDecimal(100),
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = barnasAndeler[1]
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2022,
                tom = barnFyller6ÅrDato,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = barnasAndeler[2]
        )
    }

    // Scenario: Mor har tidligere mottatt barnetrygden. Far har nå søkt om delt bosted og mors barnetrygd skal også deles 2.
    @Test
    fun `Delt, utvidet og ordinær barnetrygd deles fra juni, men skal utbetales fult fra juni til og med juli - deles som vanlig fra August`() {
        val relevantPeriode = MånedPeriode(januar2022, desember2022)
        val relevantEndringsPeriode = MånedPeriode(juni2022, juli2022)
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val barn18ÅrMånedMinusEn = barnFødtAugust2019.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth()
        val barnFyller3ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(3).toYearMonth()
        val barnFyller6ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(6).minusMonths(1).toYearMonth()

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = barnFødtAugust2019,
                    skalUtbetales = true,
                    årsak = Årsak.DELT_BOSTED,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                ),
                EndretAndel(
                    person = søker,
                    skalUtbetales = true,
                    årsak = Årsak.DELT_BOSTED,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = februar2022.toLocalDate(),
                    tom = april2022.toLocalDate().sisteDagIMåned(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2019.aktør
                ),
                AtypiskVilkår(
                    fom = mai2022.toLocalDate(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2019.aktør,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = februar2022.toLocalDate(),
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            ),
            barna = listOf(barnFødtAugust2019),
            overgangsstønadPerioder = listOf(MånedPeriode(januar2022, november2022))
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }
        assertEquals(7, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }
        assertEquals(3, utvidetAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = mai2022,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = utvidetAndeler[0]
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = juni2022,
                tom = juli2022,
                prosent = BigDecimal(100),
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = utvidetAndeler[1]
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2022,
                tom = barn18ÅrMånedMinusEn,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = utvidetAndeler[2]
        )

        assertEquals(1, småbarnstilleggAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = barnFyller3ÅrDato,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler.single()
        )

        // BARN
        assertEquals(3, barnasAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = mai2022,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = barnasAndeler[0]
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = juni2022,
                tom = juli2022,
                prosent = BigDecimal(100),
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = barnasAndeler[1]
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2022,
                tom = barnFyller6ÅrDato,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = barnasAndeler[2]
        )
    }

    // Scenario: Far søker om utvidet barnetrygd. Har full overgangsstønad, men søker sent og får ikke etterbetalt mer enn 3år.
    @Test
    fun `Småbarnstillleg, utvidet og ordinær barnetrygd fra april, men skal ikke utbetales før august på grunn av etterbetaling 3 år`() {
        val relevantPeriode = MånedPeriode(januar2019, desember2019)
        val relevantEndringsPeriode = MånedPeriode(april2019, juli2019)
        val overgangsstønadPeriode = MånedPeriode(januar2019, desember2019)
        val barnFødtAugust2016 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2016, 8, 15))
        val barn18ÅrMånedMinusEn = barnFødtAugust2016.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth()

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = barnFødtAugust2016,
                    skalUtbetales = false,
                    årsak = Årsak.ETTERBETALING_3ÅR,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                ),
                EndretAndel(
                    person = søker,
                    skalUtbetales = false,
                    årsak = Årsak.ETTERBETALING_3ÅR,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = mars2019.toLocalDate(),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2016.aktør
                )
            ),
            barna = listOf(barnFødtAugust2016),
            overgangsstønadPerioder = listOf(MånedPeriode(januar2019, november2019))
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }
        assertEquals(6, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }
        assertEquals(2, utvidetAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2019,
                tom = juli2019,
                prosent = BigDecimal(0),
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = utvidetAndeler[0]
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2019,
                tom = barn18ÅrMånedMinusEn,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = utvidetAndeler[1]
        )

        assertEquals(2, småbarnstilleggAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2019,
                tom = juli2019,
                prosent = BigDecimal(0)
            ),
            faktiskAndel = småbarnstilleggAndeler[0]
        )

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2019,
                tom = august2019,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler[1]
        )

        // BARN
        assertEquals(2, barnasAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2019,
                tom = juli2019,
                prosent = BigDecimal(0),
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = barnasAndeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2019,
                tom = august2020,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = barnasAndeler.last()
        )
    }

    // Scenario: Far har mottatt delt utvidet barnetrygd for barn 12år. Søker nå om barnetrygd for barn som flyttet til han for over 3år siden
    @Test
    fun `Det er småbarnstillegg på søker og ordinær barnetrygd på barn 1 fra april, men det skal ikke utbetales før august på grunn av etterbetaling 3 år - Søker og barn 2 har utbetalinger fra tidligere behandlinger som ikke skal overstyres`() {
        val barnFødtAugust2016 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2016, 8, 15))
        val barnFødtAugust2016Fyller18ÅrsDatoMinusEnMåned = barnFødtAugust2016.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth()
        val barnFødtDesember2006 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2006, 12, 1))
        val barnFødtDesember2006Fyller18ÅrsDatoMinusEnMåned = barnFødtDesember2006.fødselsdato.til18ÅrsVilkårsdato().toYearMonth()
        val relevantPeriode = MånedPeriode(januar2019, desember2019)
        val relevantEndringsPeriode = MånedPeriode(april2019, juli2019)
        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            barna = listOf(barnFødtAugust2016, barnFødtDesember2006),
            endretAndeler = listOf(
                EndretAndel(
                    person = barnFødtAugust2016,
                    skalUtbetales = false,
                    årsak = Årsak.ETTERBETALING_3ÅR,
                    fom = relevantEndringsPeriode.fom,
                    tom = relevantEndringsPeriode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = mars2019.toLocalDate(),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2016.aktør
                ),
                AtypiskVilkår(
                    fom = relevantPeriode.fom.førsteDagIInneværendeMåned(),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtDesember2006.aktør,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = februar2019.toLocalDate(),
                    tom = null,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            ),
            overgangsstønadPerioder = listOf(MånedPeriode(januar2019, november2019))
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }
        assertEquals(8, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }
        assertEquals(2, utvidetAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2019,
                tom = juli2019,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = utvidetAndeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2019,
                tom = barnFødtAugust2016Fyller18ÅrsDatoMinusEnMåned,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = utvidetAndeler.last()
        )

        assertEquals(2, småbarnstilleggAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2019,
                tom = juli2019,
                prosent = BigDecimal(0)
            ),
            faktiskAndel = småbarnstilleggAndeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2019,
                tom = august2019,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler.last()
        )

        // BARN
        val (barn1Andeler, barn2Andeler) = barnasAndeler.partition { it.aktør == barnFødtAugust2016.aktør }
        assertEquals(2, barn1Andeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2019,
                tom = juli2019,
                prosent = BigDecimal(0),
                knyttetTilEndringsperiode = true
            ),
            faktiskAndel = barn1Andeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = august2019,
                tom = august2020,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = barn1Andeler.last()
        )

        assertEquals(2, barn2Andeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = februar2019,
                tom = februar2019,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = barn2Andeler.first()
        )
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2019,
                tom = barnFødtDesember2006Fyller18ÅrsDatoMinusEnMåned,
                prosent = BigDecimal(50)
            ),
            faktiskAndel = barn2Andeler.last()
        )
    }

    // Scenario: Far søker om utvidet barnetrygd for barn under 3 år - han har full overgangsstlnad for bare deler av perioden
    @Test
    fun `Skal gi riktig resultat når det overgangsstønad i deler av utbetalingen`() {
        val relevantPeriode = MånedPeriode(januar2022, desember2022)
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val barnFyller6ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(6).minusMonths(1).toYearMonth()

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = februar2022.toLocalDate(),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2019.aktør
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = relevantPeriode.fom.førsteDagIInneværendeMåned(),
                    tom = august2022.toLocalDate(),
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            ),
            barna = listOf(barnFødtAugust2019),
            overgangsstønadPerioder = listOf(MånedPeriode(april2022, juni2022))
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }
        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }
        assertEquals(1, utvidetAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = august2022,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = utvidetAndeler.single()
        )

        assertEquals(1, småbarnstilleggAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2022,
                tom = juni2022,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler.single()
        )

        // BARN
        assertEquals(1, barnasAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = barnFyller6ÅrDato,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = barnasAndeler.single()
        )
    }

    // Scenario: Far søker om utvidet barnetrygd for barn under 3år, men oppfyller vilkårene kun tilbake i tid
    @Test
    fun `Skal gi riktig resultat når det overgangsstønad i deler av utbetalingen - Overgangsstønaden stopper før barn fyller 3 år fordi søker ikke lenger har rett til utvidet barnetrygd`() {
        val relevantPeriode = MånedPeriode(januar2022, desember2022)
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val barnFyller6ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(6).minusMonths(1).toYearMonth()

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = februar2022.toLocalDate(),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2019.aktør
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = januar2022.toLocalDate(),
                    tom = juni2022.toLocalDate().sisteDagIMåned(),
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            ),
            barna = listOf(barnFødtAugust2019),
            overgangsstønadPerioder = listOf(MånedPeriode(april2022, august2022))
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }
        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = juni2022,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = utvidetAndeler.single()
        )

        assertEquals(1, småbarnstilleggAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2022,
                tom = juni2022,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler.single()
        )

        // BARN
        assertEquals(1, barnasAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = barnFyller6ÅrDato,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = barnasAndeler.first()
        )
    }

    // Scenario: Far søker om utvidet barnetrygd for barn under 3 år - Har full overgangsstønad som opphører når barnet fyller 3 år
    @Test
    fun `Skal gi riktig resultat når søker har rett på ordinær og utvidet barnetrygd fra mars og rett på overgangsstønad fra April`() {
        val relevantPeriode = MånedPeriode(januar2022, desember2022)
        val barnFødtAugust2019 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 8, 15))
        val barnFyller6ÅrDato = barnFødtAugust2019.fødselsdato.plusYears(6).minusMonths(1).toYearMonth()
        val barnFyller18ÅrDato = barnFødtAugust2019.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth()

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = februar2022.toLocalDate(),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barnFødtAugust2019.aktør
                )
            ),
            overgangsstønadPerioder = listOf(MånedPeriode(april2022, desember2022)),
            barna = listOf(barnFødtAugust2019)
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantPeriode.tom) }

        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = barnFyller18ÅrDato,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = utvidetAndeler.single()
        )

        assertEquals(1, småbarnstilleggAndeler.size)

        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = april2022,
                tom = august2022,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = småbarnstilleggAndeler.single()
        )

        // BARN
        assertEquals(1, barnasAndeler.size)
        testAtAndelErSomForventet(
            forventetAndel = ForventetAndel(
                fom = mars2022,
                tom = barnFyller6ÅrDato,
                prosent = BigDecimal(100)
            ),
            faktiskAndel = barnasAndeler.single()
        )
    }

    private data class ForventetAndel(
        val fom: YearMonth,
        val tom: YearMonth,
        val prosent: BigDecimal,
        val knyttetTilEndringsperiode: Boolean = false
    )

    private fun testAtAndelErSomForventet(forventetAndel: ForventetAndel, faktiskAndel: AndelTilkjentYtelse) {
        assertEquals(forventetAndel.fom, faktiskAndel.stønadFom)
        assertEquals(forventetAndel.tom, faktiskAndel.stønadTom)
        assertEquals(forventetAndel.prosent, faktiskAndel.prosent)
        if (forventetAndel.knyttetTilEndringsperiode) {
            assertTrue(faktiskAndel.endretUtbetalingAndeler.isNotEmpty())
        } else {
            assertTrue(faktiskAndel.endretUtbetalingAndeler.isEmpty())
        }
    }

    private data class EndretAndel(
        val fom: YearMonth,
        val tom: YearMonth,
        val person: Person,
        val årsak: Årsak,
        val skalUtbetales: Boolean
    )

    private fun settOppScenarioOgBeregnTilkjentYtelse(
        endretAndeler: List<EndretAndel> = emptyList(),
        atypiskeVilkårBarna: List<AtypiskVilkår> = emptyList(),
        atypiskeVilkårSøker: List<AtypiskVilkår> = emptyList(),
        barna: List<Person>,
        overgangsstønadPerioder: List<MånedPeriode>
    ): TilkjentYtelse {
        val vilkårsvurdering = lagVilkårsvurdering(
            søker = søker,
            barn = barna,
            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
            atypiskeVilkårBarna = atypiskeVilkårBarna,
            atypiskeVilkårSøker = atypiskeVilkårSøker
        )

        val endretUtbetalingAndeler = endretAndeler.map {
            lagEndretUtbetalingAndel(
                behandlingId = vilkårsvurdering.behandling.id,
                person = it.person,
                prosent = if (it.skalUtbetales) BigDecimal(100) else BigDecimal.ZERO,
                årsak = it.årsak,
                fom = it.fom,
                tom = it.tom
            )
        }

        val tilkjentYtelse = beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = lagPersonopplysningsgrunnlag(personer = barna.plus(søker), behandlingId = vilkårsvurdering.behandling.id),
            behandling = vilkårsvurdering.behandling,
            endretUtbetalingAndeler = endretUtbetalingAndeler
        ) { (_) ->
            lagOvergangsstønadPerioder(perioder = overgangsstønadPerioder, søkerIdent = søker.aktør.aktivFødselsnummer())
        }

        return tilkjentYtelse
    }

    private fun lagPersonopplysningsgrunnlag(personer: List<Person>, behandlingId: Long): PersonopplysningGrunnlag {
        return PersonopplysningGrunnlag(
            personer = personer.toMutableSet(),
            behandlingId = behandlingId
        )
    }

    private fun lagOvergangsstønadPerioder(perioder: List<MånedPeriode>, søkerIdent: String): List<InternPeriodeOvergangsstønad> {
        return perioder.map {
            InternPeriodeOvergangsstønad(
                søkerIdent,
                it.fom.førsteDagIInneværendeMåned(),
                it.tom.sisteDagIInneværendeMåned()
            )
        }
    }

    private data class AtypiskVilkår(
        val aktør: Aktør,
        val fom: LocalDate,
        val tom: LocalDate? = null,
        val resultat: Resultat = Resultat.OPPFYLT,
        val vilkårType: Vilkår,
        val utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering? = null
    )

    private fun lagVilkårResultat(
        personResultat: PersonResultat,
        fom: LocalDate,
        tom: LocalDate? = null,
        resultat: Resultat = Resultat.OPPFYLT,
        vilkårType: Vilkår,
        utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()
    ): VilkårResultat {
        return VilkårResultat(
            personResultat = personResultat,
            vilkårType = vilkårType,
            resultat = resultat,
            periodeFom = fom,
            periodeTom = tom,
            begrunnelse = "",
            behandlingId = personResultat.vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger
        )
    }

    private fun lagVilkårsvurdering(
        søker: Person,
        barn: List<Person>,
        ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
        atypiskeVilkårSøker: List<AtypiskVilkår> = emptyList(),
        atypiskeVilkårBarna: List<AtypiskVilkår> = emptyList()
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())

        val eldsteBarn = barn.minBy { it.fødselsdato }

        val søkerPersonResultat = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = søker,
            ytelseType = ytelseType,
            standardFom = eldsteBarn.fødselsdato,
            atypiskeVilkår = atypiskeVilkårSøker
        )

        val barnasPersonResultater = barn.map { barnet ->
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                person = barnet,
                ytelseType = ytelseType,
                standardFom = barnet.fødselsdato,
                atypiskeVilkår = atypiskeVilkårBarna.filter { it.aktør == barnet.aktør }
            )
        }

        vilkårsvurdering.personResultater = barnasPersonResultater.toSet().plus(søkerPersonResultat)
        return vilkårsvurdering
    }

    private fun lagPersonResultat(
        vilkårsvurdering: Vilkårsvurdering,
        person: Person,
        ytelseType: YtelseType,
        standardFom: LocalDate,
        atypiskeVilkår: List<AtypiskVilkår>
    ): PersonResultat {
        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = person.aktør
        )

        val vilkårForPersonType = Vilkår.hentVilkårFor(personType = person.type, ytelseType = ytelseType)

        val ordinæreVilkårResultater = vilkårForPersonType.map { vilkår ->
            lagVilkårResultat(
                personResultat = personResultat,
                vilkårType = vilkår,
                resultat = Resultat.OPPFYLT,
                fom = standardFom,
                tom = if (vilkår == Vilkår.UNDER_18_ÅR) person.fødselsdato.til18ÅrsVilkårsdato() else null
            )
        }

        val atypiskeVilkårTyper = atypiskeVilkår.map { it.vilkårType }

        val oppdaterteVilkårResultater = ordinæreVilkårResultater
            .filter { it.vilkårType !in atypiskeVilkårTyper }
            .plus(
                atypiskeVilkår.map {
                    lagVilkårResultat(
                        personResultat = personResultat,
                        fom = it.fom,
                        tom = it.tom,
                        vilkårType = it.vilkårType,
                        resultat = it.resultat,
                        utdypendeVilkårsvurderinger = if (it.utdypendeVilkårsvurdering != null) listOf(it.utdypendeVilkårsvurdering) else emptyList()
                    )
                }
            )

        personResultat.setSortedVilkårResultater(oppdaterteVilkårResultater.toSet())
        return personResultat
    }
}
