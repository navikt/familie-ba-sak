package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilyyyyMMdd
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndeler
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions.assertEquals
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
}
