package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.*
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VilkårsvurderingUtilsTest {

    lateinit var vilkårResultat1: VilkårResultat
    lateinit var vilkårResultat2: VilkårResultat
    lateinit var vilkårResultat3: VilkårResultat
    lateinit var behandlingResultat: BehandlingResultat
    lateinit var personResultat: PersonResultat
    lateinit var vilkår: Vilkår
    lateinit var resultat: Resultat
    lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        val personIdent = randomFnr()

        behandling = lagBehandling()

        vilkår = Vilkår.BOR_MED_SØKER
        resultat = Resultat.JA

        behandlingResultat = lagBehandlingResultat(personIdent, behandling, resultat)

        personResultat = PersonResultat(
                behandlingResultat = behandlingResultat,
                personIdent = personIdent
        )

        vilkårResultat1 = VilkårResultat(1, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 1, 1), LocalDate.of(2010, 6, 1),
                                         "", behandlingResultat.behandling.id, regelInput = null, regelOutput = null)
        vilkårResultat2 = VilkårResultat(2, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 6, 2), LocalDate.of(2010, 8, 1),
                                         "", behandlingResultat.behandling.id, regelInput = null, regelOutput = null)
        vilkårResultat3 = VilkårResultat(3, personResultat, vilkår, resultat,
                                         LocalDate.of(2010, 8, 2), LocalDate.of(2010, 12, 1),
                                         "", behandlingResultat.behandling.id, regelInput = null, regelOutput = null)
        personResultat.setVilkårResultater(setOf(vilkårResultat1,
                                                 vilkårResultat2,
                                                 vilkårResultat3).toSortedSet(PersonResultat.comparator))
    }

    private fun assertPeriode(expected: Periode, actual: Periode) {
        assertEquals(expected.fom, actual.fom)
        assertEquals(expected.tom, actual.tom)
    }

    @Test
    fun `periode erstattes dersom en periode med overlappende tidsintervall legges til`() {
        val restVilkårResultat = RestVilkårResultat(2, vilkår, resultat,
                                                    LocalDate.of(2010, 6, 2), LocalDate.of(2011, 9, 1),
                                                    "",
                                                    "",
                                                    LocalDateTime.now(),
                                                    behandling.id)
        VilkårsvurderingUtils.muterPersonResultatPut(personResultat,
                                                     restVilkårResultat)

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
                                                    LocalDate.of(2010, 3, 5), LocalDate.of(2010, 5, 20),
                                                    "",
                                                    "",
                                                    LocalDateTime.now(),
                                                    behandling.id)

        VilkårsvurderingUtils.muterPersonResultatPut(personResultat,
                                                     restVilkårResultat)

        assertEquals(5, personResultat.vilkårResultater.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 3, 4)), personResultat.getVilkårResultat(0)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 3, 5),
                              LocalDate.of(2010, 5, 20)), personResultat.getVilkårResultat(1)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 5, 21),
                              LocalDate.of(2010, 6, 1)), personResultat.getVilkårResultat(2)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
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
                                                    "",
                                                    "",
                                                    LocalDateTime.now(),
                                                    behandling.id)

        VilkårsvurderingUtils.muterPersonResultatPut(personResultat,
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
                                                    "",
                                                    "",
                                                    LocalDateTime.now(),
                                                    behandling.id)

        VilkårsvurderingUtils.muterPersonResultatPut(personResultat,
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

    @Test
    fun `Skal fjerne og fylle inn tom periode i midten`() {
        VilkårsvurderingUtils.muterPersonResultatDelete(personResultat, 2)

        assertEquals(3, personResultat.vilkårResultater.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 6, 1)), personResultat.getVilkårResultat(0)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
                              LocalDate.of(2010, 8, 1)), personResultat.getVilkårResultat(1)!!.toPeriode()
        )
        assertEquals(Resultat.KANSKJE, personResultat.getVilkårResultat(1)!!.resultat)
        assertPeriode(Periode(LocalDate.of(2010, 8, 2),
                              LocalDate.of(2010, 12, 1)), personResultat.getVilkårResultat(2)!!.toPeriode()
        )
    }

    @Test
    fun `Skal fjerne første periode`() {
        VilkårsvurderingUtils.muterPersonResultatDelete(personResultat,
                                                        1)

        assertEquals(2, personResultat.vilkårResultater.size)
        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
                              LocalDate.of(2010, 8, 1)), personResultat.getVilkårResultat(0)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 8, 2),
                              LocalDate.of(2010, 12, 1)), personResultat.getVilkårResultat(1)!!.toPeriode()
        )
    }

    @Test
    fun `Skal fjerne siste periode`() {
        VilkårsvurderingUtils.muterPersonResultatDelete(personResultat,
                                                        3)

        assertEquals(2, personResultat.vilkårResultater.size)
        assertPeriode(Periode(LocalDate.of(2010, 1, 1),
                              LocalDate.of(2010, 6, 1)), personResultat.getVilkårResultat(0)!!.toPeriode()
        )
        assertPeriode(Periode(LocalDate.of(2010, 6, 2),
                              LocalDate.of(2010, 8, 1)), personResultat.getVilkårResultat(1)!!.toPeriode()
        )
    }

    @Test
    fun `Skal nullstille periode hvis det kun finnes en periode`() {
        val mockPersonResultat = PersonResultat(
                behandlingResultat = behandlingResultat,
                personIdent = randomFnr()
        )

        val mockVilkårResultat = VilkårResultat(1, mockPersonResultat, vilkår, resultat,
                                                LocalDate.of(2010, 1, 1), LocalDate.of(2010, 6, 1),
                                                "", behandlingResultat.behandling.id, regelInput = null, regelOutput = null)
        mockPersonResultat.setVilkårResultater(setOf(mockVilkårResultat))

        VilkårsvurderingUtils.muterPersonResultatDelete(mockPersonResultat,
                                                        1)

        assertEquals(1, mockPersonResultat.vilkårResultater.size)
        assertEquals(Resultat.KANSKJE, mockPersonResultat.getVilkårResultat(0)!!.resultat)
    }

    @Test
    fun `Skal legge til periode`() {
        assertEquals(3, personResultat.vilkårResultater.size)
        VilkårsvurderingUtils.muterPersonResultatPost(personResultat, Vilkår.BOR_MED_SØKER)
        assertEquals(4, personResultat.vilkårResultater.size)

        assertThrows(Feil::class.java) {
            VilkårsvurderingUtils.muterPersonResultatPost(personResultat, Vilkår.BOR_MED_SØKER)
        }
    }

    @Test
    fun `Skal kaste feil når det legges til periode i en vilkårtype der det allerede finnes en uvurdert periode`() {
        VilkårsvurderingUtils.muterPersonResultatPost(personResultat, Vilkår.BOR_MED_SØKER)
        assertThrows(Feil::class.java) {
            VilkårsvurderingUtils.muterPersonResultatPost(personResultat, Vilkår.BOR_MED_SØKER)
        }
    }

    @Test
    fun `Skal tilpasse vilkår for endret vilkår når begge mangler tom-dato`() {
        val vilkårResultat = VilkårResultat(personResultat = personResultat,
                                            vilkårType = vilkår,
                                            resultat = resultat,
                                            periodeFom = LocalDate.of(2020, 1, 1),
                                            begrunnelse = "",
                                            regelInput = null,
                                            behandlingId = behandlingResultat.behandling.id,
                                            regelOutput = null)
        val restVilkårResultat = RestVilkårResultat(id = 1,
                                                    vilkårType = vilkår,
                                                    resultat = resultat,
                                                    periodeFom = LocalDate.of(2020, 6, 1),
                                                    periodeTom = null,
                                                    begrunnelse = "",
                                                    endretAv = "",
                                                    endretTidspunkt = LocalDateTime.now(),
                                                    behandlingId = behandling.id)

        VilkårsvurderingUtils.tilpassVilkårForEndretVilkår(personResultat, vilkårResultat, restVilkårResultat)

        assertEquals(LocalDate.of(2020, 1, 1), vilkårResultat.periodeFom)
        assertEquals(LocalDate.of(2020, 5, 31), vilkårResultat.periodeTom)
    }
}