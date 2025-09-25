package no.nav.familie.ba.sak.common

import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

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
        assertEquals(true, dato("2020-01-01").isSameOrAfter(dato("2019-01-01")))
    }

    @Test
    fun `skal returnere false for dato som er tidligere`() {
        assertEquals(false, dato("2019-01-01").isSameOrAfter(dato("2020-01-01")))
    }

    @Test
    fun `skal returnere true for dato som er lik`() {
        assertEquals(true, dato("2020-01-01").isSameOrAfter(dato("2020-01-01")))
    }

    @Test
    fun `skal returnere true dersom dato er dagen før en annen dato`() {
        assertTrue(dato("2020-04-30").erDagenFør(dato("2020-05-01")))
        assertFalse(dato("2020-04-30").erDagenFør(dato("2020-05-02")))
        assertFalse(dato("2020-05-01").erDagenFør(dato("2020-04-30")))
        assertFalse(dato("2020-04-30").erDagenFør(dato("2020-04-30")))
        assertFalse(dato("2020-04-30").erDagenFør(null))
    }

    @Test
    fun `dato i inneværende eller forrige måned`() {
        assertTrue(LocalDate.now().erFraInneværendeMåned())
        assertTrue(LocalDate.now().erFraInneværendeEllerForrigeMåned())
        assertFalse(LocalDate.now().minusMonths(1).erFraInneværendeMåned())
        assertTrue(LocalDate.now().minusMonths(1).erFraInneværendeEllerForrigeMåned())
        assertFalse(LocalDate.now().minusYears(1).erFraInneværendeMåned())
        assertFalse(LocalDate.now().minusYears(1).erFraInneværendeEllerForrigeMåned())
    }

    @Test
    fun `skal bestemme om periode er etterfølgende periode`() {
        val personAktørId = randomAktør()
        val behandling = lagBehandling()
        val resultat: Resultat = mockk()
        val vilkår: Vilkår = mockk(relaxed = true)
        val vilkårsvurdering = lagVilkårsvurdering(personAktørId, behandling, resultat)

        val personResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = personAktørId,
            )

        val førsteVilkårResultat =
            VilkårResultat(
                personResultat = personResultat,
                resultat = resultat,
                vilkårType = vilkår,
                periodeFom = LocalDate.of(2020, 1, 1),
                periodeTom = LocalDate.of(2020, 3, 25),
                begrunnelse = "",
                sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
            )
        val etterfølgendeVilkårResultat =
            VilkårResultat(
                personResultat = personResultat,
                resultat = resultat,
                vilkårType = vilkår,
                periodeFom = LocalDate.of(2020, 3, 31),
                periodeTom = LocalDate.of(2020, 6, 1),
                begrunnelse = "",
                sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
            )
        val ikkeEtterfølgendeVilkårResultat =
            VilkårResultat(
                personResultat = personResultat,
                resultat = resultat,
                vilkårType = vilkår,
                periodeFom = LocalDate.of(2020, 5, 1),
                periodeTom = LocalDate.of(2020, 6, 1),
                begrunnelse = "",
                sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
            )

        assertTrue(førsteVilkårResultat.erEtterfølgendePeriode(etterfølgendeVilkårResultat))
        assertFalse(førsteVilkårResultat.erEtterfølgendePeriode(ikkeEtterfølgendeVilkårResultat))
    }

    @Test
    fun `formatering gir forventet resultat`() {
        assertEquals("31. desember 2020", dato("2020-12-31").tilDagMånedÅr())
        assertEquals("311220", dato("2020-12-31").tilddMMyy())
        assertEquals("31.12.20", dato("2020-12-31").tilKortString())
        assertEquals("desember 2020", dato("2020-12-31").tilMånedÅr())
    }

    @Test
    fun `sjekk for om to måned perioder helt eller delvis er overlappende`() {
        val jan2020Aug2020 = MånedPeriode(YearMonth.of(2020, 1), YearMonth.of(2020, 8))
        val jul2020Des2020 = MånedPeriode(YearMonth.of(2020, 7), YearMonth.of(2020, 12))
        val des2019Sep2021 = MånedPeriode(YearMonth.of(2019, 12), YearMonth.of(2020, 9))
        val jan2020 = MånedPeriode(YearMonth.of(2020, 1), YearMonth.of(2020, 1))
        val aug2020 = MånedPeriode(YearMonth.of(2020, 8), YearMonth.of(2020, 8))
        val des2019 = MånedPeriode(YearMonth.of(2019, 12), YearMonth.of(2019, 12))
        val sep2021 = MånedPeriode(YearMonth.of(2021, 9), YearMonth.of(2021, 9))

        assertTrue(jan2020Aug2020.overlapperHeltEllerDelvisMed(jul2020Des2020))
        assertTrue(jul2020Des2020.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertTrue(jan2020Aug2020.overlapperHeltEllerDelvisMed(des2019Sep2021))
        assertTrue(des2019Sep2021.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertTrue(jan2020Aug2020.overlapperHeltEllerDelvisMed(jan2020))
        assertTrue(jan2020.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertTrue(jan2020Aug2020.overlapperHeltEllerDelvisMed(aug2020))
        assertTrue(aug2020.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertFalse(jan2020Aug2020.overlapperHeltEllerDelvisMed(des2019))
        assertFalse(des2019.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertFalse(jan2020Aug2020.overlapperHeltEllerDelvisMed(sep2021))
        assertFalse(sep2021.overlapperHeltEllerDelvisMed(jan2020Aug2020))
    }

    @Test
    fun `sjekk for om to perioder helt eller delvis er overlappende`() {
        val jan2020Aug2020 = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 8, 1))
        val jul2020Des2020 = Periode(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 12, 1))
        val des2019Sep2021 = Periode(LocalDate.of(2019, 12, 1), LocalDate.of(2020, 9, 1))
        val jan2020 = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1))
        val aug2020 = Periode(LocalDate.of(2020, 8, 1), LocalDate.of(2020, 8, 1))
        val des2019 = Periode(LocalDate.of(2019, 12, 1), LocalDate.of(2019, 12, 1))
        val sep2021 = Periode(LocalDate.of(2021, 9, 1), LocalDate.of(2021, 9, 1))

        assertTrue(jan2020Aug2020.overlapperHeltEllerDelvisMed(jul2020Des2020))
        assertTrue(jul2020Des2020.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertTrue(jan2020Aug2020.overlapperHeltEllerDelvisMed(des2019Sep2021))
        assertTrue(des2019Sep2021.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertTrue(jan2020Aug2020.overlapperHeltEllerDelvisMed(jan2020))
        assertTrue(jan2020.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertTrue(jan2020Aug2020.overlapperHeltEllerDelvisMed(aug2020))
        assertTrue(aug2020.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertFalse(jan2020Aug2020.overlapperHeltEllerDelvisMed(des2019))
        assertFalse(des2019.overlapperHeltEllerDelvisMed(jan2020Aug2020))
        assertFalse(jan2020Aug2020.overlapperHeltEllerDelvisMed(sep2021))
        assertFalse(sep2021.overlapperHeltEllerDelvisMed(jan2020Aug2020))
    }

    private fun dato(s: String): LocalDate = LocalDate.parse(s)
}
