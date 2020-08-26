package no.nav.familie.ba.sak.common

import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.nare.core.evaluations.Resultat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class TidTest {

    @Test
    fun `skal finne siste dag i måneden før 2020-03-01`() {
        assertEquals(dato("2020-02-29"), dato("2020-03-01").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne siste dag i måneden før 2021-03-01`() {
        assertEquals(dato("2021-02-28"), dato("2021-03-01").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne siste dag i måneden forrige år`() {
        assertEquals(dato("2019-12-31"), dato("2020-01-15").sisteDagIForrigeMåned())
    }

    @Test
    fun `skal finne første dag neste år`() {
        assertEquals(dato("2020-01-01"), dato("2019-12-03").førsteDagINesteMåned())
    }

    @Test
    fun `skal finne første dag i måneden etter skuddårsdagen`() {
        assertEquals(dato("2020-03-01"), dato("2020-02-29").førsteDagINesteMåned())
    }

    @Test
    fun `skal finne siste dag i inneværende måned 2020-03-01`() {
        assertEquals(dato("2020-03-31"), dato("2020-03-01").sisteDagIMåned())
    }

    @Test
    fun `skal finne siste dag i inneværende måned 2020-02-01 skuddår`() {
        assertEquals(dato("2020-02-29"), dato("2020-02-01").sisteDagIMåned())
    }

    @Test
    fun `skal returnere seneste dato av 2020-01-01 og 2019-01-01`() {
        assertEquals(dato("2020-01-01"), senesteDatoAv(dato("2020-01-01"), dato("2019-01-01")))
    }

    @Test
    fun `skal returnere true for dato som er senere enn`() {
        assertEquals(true, dato("2020-01-01").isSameOrAfter( dato("2019-01-01")))
    }

    @Test
    fun `skal returnere false for dato som er tidligere`() {
        assertEquals(false, dato("2019-01-01").isSameOrAfter( dato("2020-01-01")))
    }

    @Test
    fun `skal returnere true for dato som er lik`() {
        assertEquals(true, dato("2020-01-01").isSameOrAfter( dato("2020-01-01")))
    }

    @Test
    fun `skal bestemme om periode er etterfølgende periode`() {
        val personIdent = randomFnr()
        val behandling = lagBehandling()
        val resultat: Resultat = mockk()
        val vilkår: Vilkår = mockk()
        val behandlingResultat = lagBehandlingResultat(personIdent, behandling, resultat)

        val personResultat = PersonResultat(
                behandlingResultat = behandlingResultat,
                personIdent = personIdent
        )

        val førsteVilkårResultat = VilkårResultat(personResultat = personResultat,
                                                  resultat = resultat,
                                                  vilkårType = vilkår,
                                                  periodeFom = LocalDate.of(2020, 1, 1),
                                                  periodeTom = LocalDate.of(2020, 3, 25),
                                                  begrunnelse = "",
                                                  behandlingId = personResultat.behandlingResultat.behandling.id,
                                                  regelInput = null,
                                                  regelOutput = null)
        val etterfølgendeVilkårResultat = VilkårResultat(personResultat = personResultat, resultat = resultat,
                                                         vilkårType = vilkår, periodeFom = LocalDate.of(2020, 3, 31),
                                                         periodeTom = LocalDate.of(2020, 6, 1), begrunnelse = "",
                                                         behandlingId = personResultat.behandlingResultat.behandling.id,
                                                         regelInput = null,
                                                         regelOutput = null)
        val ikkeEtterfølgendeVilkårResultat = VilkårResultat(personResultat = personResultat, resultat = resultat,
                                                             vilkårType = vilkår, periodeFom = LocalDate.of(2020, 5, 1),
                                                             periodeTom = LocalDate.of(2020, 6, 1), begrunnelse = "",
                                                             behandlingId = personResultat.behandlingResultat.behandling.id,
                                                             regelInput = null,
                                                             regelOutput = null)

        assertTrue(førsteVilkårResultat.erEtterfølgendePeriode(etterfølgendeVilkårResultat))
        assertFalse(førsteVilkårResultat.erEtterfølgendePeriode(ikkeEtterfølgendeVilkårResultat))
    }

    @Test
    fun `skal slå sammen overlappende perioder til en periode og bruke laveste fom og beholde tom fra periode 3`() {
        val periode1 = DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2005,9,2))
        val periode2 = DatoIntervallEntitet(LocalDate.of(2005,10,1), LocalDate.of(2015,5,20))
        val periode3 = DatoIntervallEntitet(LocalDate.of(2014,10,1), LocalDate.of(2018,5,20))
        val currentPeriode = DatoIntervallEntitet(LocalDate.of(2018,6,1), null)

        val result = slåSammenOverlappendePerioder(listOf( periode2, periode3, periode1, currentPeriode))
        Assertions.assertThat(result)
                .hasSize(3)
                .contains(periode1)
                .contains(DatoIntervallEntitet(LocalDate.of(2005, 10, 1), LocalDate.of(2018,5,20)))
                .contains(currentPeriode)
    }

    @Test
    fun `skal slå sammen overlappende perioder til en periode og bruke laveste fom og beholde tom fra periode 2`() {
        val periode1 = DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2005,9,2))
        val periode2 = DatoIntervallEntitet(LocalDate.of(2005,10,1), LocalDate.of(2018,5,20))
        val periode3 = DatoIntervallEntitet(LocalDate.of(2014,10,1), LocalDate.of(2015,5,20))
        val currentPeriode = DatoIntervallEntitet(LocalDate.of(2018,6,1), null)

        val result = slåSammenOverlappendePerioder(listOf( periode2, periode3, periode1, currentPeriode))
        Assertions.assertThat(result)
                .hasSize(3)
                .contains(periode1)
                .contains(DatoIntervallEntitet(LocalDate.of(2005, 10, 1), LocalDate.of(2018,5,20)))
                .contains(currentPeriode)
    }

    @Test
    fun `skal slå sammen overlappende perioder med samme startdato`() {
        val result = slåSammenOverlappendePerioder(listOf(
                DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2004,2,1)),
                DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2004,3,1)),
                DatoIntervallEntitet(LocalDate.of(2005,1,1), LocalDate.of(2005,3,1)),
                DatoIntervallEntitet(LocalDate.of(2005,1,1), LocalDate.of(2005,2,1))
        ))

        Assertions.assertThat(result)
                .hasSize(2)
                .contains(DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2004,3,1)))
                .contains(DatoIntervallEntitet(LocalDate.of(2005,1,1), LocalDate.of(2005,3,1)))

    }

    @Test
    fun `skal ikke slå sammen perioder som ligger inntil hverandre`() {
        val result = slåSammenOverlappendePerioder(listOf(
                DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2004,1,31)),
                DatoIntervallEntitet(LocalDate.of(2004,2,1), LocalDate.of(2004,2,28))
        ))

        Assertions.assertThat(result)
                .hasSize(2)
                .contains(DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2004,1,31)))
                .contains(DatoIntervallEntitet(LocalDate.of(2004,2,1), LocalDate.of(2004,2,28)))
    }

    @Test
    fun `skal slå sammen overlappende perioder til en periode og videreføre null i tom`() {
        val periode1 = DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2005,9,2))
        val periode2 = DatoIntervallEntitet(LocalDate.of(2005,10,1), LocalDate.of(2015,5,20))
        val periode3 = DatoIntervallEntitet(LocalDate.of(2014,10,1), LocalDate.of(2018,5,20))
        val currentPeriode = DatoIntervallEntitet(LocalDate.of(2008,6,1), null)

        val result = slåSammenOverlappendePerioder(listOf( periode2, periode3, periode1, currentPeriode))
        Assertions.assertThat(result)
                .hasSize(2)
                .contains(periode1)
                .contains(DatoIntervallEntitet(LocalDate.of(2005, 10, 1), null))
    }

    @Test
    fun `skal slå sammen perioder til én periode hvor første periode har tom som null`() {
        val periode1 = DatoIntervallEntitet(LocalDate.of(2004,1,1), null)
        val periode2 = DatoIntervallEntitet(LocalDate.of(2005,10,1), LocalDate.of(2015,5,20))

        val result = slåSammenOverlappendePerioder(listOf( periode1, periode2))
        Assertions.assertThat(result)
                .hasSize(1)
                .contains(periode1)
                .contains(DatoIntervallEntitet(LocalDate.of(2004, 1, 1), null))
    }

    @Test
    fun `hopp over perioder som ikke har fra-dato`() {
        val periode1 = DatoIntervallEntitet(null, null)
        val periode2 = DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2004,1,5))

        val result = slåSammenOverlappendePerioder(listOf(periode1, periode2))
        Assertions.assertThat(result)
                .hasSize(1)
    }

    @Test
    fun `det skal kun finnes en periode med tom = null  etter sammenslåing`() {
        val result = slåSammenOverlappendePerioder(listOf(
                DatoIntervallEntitet(LocalDate.of(2004,1,1), LocalDate.of(2004,1,1)),
                DatoIntervallEntitet(LocalDate.of(2005,1,1), null),
                DatoIntervallEntitet(LocalDate.of(2005,5,1), LocalDate.of(2005,6,1)),
                DatoIntervallEntitet(LocalDate.of(2006,1,1), null),
                DatoIntervallEntitet(LocalDate.of(2006,5,1), LocalDate.of(2006,6,1))
                ))
        Assertions.assertThat(result).hasSize(2)
        Assertions.assertThat(result.filter { it.tom != null}).hasSize(1)
    }

    private fun dato(s: String): LocalDate {
        return LocalDate.parse(s)
    }
}

