package no.nav.familie.ba.sak.behandling.vilkår

import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingUtils.sorterListe
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsvurderingUtilsTest() {

    lateinit var vilkårResultat1: VilkårResultat
    lateinit var vilkårResultat2: VilkårResultat
    lateinit var vilkårResultat3: VilkårResultat
    lateinit var vilkårResultater: List<VilkårResultat>
    lateinit var personResultat: PersonResultat
    lateinit var vilkår: Vilkår
    lateinit var resultat: Resultat

    @BeforeEach
    fun init() {
        personResultat = mockk()
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
        vilkårResultater = listOf(vilkårResultat1, vilkårResultat2, vilkårResultat3)
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
        val gyldigVilkårListe = VilkårsvurderingUtils.endreVurderingForPeriodePåVilkår(vilkårResultater,
                                                                                       restVilkårResultat)

        assertEquals(2, gyldigVilkårListe.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 6, 1)), sorterListe(gyldigVilkårListe)[0].toPeriode()
        )

        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
                              LocalDate.of(2011, 9, 1)), sorterListe(gyldigVilkårListe)[1].toPeriode()
        )
    }

    @Test
    fun `periode splittes dersom en periode med inneklemt tidsintervall legges til`() {
        val restVilkårResultat = RestVilkårResultat(2, vilkår, resultat,
                                                    LocalDate.of(2010, 6, 5), LocalDate.of(2010, 7, 1),
                                                    "")

        val gyldigVilkårListe = VilkårsvurderingUtils.endreVurderingForPeriodePåVilkår(vilkårResultater,
                                                                                       restVilkårResultat)

        assertEquals(5, gyldigVilkårListe.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 6, 1)), sorterListe(gyldigVilkårListe)[0].toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
                              LocalDate.of(2010, 6, 4)), sorterListe(gyldigVilkårListe)[1].toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 6, 5),
                              LocalDate.of(2010, 7, 1)), sorterListe(gyldigVilkårListe)[2].toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 7, 2),
                              LocalDate.of(2010, 8, 1)), sorterListe(gyldigVilkårListe)[3].toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 8, 2),
                              LocalDate.of(2010, 12, 1)), sorterListe(gyldigVilkårListe)[4].toPeriode()
        )
    }

    @Test
    fun `fom-dato flyttes korrekt`() {
        val nyttResultat = VilkårResultat(1, personResultat, vilkår, resultat,
                                          LocalDate.of(2010, 5, 17), LocalDate.of(2010, 7, 1),
                                          "")
        val restVilkårResultat = RestVilkårResultat(1, vilkår, resultat,
                                                    LocalDate.of(2010, 5, 17), LocalDate.of(2010, 7, 1),
                                                    "")
        val vilkårResultater: List<VilkårResultat> = listOf(vilkårResultat1, vilkårResultat2, vilkårResultat3,
                                                            nyttResultat)

        val gyldigVilkårListe = VilkårsvurderingUtils.endreVurderingForPeriodePåVilkår(vilkårResultater,
                                                                                       restVilkårResultat)
        val vilkårMedEndretFomDato = gyldigVilkårListe.find {
            it.periodeFom ==
            LocalDate.of(2010, 7, 2) &&
            it.periodeTom == LocalDate.of(2010, 8, 1)
        }

        assertNotNull(vilkårMedEndretFomDato)
        assertEquals(4, gyldigVilkårListe.size)

    }
}