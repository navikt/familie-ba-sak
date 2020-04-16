package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.beregning.domene.lagTidslinjeMedOverlappendePerioder
import no.nav.familie.ba.sak.common.lagBehandlingResultat
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate


class PeriodeMapperTest {

    private val datoer = listOf(
            LocalDate.now().minusMonths(2),
            LocalDate.now().minusMonths(1),
            LocalDate.now(),
            LocalDate.now().plusMonths(1),
            LocalDate.now().plusMonths(2))

    private lateinit var behandlingResultat: BehandlingResultat

    @BeforeEach fun initEach() {
        val fagsak = Fagsak(aktørId = AktørId("123"),
                            personIdent = PersonIdent("123"),
                            status = FagsakStatus.OPPRETTET)
        val behandling = Behandling(fagsak = fagsak,
                                    kategori = BehandlingKategori.NASJONAL,
                                    underkategori = BehandlingUnderkategori.ORDINÆR,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING)
        behandlingResultat = lagBehandlingResultat("", behandling, Resultat.KANSKJE)
    }

    @Test
    fun `Kombinert tidslinje returnerer rette rette vilkårsresultater for tidsintervaller`() {
        val personResultat = PersonResultat(behandlingResultat = behandlingResultat, personIdent = "")

        val tidslinje1 = LocalDateTimeline(listOf(LocalDateSegment(datoer[0],
                                                                   datoer[2],
                                                                   VilkårResultat(personResultat = personResultat,
                                                                                  vilkårType = Vilkår.UNDER_18_ÅR,
                                                                                  resultat = Resultat.JA,
                                                                                  begrunnelse = ""))))
        val tidslinje2 = LocalDateTimeline(listOf(LocalDateSegment(datoer[1],
                                                                   datoer[3],
                                                                   VilkårResultat(personResultat = personResultat,
                                                                                  vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                                  resultat = Resultat.JA,
                                                                                  begrunnelse = ""))))

        val kombinertTidslinje = lagTidslinjeMedOverlappendePerioder(listOf(tidslinje1, tidslinje2))

        assert(kombinertTidslinje.toSegments().size == 3)
        val segment1 = kombinertTidslinje.getSegment(LocalDateInterval(datoer[0], datoer[1].minusDays(1)))
        val segment2 = kombinertTidslinje.getSegment(LocalDateInterval(datoer[1], datoer[2]))
        val segment3 = kombinertTidslinje.getSegment(LocalDateInterval(datoer[2].plusDays(1), datoer[3]))

        assert(segment1.value.size == 1)
        assert(segment2.value.size == 2)
        assert(segment3.value.size == 1)

        assert(segment1.value[0].vilkårType == Vilkår.UNDER_18_ÅR)
        assert(segment2.value[0].vilkårType == Vilkår.UNDER_18_ÅR)
        assert(segment2.value[1].vilkårType == Vilkår.BOSATT_I_RIKET)
        assert(segment3.value[0].vilkårType == Vilkår.BOSATT_I_RIKET)
    }

    @Test
    fun `Mapper tre PersonResultater til fire PeriodeResultater med rette vilkår og datoer for to personer`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val personResultat1 = PersonResultat(behandlingResultat = behandlingResultat, personIdent = fnr1)
        val personResultat2 = PersonResultat(behandlingResultat = behandlingResultat, personIdent = fnr2)
        personResultat1.vilkårResultater = setOf(VilkårResultat(personResultat = personResultat1,
                                                                vilkårType = Vilkår.UNDER_18_ÅR,
                                                                resultat = Resultat.JA,
                                                                periodeFom = datoer[0],
                                                                periodeTom = datoer[2],
                                                                begrunnelse = ""),
                                                 VilkårResultat(personResultat = personResultat1,
                                                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                resultat = Resultat.JA,
                                                                periodeFom = datoer[1],
                                                                periodeTom = datoer[3],
                                                                begrunnelse = ""),
                                                 VilkårResultat(personResultat = personResultat1,
                                                                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                                                resultat = Resultat.JA,
                                                                periodeFom = datoer[1],
                                                                periodeTom = datoer[4],
                                                                begrunnelse = ""))
        personResultat2.vilkårResultater = setOf(VilkårResultat(personResultat = personResultat2,
                                                                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                                                resultat = Resultat.JA,
                                                                periodeFom = datoer[1],
                                                                periodeTom = datoer[4],
                                                                begrunnelse = ""))
        behandlingResultat.personResultater = setOf(personResultat1, personResultat2)
        val periodeResultater = behandlingResultat.periodeResultater.toList()

        /*
        Person 1 med tre overlappende perioder som skal splittes til fire
        Person 2 med én periode som skal bevares som én

        Siste periode vil alltid få
         */
        assert(periodeResultater.size == 5)

        println(periodeResultater[0].toString())
        println(periodeResultater[1].toString())
        println(periodeResultater[2].toString())
        println(periodeResultater[3].toString())
        println(periodeResultater[4].toString())

        // Person 1 første periode
        assert(periodeResultater[0].vilkårResultater.size == 1)
        assert(periodeResultater[0].personIdent == fnr1)
        assert(periodeResultater[0].vilkårResultater.any { it.vilkårType == Vilkår.UNDER_18_ÅR })
        assert(periodeResultater[0].periodeFom!! == datoer[0])
        assert(periodeResultater[0].periodeTom!! == datoer[1].minusDays(1))

        // Person 1 andre periode (overlappende)
        assert(periodeResultater[1].vilkårResultater.size == 3)
        assert(periodeResultater[1].personIdent == fnr1)
        assert(periodeResultater[1].vilkårResultater.any { it.vilkårType == Vilkår.UNDER_18_ÅR })
        assert(periodeResultater[1].vilkårResultater.any { it.vilkårType == Vilkår.BOSATT_I_RIKET })
        assert(periodeResultater[1].vilkårResultater.any { it.vilkårType == Vilkår.LOVLIG_OPPHOLD })
        assert(periodeResultater[1].periodeFom!! == datoer[1])
        assert(periodeResultater[1].periodeTom!! == datoer[2])

        // Person 1 tredje periode
        assert(periodeResultater[2].vilkårResultater.size == 2)
        assert(periodeResultater[2].personIdent == fnr1)
        assert(periodeResultater[2].vilkårResultater.any { it.vilkårType == Vilkår.BOSATT_I_RIKET })
        assert(periodeResultater[2].vilkårResultater.any { it.vilkårType == Vilkår.LOVLIG_OPPHOLD })
        assert(periodeResultater[2].periodeFom!! == datoer[2].plusDays(1))
        assert(periodeResultater[2].periodeTom!! == datoer[3])

        // Person 1 fjerde periode
        assert(periodeResultater[3].vilkårResultater.size == 1)
        assert(periodeResultater[3].personIdent == fnr1)
        assert(periodeResultater[3].vilkårResultater.any { it.vilkårType == Vilkår.LOVLIG_OPPHOLD })
        assert(periodeResultater[3].periodeFom!! == datoer[3].plusDays(1))
        assert(periodeResultater[3].periodeTom!! == datoer[4])

        // Person 2
        assert(periodeResultater[4].vilkårResultater.size == 1)
        assert(periodeResultater[4].personIdent == fnr2)
        assert(periodeResultater[4].vilkårResultater.any { it.vilkårType == Vilkår.LOVLIG_OPPHOLD })
        assert(periodeResultater[4].periodeFom!! == datoer[1])
        assert(periodeResultater[4].periodeTom!! == datoer[4])
    }
}