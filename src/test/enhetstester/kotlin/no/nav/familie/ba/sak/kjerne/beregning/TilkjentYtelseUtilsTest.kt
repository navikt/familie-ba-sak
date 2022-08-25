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
    val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(3))

    val relevantTidsperiode = MånedPeriode(YearMonth.now().minusMonths(7), YearMonth.now().plusMonths(5))
    val overgangsstønadPeriode = relevantTidsperiode.copy(tom = YearMonth.now().plusMonths(4))
    val langEndringsperiode = MånedPeriode(fom = YearMonth.now().minusMonths(4), tom = YearMonth.now().minusMonths(1))
    val kortEndringsperiode = MånedPeriode(fom = YearMonth.now().minusMonths(2), tom = YearMonth.now().minusMonths(1))

    @Test
    fun `Delt bosted - case 1`() {
        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = søker,
                    skalUtbetales = false,
                    årsak = Årsak.DELT_BOSTED,
                    fom = langEndringsperiode.fom,
                    tom = langEndringsperiode.tom
                ),
                EndretAndel(
                    person = barn,
                    skalUtbetales = false,
                    årsak = Årsak.DELT_BOSTED,
                    fom = langEndringsperiode.fom,
                    tom = langEndringsperiode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(5),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                    aktør = barn.aktør
                )
            )
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(6, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        assertEquals(2, barnasAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.fom, tom = langEndringsperiode.tom, prosent = BigDecimal.ZERO, knyttetTilEndringsperiode = true),
            faktiskAndel = barnasAndeler.first()
        )

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.tom.plusMonths(1), tom = barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), prosent = BigDecimal(50)),
            faktiskAndel = barnasAndeler.last()
        )

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(2, utvidetAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.fom, tom = langEndringsperiode.tom, prosent = BigDecimal.ZERO, knyttetTilEndringsperiode = true),
            faktiskAndel = utvidetAndeler.first()
        )

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.tom.plusMonths(1), tom = barn.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(), prosent = BigDecimal(50)),
            faktiskAndel = utvidetAndeler.last()
        )

        assertEquals(2, småbarnstilleggAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.fom, tom = langEndringsperiode.tom, prosent = BigDecimal.ZERO),
            faktiskAndel = småbarnstilleggAndeler.first()
        )

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.tom.plusMonths(1), tom = barn.fødselsdato.plusYears(3).toYearMonth(), prosent = BigDecimal(100)),
            faktiskAndel = småbarnstilleggAndeler.last()
        )
    }

    @Test
    fun `Delt bosted - case 2`() {
        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = barn,
                    skalUtbetales = false,
                    årsak = Årsak.DELT_BOSTED,
                    fom = langEndringsperiode.fom,
                    tom = langEndringsperiode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(5),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED,
                    aktør = barn.aktør
                )
            )
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(4, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        assertEquals(2, barnasAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.fom, tom = langEndringsperiode.tom, prosent = BigDecimal.ZERO, knyttetTilEndringsperiode = true),
            faktiskAndel = barnasAndeler.first()
        )

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.tom.plusMonths(1), tom = barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), prosent = BigDecimal(50)),
            faktiskAndel = barnasAndeler.last()
        )

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.fom, tom = barn.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(), prosent = BigDecimal(50)),
            faktiskAndel = utvidetAndeler.single()
        )

        assertEquals(1, småbarnstilleggAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = langEndringsperiode.fom, tom = barn.fødselsdato.plusYears(3).toYearMonth(), prosent = BigDecimal(100)),
            faktiskAndel = småbarnstilleggAndeler.single()
        )
    }

    @Test
    fun `Delt bosted - case 3`() {
        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = barn,
                    skalUtbetales = true,
                    årsak = Årsak.DELT_BOSTED,
                    fom = kortEndringsperiode.fom,
                    tom = kortEndringsperiode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(5),
                    tom = LocalDate.now().minusMonths(4).sisteDagIMåned(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør
                ),
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(3).førsteDagIInneværendeMåned(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(3),
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            )
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(5, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        assertEquals(3, barnasAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = YearMonth.now().minusMonths(4), tom = kortEndringsperiode.fom.minusMonths(1), prosent = BigDecimal(100)),
            faktiskAndel = barnasAndeler[0]
        )

        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.fom, tom = kortEndringsperiode.tom, prosent = BigDecimal(100), knyttetTilEndringsperiode = true),
            faktiskAndel = barnasAndeler[1]
        )

        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.tom.plusMonths(1), tom = barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), prosent = BigDecimal(50)),
            faktiskAndel = barnasAndeler[2]
        )

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.fom, tom = barn.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(), prosent = BigDecimal(50)),
            faktiskAndel = utvidetAndeler.single()
        )

        assertEquals(1, småbarnstilleggAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.fom, tom = barn.fødselsdato.plusYears(3).toYearMonth(), prosent = BigDecimal(100)),
            faktiskAndel = småbarnstilleggAndeler.single()
        )
    }

    @Test
    fun `Delt bosted - case 4`() {
        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = barn,
                    skalUtbetales = true,
                    årsak = Årsak.DELT_BOSTED,
                    fom = kortEndringsperiode.fom,
                    tom = kortEndringsperiode.tom
                ),
                EndretAndel(
                    person = søker,
                    skalUtbetales = true,
                    årsak = Årsak.DELT_BOSTED,
                    fom = kortEndringsperiode.fom,
                    tom = kortEndringsperiode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(5),
                    tom = LocalDate.now().minusMonths(4).sisteDagIMåned(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør
                ),
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(3).førsteDagIInneværendeMåned(),
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(3),
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            )
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(6, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        assertEquals(3, barnasAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = YearMonth.now().minusMonths(4), tom = kortEndringsperiode.fom.minusMonths(1), prosent = BigDecimal(100)),
            faktiskAndel = barnasAndeler[0]
        )
        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.fom, tom = kortEndringsperiode.tom, prosent = BigDecimal(100), knyttetTilEndringsperiode = true),
            faktiskAndel = barnasAndeler[1]
        )

        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.tom.plusMonths(1), tom = barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), prosent = BigDecimal(50)),
            faktiskAndel = barnasAndeler[2]
        )

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(2, utvidetAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.fom, tom = kortEndringsperiode.tom, prosent = BigDecimal(100), knyttetTilEndringsperiode = true),
            faktiskAndel = utvidetAndeler.first()
        )

        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.tom.plusMonths(1), tom = barn.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(), prosent = BigDecimal(50)),
            faktiskAndel = utvidetAndeler.last()
        )

        assertEquals(1, småbarnstilleggAndeler.size)

        testPeriode(
            forventetAndel = ForventetAndel(fom = kortEndringsperiode.fom, tom = barn.fødselsdato.plusYears(3).toYearMonth(), prosent = BigDecimal(100)),
            faktiskAndel = småbarnstilleggAndeler.single()
        )
    }

    @Test
    fun `Etterbetaling 3 år - case 1`() {
        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            endretAndeler = listOf(
                EndretAndel(
                    person = barn,
                    skalUtbetales = false,
                    årsak = Årsak.ETTERBETALING_3ÅR,
                    fom = langEndringsperiode.fom,
                    tom = langEndringsperiode.tom
                ),
                EndretAndel(
                    person = søker,
                    skalUtbetales = false,
                    årsak = Årsak.ETTERBETALING_3ÅR,
                    fom = langEndringsperiode.fom,
                    tom = langEndringsperiode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(5),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør
                )
            )
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(6, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        assertEquals(2, barnasAndeler.size)

        val barnetsAndel1 = barnasAndeler[0]
        val barnetsAndel2 = barnasAndeler[1]

        assertEquals(langEndringsperiode.fom, barnetsAndel1.stønadFom)
        assertEquals(langEndringsperiode.tom, barnetsAndel1.stønadTom)
        assertEquals(BigDecimal.ZERO, barnetsAndel1.prosent)
        assertTrue(barnetsAndel1.endretUtbetalingAndeler.isNotEmpty())

        assertEquals(langEndringsperiode.tom.plusMonths(1), barnetsAndel2.stønadFom)
        assertEquals(barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), barnetsAndel2.stønadTom)
        assertEquals(BigDecimal(100), barnetsAndel2.prosent)
        assertTrue(barnetsAndel2.endretUtbetalingAndeler.isEmpty())

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(2, utvidetAndeler.size)

        val utvidetAndel1 = utvidetAndeler.first()
        val utvidetAndel2 = utvidetAndeler.last()

        assertEquals(langEndringsperiode.fom, utvidetAndel1.stønadFom)
        assertEquals(langEndringsperiode.tom, utvidetAndel1.stønadTom)
        assertEquals(BigDecimal.ZERO, utvidetAndel1.prosent)
        assertTrue(utvidetAndel1.endretUtbetalingAndeler.isNotEmpty())

        assertEquals(langEndringsperiode.tom.plusMonths(1), utvidetAndel2.stønadFom)
        assertEquals(barn.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(), utvidetAndel2.stønadTom)
        assertEquals(BigDecimal(100), utvidetAndel2.prosent)
        assertTrue(utvidetAndel2.endretUtbetalingAndeler.isEmpty())

        assertEquals(2, småbarnstilleggAndeler.size)

        val småbarnstilleggAndel1 = småbarnstilleggAndeler.first()
        val småbarnstilleggAndel2 = småbarnstilleggAndeler.last()

        assertEquals(langEndringsperiode.fom, småbarnstilleggAndel1.stønadFom)
        assertEquals(langEndringsperiode.tom, småbarnstilleggAndel1.stønadTom)
        assertEquals(BigDecimal.ZERO, småbarnstilleggAndel1.prosent)
        assertTrue(småbarnstilleggAndel1.endretUtbetalingAndeler.isEmpty())

        assertEquals(langEndringsperiode.tom.plusMonths(1), småbarnstilleggAndel2.stønadFom)
        assertEquals(barn.fødselsdato.plusYears(3).toYearMonth(), småbarnstilleggAndel2.stønadTom)
        assertEquals(BigDecimal(100), småbarnstilleggAndel2.prosent)
        assertTrue(småbarnstilleggAndel2.endretUtbetalingAndeler.isEmpty())
    }

    @Test
    fun `Etterbetaling 3 år - case 2`() {
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(12))
        val utvidetVilkårStart = LocalDate.now().minusMonths(6)

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            barna = listOf(barn, barn2),
            endretAndeler = listOf(
                EndretAndel(
                    person = barn,
                    skalUtbetales = false,
                    årsak = Årsak.ETTERBETALING_3ÅR,
                    fom = langEndringsperiode.fom,
                    tom = langEndringsperiode.tom
                )
            ),
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = LocalDate.now().minusMonths(5),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør
                ),
                AtypiskVilkår(
                    fom = relevantTidsperiode.fom.førsteDagIInneværendeMåned(),
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn2.aktør,
                    utdypendeVilkårsvurdering = UtdypendeVilkårsvurdering.DELT_BOSTED
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = utvidetVilkårStart,
                    tom = null,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            )
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(7, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        val (barn1Andeler, barn2Andeler) = barnasAndeler.partition { it.aktør == barn.aktør }

        assertEquals(2, barn1Andeler.size)

        val barn1Andel1 = barn1Andeler[0]
        val barn1Andel2 = barn1Andeler[1]

        assertEquals(langEndringsperiode.fom, barn1Andel1.stønadFom)
        assertEquals(langEndringsperiode.tom, barn1Andel1.stønadTom)
        assertEquals(BigDecimal.ZERO, barn1Andel1.prosent)
        assertTrue(barn1Andel1.endretUtbetalingAndeler.isNotEmpty())

        assertEquals(langEndringsperiode.tom.plusMonths(1), barn1Andel2.stønadFom)
        assertEquals(barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), barn1Andel2.stønadTom)
        assertEquals(BigDecimal(100), barn1Andel2.prosent)
        assertTrue(barn1Andel2.endretUtbetalingAndeler.isEmpty())

        assertEquals(1, barn2Andeler.size)

        val barn2andel = barn2Andeler.single()

        assertEquals(relevantTidsperiode.fom.plusMonths(1), barn2andel.stønadFom)
        assertEquals(barn2.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(), barn2andel.stønadTom)
        assertEquals(BigDecimal(50), barn2andel.prosent)
        assertTrue(barn2andel.endretUtbetalingAndeler.isEmpty())

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(2, utvidetAndeler.size)

        val utvidetAndel1 = utvidetAndeler.first()
        val utvidetAndel2 = utvidetAndeler.last()

        assertEquals(utvidetVilkårStart.plusMonths(1).toYearMonth(), utvidetAndel1.stønadFom)
        assertEquals(langEndringsperiode.tom, utvidetAndel1.stønadTom)
        assertEquals(BigDecimal(50), utvidetAndel1.prosent)
        assertTrue(utvidetAndel1.endretUtbetalingAndeler.isEmpty())

        assertEquals(langEndringsperiode.tom.plusMonths(1), utvidetAndel2.stønadFom)
        assertEquals(barn.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(), utvidetAndel2.stønadTom)
        assertEquals(BigDecimal(100), utvidetAndel2.prosent)
        assertTrue(utvidetAndel2.endretUtbetalingAndeler.isEmpty())

        assertEquals(2, småbarnstilleggAndeler.size)

        val småbarnstilleggAndel1 = småbarnstilleggAndeler.first()
        val småbarnstilleggAndel2 = småbarnstilleggAndeler.last()

        assertEquals(langEndringsperiode.fom, småbarnstilleggAndel1.stønadFom)
        assertEquals(langEndringsperiode.tom, småbarnstilleggAndel1.stønadTom)
        assertEquals(BigDecimal.ZERO, småbarnstilleggAndel1.prosent)
        assertTrue(småbarnstilleggAndel1.endretUtbetalingAndeler.isEmpty())

        assertEquals(langEndringsperiode.tom.plusMonths(1), småbarnstilleggAndel2.stønadFom)
        assertEquals(barn.fødselsdato.plusYears(3).toYearMonth(), småbarnstilleggAndel2.stønadTom)
        assertEquals(BigDecimal(100), småbarnstilleggAndel2.prosent)
        assertTrue(småbarnstilleggAndel2.endretUtbetalingAndeler.isEmpty())
    }

    @Test
    fun `Småbarnstillegg - case 1`() {
        val osPeriode = MånedPeriode(fom = YearMonth.now().minusMonths(4), tom = YearMonth.now().minusMonths(2))
        val borMedSøkerStart = LocalDate.now().minusMonths(6)

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = borMedSøkerStart,
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = relevantTidsperiode.fom.førsteDagIInneværendeMåned(),
                    tom = LocalDate.now(),
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            ),
            osPerioder = listOf(osPeriode)
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        assertEquals(1, barnasAndeler.size)

        val barnetsAndel = barnasAndeler.single()

        assertEquals(borMedSøkerStart.plusMonths(1).toYearMonth(), barnetsAndel.stønadFom)
        assertEquals(barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), barnetsAndel.stønadTom)
        assertEquals(BigDecimal(100), barnetsAndel.prosent)
        assertTrue(barnetsAndel.endretUtbetalingAndeler.isEmpty())

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)

        val utvidetAndel = utvidetAndeler.single()

        assertEquals(borMedSøkerStart.plusMonths(1).toYearMonth(), utvidetAndel.stønadFom)
        assertEquals(YearMonth.now(), utvidetAndel.stønadTom)
        assertEquals(BigDecimal(100), utvidetAndel.prosent)
        assertTrue(utvidetAndel.endretUtbetalingAndeler.isEmpty())

        assertEquals(1, småbarnstilleggAndeler.size)

        val småbarnstilleggAndel = småbarnstilleggAndeler.first()

        assertEquals(osPeriode.fom, småbarnstilleggAndel.stønadFom)
        assertEquals(osPeriode.tom, småbarnstilleggAndel.stønadTom)
        assertEquals(BigDecimal(100), småbarnstilleggAndel.prosent)
        assertTrue(småbarnstilleggAndel.endretUtbetalingAndeler.isEmpty())
    }

    @Test
    fun `Småbarnstillegg - case 2`() {
        val osPeriode = MånedPeriode(fom = YearMonth.now().minusMonths(4), tom = YearMonth.now())
        val borMedSøkerStart = LocalDate.now().minusMonths(6)

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = borMedSøkerStart,
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør
                )
            ),
            atypiskeVilkårSøker = listOf(
                AtypiskVilkår(
                    fom = relevantTidsperiode.fom.førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().minusMonths(1),
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    aktør = søker.aktør
                )
            ),
            osPerioder = listOf(osPeriode)
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        assertEquals(1, barnasAndeler.size)

        val barnetsAndel = barnasAndeler.single()

        assertEquals(borMedSøkerStart.plusMonths(1).toYearMonth(), barnetsAndel.stønadFom)
        assertEquals(barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), barnetsAndel.stønadTom)
        assertEquals(BigDecimal(100), barnetsAndel.prosent)
        assertTrue(barnetsAndel.endretUtbetalingAndeler.isEmpty())

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)

        val utvidetAndel = utvidetAndeler.single()

        assertEquals(borMedSøkerStart.plusMonths(1).toYearMonth(), utvidetAndel.stønadFom)
        assertEquals(YearMonth.now().minusMonths(1), utvidetAndel.stønadTom)
        assertEquals(BigDecimal(100), utvidetAndel.prosent)
        assertTrue(utvidetAndel.endretUtbetalingAndeler.isEmpty())

        assertEquals(1, småbarnstilleggAndeler.size)

        val småbarnstilleggAndel = småbarnstilleggAndeler.first()

        assertEquals(osPeriode.fom, småbarnstilleggAndel.stønadFom)
        assertEquals(YearMonth.now().minusMonths(1), småbarnstilleggAndel.stønadTom)
        assertEquals(BigDecimal(100), småbarnstilleggAndel.prosent)
        assertTrue(småbarnstilleggAndel.endretUtbetalingAndeler.isEmpty())
    }

    @Test
    fun `Småbarnstillegg - case 3`() {
        val osPeriode = MånedPeriode(fom = YearMonth.now().minusMonths(4), tom = YearMonth.now().plusMonths(4))
        val borMedSøkerStart = LocalDate.now().minusMonths(6)

        val tilkjentYtelse = settOppScenarioOgBeregnTilkjentYtelse(
            atypiskeVilkårBarna = listOf(
                AtypiskVilkår(
                    fom = borMedSøkerStart,
                    tom = null,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    aktør = barn.aktør
                )
            ),
            osPerioder = listOf(osPeriode)
        )

        val andelerTilkjentYtelseITidsrom = tilkjentYtelse.andelerTilkjentYtelse.filter { it.stønadFom.isSameOrBefore(relevantTidsperiode.tom) }

        assertEquals(3, andelerTilkjentYtelseITidsrom.size)

        val (søkersAndeler, barnasAndeler) = andelerTilkjentYtelseITidsrom.partition { it.erSøkersAndel() }

        // BARN
        assertEquals(1, barnasAndeler.size)

        val barnetsAndel = barnasAndeler.single()

        assertEquals(borMedSøkerStart.plusMonths(1).toYearMonth(), barnetsAndel.stønadFom)
        assertEquals(barn.fødselsdato.plusYears(6).minusMonths(1).toYearMonth(), barnetsAndel.stønadTom)
        assertEquals(BigDecimal(100), barnetsAndel.prosent)
        assertTrue(barnetsAndel.endretUtbetalingAndeler.isEmpty())

        // SØKER
        val (utvidetAndeler, småbarnstilleggAndeler) = søkersAndeler.partition { it.erUtvidet() }

        assertEquals(1, utvidetAndeler.size)

        val utvidetAndel = utvidetAndeler.single()

        assertEquals(borMedSøkerStart.plusMonths(1).toYearMonth(), utvidetAndel.stønadFom)
        assertEquals(barn.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(), utvidetAndel.stønadTom)
        assertEquals(BigDecimal(100), utvidetAndel.prosent)
        assertTrue(utvidetAndel.endretUtbetalingAndeler.isEmpty())

        assertEquals(1, småbarnstilleggAndeler.size)

        val småbarnstilleggAndel = småbarnstilleggAndeler.first()

        assertEquals(osPeriode.fom, småbarnstilleggAndel.stønadFom)
        assertEquals(barn.fødselsdato.plusYears(3).toYearMonth(), småbarnstilleggAndel.stønadTom)
        assertEquals(BigDecimal(100), småbarnstilleggAndel.prosent)
        assertTrue(småbarnstilleggAndel.endretUtbetalingAndeler.isEmpty())
    }

    private data class ForventetAndel(
        val fom: YearMonth,
        val tom: YearMonth,
        val prosent: BigDecimal,
        val knyttetTilEndringsperiode: Boolean = false
    )

    private fun testPeriode(forventetAndel: ForventetAndel, faktiskAndel: AndelTilkjentYtelse) {
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
        barna: List<Person> = listOf(barn),
        osPerioder: List<MånedPeriode> = listOf(overgangsstønadPeriode)
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
            lagOvergangsstønadPerioder(perioder = osPerioder, søkerIdent = søker.aktør.aktivFødselsnummer())
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

    private fun lagVilkårResultat(personResultat: PersonResultat, fom: LocalDate, tom: LocalDate? = null, resultat: Resultat = Resultat.OPPFYLT, vilkårType: Vilkår, utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()): VilkårResultat {
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
            lagVilkårResultat(personResultat = personResultat, vilkårType = vilkår, resultat = Resultat.OPPFYLT, fom = standardFom, tom = if (vilkår == Vilkår.UNDER_18_ÅR) person.fødselsdato.til18ÅrsVilkårsdato() else null)
        }

        val atypiskeVilkårTyper = atypiskeVilkår.map { it.vilkårType }

        val oppdaterteVilkårResultater = ordinæreVilkårResultater
            .filter { it.vilkårType !in atypiskeVilkårTyper }
            .plus(atypiskeVilkår.map { lagVilkårResultat(personResultat = personResultat, fom = it.fom, tom = it.tom, vilkårType = it.vilkårType, resultat = it.resultat, utdypendeVilkårsvurderinger = if (it.utdypendeVilkårsvurdering != null) listOf(it.utdypendeVilkårsvurdering) else emptyList()) })

        personResultat.setSortedVilkårResultater(oppdaterteVilkårResultater.toSet())
        return personResultat
    }
}
