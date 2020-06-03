package no.nav.familie.ba.sak.behandling.vilkår

import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsvurderingUtilsTest {

    lateinit var vilkårResultat1: VilkårResultat
    lateinit var vilkårResultat2: VilkårResultat
    lateinit var vilkårResultat3: VilkårResultat
    lateinit var behandlingResultat: BehandlingResultat
    lateinit var personResultat: PersonResultat
    lateinit var vilkår: Vilkår
    lateinit var resultat: Resultat

    @BeforeEach
    fun init() {
        behandlingResultat = mockk()
        personResultat = PersonResultat(
                behandlingResultat = behandlingResultat,
                personIdent = randomFnr()
        )
        vilkår = mockk()
        resultat = mockk()

        vilkårResultat1 = VilkårResultat(1, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 1, 1), LocalDate.of(2010, 6, 1),
                                         "")
        vilkårResultat2 = VilkårResultat(2, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 6, 2), LocalDate.of(2010, 8, 1),
                                         "")
        vilkårResultat3 = VilkårResultat(3, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 8, 2), LocalDate.of(2010, 12, 1),
                                         "")
        personResultat.vilkårResultater = setOf(vilkårResultat1, vilkårResultat2, vilkårResultat3)

    }

    private fun assertPeriode(expected: Periode, actual: Periode) {
        assertEquals(expected.fom, actual.fom)
        assertEquals(expected.tom, actual.tom)
    }

    @Test
    fun `periode erstattes dersom en periode med overlappende tidsintervall legges til`() {
        val restVilkårResultat = RestVilkårResultat(2, vilkår, resultat,
                                                    LocalDate.of(2010, 6, 2), LocalDate.of(2011, 9, 1),
                                                    "")
        VilkårsvurderingUtils.muterPersonResultat(personResultat,
                                                  restVilkårResultat)

        personResultat.sorterVilkårResultater()
        assertEquals(2, personResultat.vilkårResultater.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 6, 1)), personResultat.getVilkårResultat(0)!!.toPeriode()
        )

        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
                              LocalDate.of(2011, 9, 1)), personResultat.getVilkårResultat(1)!!.toPeriode()
        )
    }

    @Test
    fun `periode splittes dersom en periode med inneklemt tidsintervall legges til`() {
        val restVilkårResultat = RestVilkårResultat(2, vilkår, resultat,
                                                    LocalDate.of(2010, 6, 5), LocalDate.of(2010, 7, 1),
                                                    "")

        VilkårsvurderingUtils.muterPersonResultat(personResultat,
                                                  restVilkårResultat)

        assertEquals(5, personResultat.vilkårResultater.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 6, 1)), personResultat.getVilkårResultat(0)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
                              LocalDate.of(2010, 6, 4)), personResultat.getVilkårResultat(1)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 6, 5),
                              LocalDate.of(2010, 7, 1)), personResultat.getVilkårResultat(2)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 7, 2),
                              LocalDate.of(2010, 8, 1)), personResultat.getVilkårResultat(3)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 8, 2),
                              LocalDate.of(2010, 12, 1)), personResultat.getVilkårResultat(4)!!.toPeriode()
        )
    }

    @Test
    fun `fom-dato flyttes korrekt`() {
        val restVilkårResultat = RestVilkårResultat(2, vilkår, resultat,
                                                    LocalDate.of(2010, 4, 2), LocalDate.of(2010, 8, 1),
                                                    "")

        VilkårsvurderingUtils.muterPersonResultat(personResultat,
                                                  restVilkårResultat)

        assertEquals(3, personResultat.vilkårResultater.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 4, 1)), personResultat.getVilkårResultat(0)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 4, 2),
                              LocalDate.of(2010, 8, 1)), personResultat.getVilkårResultat(1)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 8, 2),
                              LocalDate.of(2010, 12, 1)), personResultat.getVilkårResultat(2)!!.toPeriode()
        )
    }

    @Test
    fun `tom-dato flyttes korrekt`() {
        val restVilkårResultat = RestVilkårResultat(2, vilkår, resultat,
                                                    LocalDate.of(2010, 6, 2), LocalDate.of(2010, 9, 1),
                                                    "")

        VilkårsvurderingUtils.muterPersonResultat(personResultat,
                                                  restVilkårResultat)

        assertEquals(3, personResultat.vilkårResultater.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 6, 1)), personResultat.getVilkårResultat(0)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
                              LocalDate.of(2010, 9, 1)), personResultat.getVilkårResultat(1)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 9, 2),
                              LocalDate.of(2010, 12, 1)), personResultat.getVilkårResultat(2)!!.toPeriode()
        )
    }
}