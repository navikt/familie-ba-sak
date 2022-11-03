package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeMedBegrunnelser

import hentPerioderMedUtbetaling
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class UtbetalingsperiodeUtilTest {

    @Test
    fun `Skal beholde split i andel tilkjent ytelse`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juli2020 = YearMonth.of(2020, 7)

        val person1 = lagPerson()
        val person2 = lagPerson()

        val vedtak = lagVedtak()

        val andelPerson1MarsTilApril = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mars2020,
            tom = april2020,
            beløp = 1000,
            person = person1
        )

        val andelPerson1MaiTilJuli = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mai2020,
            tom = juli2020,
            beløp = 1000,
            person = person1
        )

        val andelPerson2MarsTilJuli = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mars2020,
            tom = juli2020,
            beløp = 1000,
            person = person2
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mai2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            )
        )

        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val personResultater = setOf(
            vilkårsvurdering.lagGodkjentPersonResultatForBarn(person1),
            vilkårsvurdering.lagGodkjentPersonResultatForBarn(person2)
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            listOf(andelPerson1MarsTilApril, andelPerson1MaiTilJuli, andelPerson2MarsTilJuli),
            vedtak,
            personResultater
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) }
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet()
        )
    }

    @Test
    fun `Skal splitte på forskjellige personer`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juni2020 = YearMonth.of(2020, 6)
        val juli2020 = YearMonth.of(2020, 7)

        val person1 = lagPerson(type = PersonType.BARN)
        val person2 = lagPerson(type = PersonType.BARN)

        val vedtak = lagVedtak()

        val andelPerson1MarsTilMai = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mars2020,
            tom = mai2020,
            beløp = 1000,
            person = person1
        )

        val andelPerson2MaiTilJuli = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mai2020,
            tom = juli2020,
            beløp = 1000,
            person = person2
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mai2020.førsteDagIInneværendeMåned(),
                tom = mai2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = juni2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            )
        )

        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val personResultater = setOf(
            vilkårsvurdering.lagGodkjentPersonResultatForBarn(person1),
            vilkårsvurdering.lagGodkjentPersonResultatForBarn(person2)
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            listOf(andelPerson1MarsTilMai, andelPerson2MaiTilJuli),
            vedtak,
            personResultater
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) }
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet()
        )
    }

    @Test
    fun `Skal splitte på utdypende vilkårsvurdering når det flytter seg fra ett barn til et annet`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juli2020 = YearMonth.of(2020, 7)

        val søker = lagPerson()
        val barn1 = lagPerson()
        val barn2 = lagPerson()

        val vedtak = lagVedtak()

        val andelBarn1MarsTilJuli = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mars2020,
            tom = juli2020,
            beløp = 1000,
            person = barn1
        )

        val andelBarn2MarsTilJuli = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mars2020,
            tom = juli2020,
            beløp = 2000,
            person = barn2
        )

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultatBarn1 = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn1.aktør
        )

        val vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn1 = VilkårResultat(
            personResultat = personResultatBarn1,
            periodeFom = mars2020.minusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = april2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            behandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_SØKER)
        )
        val vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn1 = VilkårResultat(
            personResultat = personResultatBarn1,
            periodeFom = mai2020.førsteDagIInneværendeMåned(),
            periodeTom = juli2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            behandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = emptyList()
        )
        val vilkårResultaterBarn1 = listOf(
            vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn1,
            vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn1
        )

        personResultatBarn1.setSortedVilkårResultater(
            vilkårResultaterBarn1.toSet()
        )

        val personResultatBarn2 = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn2.aktør
        )

        val vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn2 = VilkårResultat(
            personResultat = personResultatBarn2,
            periodeFom = mars2020.minusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = april2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            behandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = emptyList()
        )
        val vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn2 = VilkårResultat(
            personResultat = personResultatBarn2,
            periodeFom = mai2020.førsteDagIInneværendeMåned(),
            periodeTom = juli2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            behandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_SØKER)
        )

        val vilkårResultaterBarn2 = listOf(
            vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn2,
            vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn2
        )

        personResultatBarn2.setSortedVilkårResultater(
            vilkårResultaterBarn2.toSet()
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mai2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            )
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            listOf(andelBarn1MarsTilJuli, andelBarn2MarsTilJuli),
            vedtak,
            setOf(personResultatBarn1, personResultatBarn2)
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) }
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet()
        )
    }

    @Test
    fun `Skal få med opphør i andel tilkjent ytelse`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val juli2020 = YearMonth.of(2020, 7)
        val barn1 = lagPerson()
        val vedtak = lagVedtak()

        val andelBarn1MarsTilApril = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mars2020,
            tom = april2020,
            beløp = 1000,
            person = barn1
        )
        val andelBarn1JuliTilJuli = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = juli2020,
            tom = juli2020,
            beløp = 1000,
            person = barn1
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            listOf(andelBarn1MarsTilApril, andelBarn1JuliTilJuli),
            vedtak,
            emptySet()
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = juli2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            )
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) }
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet()
        )
    }

    @Test
    fun `Skal lage splitt i vedtaksperioder med der ulikt regelverk er brukt`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juli2020 = YearMonth.of(2020, 7)

        val søker = lagPerson()
        val barn = lagPerson()

        val vedtak = lagVedtak()

        val andelBarnMarsTilJuli = lagAndelTilkjentYtelseMedEndreteUtbetalinger(
            fom = mars2020,
            tom = juli2020,
            beløp = 1000,
            person = barn
        )

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT
        )

        val personResultatBarn = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn.aktør
        )

        val vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn1 = VilkårResultat(
            personResultat = personResultatBarn,
            periodeFom = mars2020.minusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = april2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            behandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = emptyList(),
            vurderesEtter = Regelverk.NASJONALE_REGLER
        )
        val vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn1 = VilkårResultat(
            personResultat = personResultatBarn,
            periodeFom = mai2020.førsteDagIInneværendeMåned(),
            periodeTom = juli2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            behandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = emptyList(),
            vurderesEtter = Regelverk.EØS_FORORDNINGEN
        )
        val vilkårResultaterBarn1 = listOf(
            vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn1,
            vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn1
        )

        personResultatBarn.setSortedVilkårResultater(
            vilkårResultaterBarn1.toSet()
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mai2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf()
            )
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            listOf(andelBarnMarsTilJuli),
            vedtak,
            setOf(personResultatBarn)
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) }
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet()
        )
    }

    private fun Vilkårsvurdering.lagGodkjentPersonResultatForBarn(person: Person) = lagPersonResultat(
        vilkårsvurdering = this,
        aktør = person.aktør,
        resultat = Resultat.OPPFYLT,
        periodeFom = person.fødselsdato,
        periodeTom = person.fødselsdato.til18ÅrsVilkårsdato(),
        lagFullstendigVilkårResultat = true,
        personType = person.type
    )
}
