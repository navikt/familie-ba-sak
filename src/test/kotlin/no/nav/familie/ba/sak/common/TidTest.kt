package no.nav.familie.ba.sak.common

import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.nare.core.evaluations.Resultat
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

    private fun dato(s: String): LocalDate {
        return LocalDate.parse(s)
    }
}

