package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIForrigeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class TilkjentYtelseUtilsTest {

    @Test
    fun `Barn som er under 6 år hele perioden får tillegg hele perioden`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2023, 1, 1)

        assertEquals(periode, SatsService.hentPeriodeTil6år(seksårsdag, periode.fom, periode.tom))
    }

    @Test
    fun `Barn som fyller 6 år i løpet av perioden får tilleggsperiode før seksårsdag`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2021, 1, 1)

        assertEquals(
            Periode(periode.fom, seksårsdag.sisteDagIForrigeMåned()),
            SatsService.hentPeriodeTil6år(seksårsdag, periode.fom, periode.tom)
        )
    }

    @Test
    fun `Barn som er over 6 år hele perioden får ingen tillegsperiode`() {
        val periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2022, 1, 1))
        val seksårsdag = LocalDate.of(2018, 1, 1)

        assertEquals(null, SatsService.hentPeriodeTil6år(seksårsdag, periode.fom, periode.tom))
    }

    @Test
    fun `Barn som fyller 6 år i det vilkårene er oppfylt får andel måneden etter`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnSeksårsdag
            )

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
        val barnFødselsdato = LocalDate.of(2016, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val vilkårOppfyltFom = barnSeksårsdag.minusMonths(2)
        val vilkårOppfyltTom = barnSeksårsdag.plusDays(2)
        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = vilkårOppfyltFom,
                vilkårOppfyltTom = vilkårOppfyltTom
            )

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
        assertEquals(1654, andelTilkjentYtelseFør6År.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()
        assertEquals(
            MånedPeriode(barnSeksårsdag.toYearMonth(), barnSeksårsdag.toYearMonth()),
            MånedPeriode(andelTilkjentYtelseEtter6År.stønadFom, andelTilkjentYtelseEtter6År.stønadTom)
        )
        assertEquals(1054, andelTilkjentYtelseEtter6År.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `1 barn får normal utbetaling med satsendring fra september 2020 og september 2021`() {
        val barnFødselsdato = LocalDate.of(2021, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato
            )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            behandling = lagBehandling()
        )
            .andelerTilkjentYtelse
            .toList()
            .sortedBy { it.stønadFom }

        assertEquals(3, andeler.size)

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
            MånedPeriode(YearMonth.of(2021, 9), barnSeksårsdag.forrigeMåned()),
            MånedPeriode(
                andelTilkjentYtelseFør6ÅrSeptember2021.stønadFom,
                andelTilkjentYtelseFør6ÅrSeptember2021.stønadTom
            )
        )
        assertEquals(1654, andelTilkjentYtelseFør6ÅrSeptember2021.kalkulertUtbetalingsbeløp)

        val andelTilkjentYtelseEtter6År = andeler[2]
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
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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

        val andelTilkjentYtelseEtter6År = andeler[2]
        assertEquals(527, andelTilkjentYtelseEtter6År.kalkulertUtbetalingsbeløp)
    }

    @Test
    fun `Skal opprette riktig tilkjent ytelse-perioder for back-to-back perioder på barnet innenfor en måned`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 5)

        val (vilkårsvurdering, personopplysningGrunnlag) =
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 17),
            backToBackFom = LocalDate.of(2019, 8, 18)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 17),
            backToBackFom = LocalDate.of(2019, 8, 18)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 31),
            backToBackFom = LocalDate.of(2019, 9, 1)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 31),
            backToBackFom = LocalDate.of(2019, 9, 1)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 31),
            backToBackFom = LocalDate.of(2019, 9, 2)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 31),
            backToBackFom = LocalDate.of(2019, 9, 2)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { !it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 1),
            backToBackFom = LocalDate.of(2019, 8, 31)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            genererBehandlingResultatOgPersonopplysningGrunnlag(
                barnFødselsdato = barnFødselsdato,
                vilkårOppfyltFom = barnFødselsdato,
                erDeltBosted = true
            )

        val oppdatertVilkårsvurdering = oppdaterBosattIRiketMedBack2BackPerioder(
            vilkårsvurdering = vilkårsvurdering,
            personResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!,
            barnFødselsdato = barnFødselsdato,
            backToBackTom = LocalDate.of(2019, 8, 1),
            backToBackFom = LocalDate.of(2019, 8, 31)
        )

        val andeler = TilkjentYtelseUtils.beregnTilkjentYtelse(
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
            vilkårsvurdering.personResultater.filter { it.personIdent != personResultat.personIdent }.toSet() + setOf(
            personResultat
        )

        return vilkårsvurdering
    }

    private fun genererBehandlingResultatOgPersonopplysningGrunnlag(
        barnFødselsdato: LocalDate,
        vilkårOppfyltFom: LocalDate,
        vilkårOppfyltTom: LocalDate? = barnFødselsdato.plusYears(18),
        erDeltBosted: Boolean = false
    ): Pair<Vilkårsvurdering, PersonopplysningGrunnlag> {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val behandling = lagBehandling()

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerFnr = søkerFnr,
            behandling = behandling,
            resultat = Resultat.OPPFYLT,
            søkerPeriodeFom = LocalDate.of(2014, 1, 1),
            søkerPeriodeTom = null
        )

        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)
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
                    periodeTom = barnFødselsdato.plusYears(18),
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
                    erDeltBosted = erDeltBosted,
                    behandlingId = behandling.id
                )
            )
        )

        vilkårsvurdering.personResultater = setOf(vilkårsvurdering.personResultater.first(), barnResultat)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
        val barn = Person(
            aktørId = randomAktørId(),
            personIdent = PersonIdent(barnFnr),
            type = PersonType.BARN,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = barnFødselsdato,
            navn = "Barn",
            kjønn = Kjønn.MANN
        )
            .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }
        val søker = Person(
            aktørId = randomAktørId(),
            personIdent = PersonIdent(søkerFnr),
            type = PersonType.SØKER,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = barnFødselsdato.minusYears(20),
            navn = "Barn",
            kjønn = Kjønn.MANN
        )
            .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }
        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.add(barn)

        return Pair(vilkårsvurdering, personopplysningGrunnlag)
    }
}
