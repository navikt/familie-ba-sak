package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultat
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

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

        assertEquals(Periode(periode.fom, seksårsdag.sisteDagIForrigeMåned()),
                     SatsService.hentPeriodeTil6år(seksårsdag, periode.fom, periode.tom))
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

        val (behandlingResultat, personopplysningGrunnlag) =
                genererBehandlingResultatOgPersonopplysningGrunnlag(barnFødselsdato = barnFødselsdato,
                                                                    vilkårOppfyltFom = barnSeksårsdag)

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(behandlingResultat = behandlingResultat,
                                                                      personopplysningGrunnlag = personopplysningGrunnlag)

        assertEquals(1, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(MånedPeriode(barnSeksårsdag.nesteMåned(),
                                  barnFødselsdato.plusYears(18).forrigeMåned()),
                     MånedPeriode(andelTilkjentYtelse.stønadFom, andelTilkjentYtelse.stønadTom))
    }

    @Test
    fun `Barn som fyller 6 år i det vilkårene ikke lenger er oppfylt får andel den måneden også`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val vilkårOppfyltFom = barnSeksårsdag.minusMonths(2)
        val vilkårOppfyltTom = barnSeksårsdag.plusDays(2)
        val (behandlingResultat, personopplysningGrunnlag) =
                genererBehandlingResultatOgPersonopplysningGrunnlag(barnFødselsdato = barnFødselsdato,
                                                                    vilkårOppfyltFom = vilkårOppfyltFom,
                                                                    vilkårOppfyltTom = vilkårOppfyltTom)

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(behandlingResultat = behandlingResultat,
                                                                      personopplysningGrunnlag = personopplysningGrunnlag)

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelTilkjentYtelseFør6År = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(MånedPeriode(vilkårOppfyltFom.nesteMåned(), barnSeksårsdag.forrigeMåned()),
                     MånedPeriode(andelTilkjentYtelseFør6År.stønadFom, andelTilkjentYtelseFør6År.stønadTom))
        assertEquals(1354, andelTilkjentYtelseFør6År.beløp)

        val andelTilkjentYtelseEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()
        assertEquals(MånedPeriode(barnSeksårsdag.toYearMonth(), barnSeksårsdag.toYearMonth()),
                     MånedPeriode(andelTilkjentYtelseEtter6År.stønadFom, andelTilkjentYtelseEtter6År.stønadTom))
        assertEquals(1054, andelTilkjentYtelseEtter6År.beløp)
    }

    @Test
    fun `1 barn får normal utbetaling med satsendring fra september 2020`() {
        val barnFødselsdato = LocalDate.of(2021, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val vilkårOppfyltFom = barnFødselsdato
        val (behandlingResultat, personopplysningGrunnlag) =
                genererBehandlingResultatOgPersonopplysningGrunnlag(barnFødselsdato = barnFødselsdato,
                                                                    vilkårOppfyltFom = vilkårOppfyltFom)

        val tilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(behandlingResultat = behandlingResultat,
                                                                      personopplysningGrunnlag = personopplysningGrunnlag)

        assertEquals(2, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelTilkjentYtelseFør6År = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(MånedPeriode(vilkårOppfyltFom.nesteMåned(), barnSeksårsdag.forrigeMåned()),
                     MånedPeriode(andelTilkjentYtelseFør6År.stønadFom, andelTilkjentYtelseFør6År.stønadTom))
        assertEquals(1354, andelTilkjentYtelseFør6År.beløp)

        val andelTilkjentYtelseEtter6År = tilkjentYtelse.andelerTilkjentYtelse.last()
        assertEquals(MånedPeriode(barnSeksårsdag.toYearMonth(), barnFødselsdato.plusYears(18).forrigeMåned()),
                     MånedPeriode(andelTilkjentYtelseEtter6År.stønadFom, andelTilkjentYtelseEtter6År.stønadTom))
        assertEquals(1054, andelTilkjentYtelseEtter6År.beløp)
    }

    private fun genererBehandlingResultatOgPersonopplysningGrunnlag(barnFødselsdato: LocalDate,
                                                                    vilkårOppfyltFom: LocalDate,
                                                                    vilkårOppfyltTom: LocalDate? = barnFødselsdato.plusYears(18)): Pair<BehandlingResultat, PersonopplysningGrunnlag> {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()

        val behandling = lagBehandling()

        val behandlingResultat = lagBehandlingResultat(
                søkerFnr = søkerFnr,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                søkerPeriodeFom = LocalDate.of(2014, 1, 1),
                søkerPeriodeTom = null)

        val barnResultat = PersonResultat(behandlingResultat = behandlingResultat, personIdent = barnFnr)
        barnResultat.setVilkårResultater(setOf(
                VilkårResultat(personResultat = barnResultat,
                               vilkårType = Vilkår.BOSATT_I_RIKET,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = vilkårOppfyltFom,
                               periodeTom = vilkårOppfyltTom,
                               begrunnelse = "",
                               behandlingId = behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barnResultat,
                               vilkårType = Vilkår.UNDER_18_ÅR,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = barnFødselsdato,
                               periodeTom = barnFødselsdato.plusYears(18),
                               begrunnelse = "",
                               behandlingId = behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barnResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = barnFødselsdato,
                               periodeTom = null,
                               begrunnelse = "",
                               behandlingId = behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barnResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = barnFødselsdato,
                               periodeTom = null,
                               begrunnelse = "",
                               behandlingId = behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barnResultat,
                               vilkårType = Vilkår.BOR_MED_SØKER,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = barnFødselsdato,
                               periodeTom = null,
                               begrunnelse = "",
                               behandlingId = behandling.id,
                               regelInput = null,
                               regelOutput = null)
        ))

        behandlingResultat.personResultater = setOf(behandlingResultat.personResultater.first(), barnResultat)

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id)
        val barn = Person(aktørId = randomAktørId(),
                          personIdent = PersonIdent(barnFnr),
                          type = PersonType.BARN,
                          personopplysningGrunnlag = personopplysningGrunnlag,
                          fødselsdato = barnFødselsdato,
                          navn = "Barn",
                          kjønn = Kjønn.MANN,
                          sivilstand = SIVILSTAND.UGIFT)
        val søker = Person(aktørId = randomAktørId(),
                           personIdent = PersonIdent(søkerFnr),
                           type = PersonType.SØKER,
                           personopplysningGrunnlag = personopplysningGrunnlag,
                           fødselsdato = barnFødselsdato.minusYears(20),
                           navn = "Barn",
                           kjønn = Kjønn.MANN,
                           sivilstand = SIVILSTAND.UGIFT)
        personopplysningGrunnlag.personer.add(søker)
        personopplysningGrunnlag.personer.add(barn)

        return Pair(behandlingResultat, personopplysningGrunnlag)
    }
}