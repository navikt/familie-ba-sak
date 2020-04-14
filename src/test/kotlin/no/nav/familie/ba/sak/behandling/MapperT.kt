package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.beregning.domene.PeriodeVilkår
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagBehandlingResultat
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(classes = [ApplicationConfig::class],
                properties = ["FAMILIE_OPPDRAG_API_URL=http://localhost:28085/api",
                    "FAMILIE_BA_DOKGEN_API_URL=http://localhost:28085/api",
                    "FAMILIE_INTEGRASJONER_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen", "mock-oauth")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWireMock(port = 28085)
class MapperT {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    private fun kombinerVerdier(lhs: LocalDateTimeline<List<VilkårResultat>>,
                                rhs: LocalDateTimeline<VilkårResultat>): LocalDateTimeline<List<VilkårResultat>> {
        return lhs.combine(rhs,
                           { dateInterval, left, right ->
                               StandardCombinators.allValues<VilkårResultat>(dateInterval,
                                                                             left,
                                                                             right)
                           },
                           LocalDateTimeline.JoinStyle.CROSS_JOIN)
    }

    private fun foldTidslinjer(tidslinjer: List<LocalDateTimeline<VilkårResultat>>): LocalDateTimeline<List<VilkårResultat>> {
        val førsteSegment = tidslinjer.first().toSegments().first()
        val initiellSammenlagt =
                LocalDateTimeline(listOf(LocalDateSegment(førsteSegment.fom, førsteSegment.tom, listOf(førsteSegment.value))))
        val resterende = tidslinjer.drop(1)
        return resterende.fold(initiellSammenlagt) { sammenlagt, neste -> (kombinerVerdier(sammenlagt, neste)) }
    }

    fun personResultatTilPeriodeResultater(personResultat: PersonResultat): List<PeriodeResultat> {
        val tidslinjer = personResultat.vilkårResultater.map { vilkårResultat ->
            LocalDateTimeline(listOf(LocalDateSegment(vilkårResultat.periodeFom,
                                                      vilkårResultat.periodeTom,
                                                      vilkårResultat)))
        }
        val kombinertTidslinje = foldTidslinjer(tidslinjer)
        val periodeResultater = kombinertTidslinje.toSegments().map { segment ->
            PeriodeResultat(
                    personIdent = personResultat.personIdent,
                    periodeFom = segment.fom,
                    periodeTom = segment.tom,
                    vilkårResultater = segment.value.map { PeriodeVilkår(it.vilkårType, it.resultat, it.begrunnelse) }.toSet()
            )
        }
        return periodeResultater
    }

    @Test
    fun `Kombinert tidslinje returnerer rette rette vilkårsresultater for tidsintervaller`() {
        val d1 = LocalDate.now().minusMonths(2);
        val d2 = LocalDate.now().minusMonths(1)
        val d3 = LocalDate.now()
        val d4 = LocalDate.now().plusMonths(1)

        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.JA)
        val personResultat = PersonResultat(behandlingResultat = behandlingResultat, personIdent = "")

        val tidslinje1 = LocalDateTimeline(listOf(LocalDateSegment(d1,
                                                                   d3,
                                                                   VilkårResultat(personResultat = personResultat,
                                                                                  vilkårType = Vilkår.UNDER_18_ÅR,
                                                                                  resultat = Resultat.JA,
                                                                                  begrunnelse = ""))))
        val tidslinje2 = LocalDateTimeline(listOf(LocalDateSegment(d2,
                                                                   d4,
                                                                   VilkårResultat(personResultat = personResultat,
                                                                                  vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                                  resultat = Resultat.JA,
                                                                                  begrunnelse = ""))))
        val kombinertTidslinje = foldTidslinjer(listOf(tidslinje1, tidslinje2))

        assert(kombinertTidslinje.toSegments().size == 3)
        val segment1 = kombinertTidslinje.getSegment(LocalDateInterval(d1, d2))
        val segment2 = kombinertTidslinje.getSegment(LocalDateInterval(d2.plusDays(1), d3))
        val segment3 = kombinertTidslinje.getSegment(LocalDateInterval(d3.plusDays(1), d4))

        assert(segment1.value.size == 1)
        assert(segment2.value.size == 2)
        assert(segment3.value.size == 1)

        assert(segment1.value[0].vilkårType == Vilkår.UNDER_18_ÅR)
        assert(segment2.value[0].vilkårType == Vilkår.UNDER_18_ÅR)
        assert(segment2.value[1].vilkårType == Vilkår.BOSATT_I_RIKET)
        assert(segment3.value[0].vilkårType == Vilkår.BOSATT_I_RIKET)
    }

    //TODO: Håndtering av motstridende samme vilkår

    @Test
    fun `Mapper PersonResultater til PeriodeResultater korrekt`() {
        val d1 = LocalDate.now().minusMonths(2);
        val d2 = LocalDate.now().minusMonths(1)
        val d3 = LocalDate.now()
        val d4 = LocalDate.now().plusMonths(1)
        val d5 = LocalDate.now().plusMonths(2)


        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr1)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val behandlingResultat = lagBehandlingResultat(fnr1, behandling, Resultat.JA)
        val personResultat1 = PersonResultat(behandlingResultat = behandlingResultat, personIdent = fnr1)
        val personResultat2 = PersonResultat(behandlingResultat = behandlingResultat, personIdent = fnr2)
        personResultat1.vilkårResultater = setOf(VilkårResultat(personResultat = personResultat1,
                                                                vilkårType = Vilkår.UNDER_18_ÅR,
                                                                resultat = Resultat.JA,
                                                                periodeFom = d1,
                                                                periodeTom = d3,
                                                                begrunnelse = ""),
                                                 VilkårResultat(personResultat = personResultat1,
                                                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                                                resultat = Resultat.JA,
                                                                periodeFom = d2,
                                                                periodeTom = d4,
                                                                begrunnelse = ""))
        personResultat2.vilkårResultater = setOf(VilkårResultat(personResultat = personResultat1,
                                                                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                                                resultat = Resultat.JA,
                                                                periodeFom = d2,
                                                                periodeTom = d5,
                                                                begrunnelse = ""))
        val personResultater = listOf(personResultat1, personResultat2)
        val periodeResultater = personResultater.map { personResultatTilPeriodeResultater(it) }.flatten()

        assert(periodeResultater.size == 4)

        assert(periodeResultater[0].vilkårResultater.size == 1)
        assert(periodeResultater[0].personIdent == fnr1)
        assert(periodeResultater[0].vilkårResultater.any { it.vilkårType == Vilkår.UNDER_18_ÅR })

        assert(periodeResultater[1].vilkårResultater.size == 2)
        assert(periodeResultater[1].personIdent == fnr1)
        assert(periodeResultater[1].vilkårResultater.any { it.vilkårType == Vilkår.UNDER_18_ÅR })
        assert(periodeResultater[1].vilkårResultater.any { it.vilkårType == Vilkår.BOSATT_I_RIKET })

        assert(periodeResultater[2].vilkårResultater.size == 1)
        assert(periodeResultater[2].personIdent == fnr1)
        assert(periodeResultater[2].vilkårResultater.any { it.vilkårType == Vilkår.BOSATT_I_RIKET })

        assert(periodeResultater[3].vilkårResultater.size == 1)
        assert(periodeResultater[3].personIdent == fnr2)
        assert(periodeResultater[3].vilkårResultater.any { it.vilkårType == Vilkår.LOVLIG_OPPHOLD })
    }
}