package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.VilkårTmp
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
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

    fun personrestiltimeline(personres: List<PersonResultat>): LocalDateTimeline<List<VilkårTmp>> {
        val flattMedPerson: List<VilkårTmp> = personres.flatMap { personResultat ->
            personResultat.vilkårResultater.map {
                VilkårTmp(personIdent = personResultat.personIdent,
                          vilkårType = it.vilkårType,
                          begrunnelse = it.begrunnelse,
                          periodeFom = it.periodeFom,
                          periodeTom = it.periodeTom,
                          resultat = it.resultat)
            }
        }
        val tidslinjer: List<LocalDateTimeline<VilkårTmp>> =
                flattMedPerson.map { vilkårTmp -> vilkårResultatTilTimeline(vilkårTmp) }

        val førsteSegment = tidslinjer.first().toSegments().first()
        val initSammenlagtTidslinje =
                LocalDateTimeline(listOf(LocalDateSegment(førsteSegment.fom, førsteSegment.tom, listOf(førsteSegment.value))))
        val resterendeTidslinjer = tidslinjer.drop(1)
        return resterendeTidslinjer.fold(initSammenlagtTidslinje) { lhs, rhs -> (kombinerTidslinjer(lhs, rhs)) }
    }

    private fun vilkårResultatTilTimeline(it: VilkårTmp): LocalDateTimeline<VilkårTmp> =
            LocalDateTimeline(listOf(LocalDateSegment(it.periodeFom,
                                                      it.periodeTom,
                                                      it)))

    private fun kombinerTidslinjer(lhs: LocalDateTimeline<List<VilkårTmp>>,
                                   rhs: LocalDateTimeline<VilkårTmp>): LocalDateTimeline<List<VilkårTmp>> {
        return lhs.combine(rhs,
                           { dateInterval, left, right -> StandardCombinators.allValues<VilkårTmp>(dateInterval, left, right) },
                           LocalDateTimeline.JoinStyle.CROSS_JOIN)
    }

    @Test
    fun `Kombinert tidslinje returnerer rette rette vilkårsresultater for tidsintervaller`() {
        val d1 = LocalDate.now().minusMonths(4);
        val d2 = LocalDate.now().minusMonths(2)
        val d3 = LocalDate.now()
        val d4 = LocalDate.now().plusMonths(1)

        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.JA)

        val personResultat = PersonResultat(id = 1,
                                            behandlingResultat = behandlingResultat,
                                            personIdent = "")
        personResultat.vilkårResultater = setOf(
                VilkårResultat(id = 1,
                               personResultat = personResultat,
                               vilkårType = Vilkår.UNDER_18_ÅR,
                               resultat = Resultat.JA,
                               periodeFom = d1,
                               periodeTom = d3,
                               begrunnelse = "Fordi"),
                VilkårResultat(id = 2,
                               personResultat = personResultat,
                               vilkårType = Vilkår.BOSATT_I_RIKET,
                               resultat = Resultat.JA,
                               periodeFom = d2,
                               periodeTom = d4,
                               begrunnelse = "Fordi"))

        val kombinert = personrestiltimeline(listOf(personResultat))

        assert(kombinert.toSegments().size.equals(3))
        val verdier1 = kombinert.getSegment(LocalDateInterval(d1, d2))
        val verdier2 = kombinert.getSegment(LocalDateInterval(d2.plusDays(1), d3))
        val verdier3 = kombinert.getSegment(LocalDateInterval(d3.plusDays(1), d4))
        assert(verdier1.value.size.equals(1))
        assert(verdier2.value.size.equals(2))
        assert(verdier3.value.size.equals(1))

        assert(verdier1.value[0].vilkårType.equals(Vilkår.UNDER_18_ÅR))
        assert(verdier2.value[0].vilkårType.equals(Vilkår.UNDER_18_ÅR))
        assert(verdier2.value[1].vilkårType.equals(Vilkår.BOSATT_I_RIKET))
        assert(verdier3.value[0].vilkårType.equals(Vilkår.BOSATT_I_RIKET))
    }

    @Test
    fun `flere personer`() {
        val d1 = LocalDate.now().minusMonths(4);
        val d2 = LocalDate.now().minusMonths(2)
        val d3 = LocalDate.now()
        val d4 = LocalDate.now().plusMonths(1)
        val d5 = LocalDate.now().plusMonths(3)

        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val behandlingResultat = lagBehandlingResultat(fnr, behandling, Resultat.JA)

        val personResultat = PersonResultat(id = 1,
                                            behandlingResultat = behandlingResultat,
                                            personIdent = "")
        personResultat.vilkårResultater = setOf(
                VilkårResultat(id = 1,
                               personResultat = personResultat,
                               vilkårType = Vilkår.UNDER_18_ÅR,
                               resultat = Resultat.JA,
                               periodeFom = d1,
                               periodeTom = d3,
                               begrunnelse = "Fordi"),
                VilkårResultat(id = 2,
                               personResultat = personResultat,
                               vilkårType = Vilkår.BOSATT_I_RIKET,
                               resultat = Resultat.JA,
                               periodeFom = d2,
                               periodeTom = d4,
                               begrunnelse = "Fordi"))


        val personResultat2 = PersonResultat(id = 2,
                                            behandlingResultat = behandlingResultat,
                                            personIdent = "")
        personResultat2.vilkårResultater = setOf(
                VilkårResultat(id = 2,
                               personResultat = personResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.JA,
                               periodeFom = d2,
                               periodeTom = d5,
                               begrunnelse = "Fordi"))

        val kombinert = personrestiltimeline(listOf(personResultat, personResultat2))

        assert(kombinert.toSegments().size.equals(4))
        val verdier1 = kombinert.getSegment(LocalDateInterval(d1, d2))
        val verdier2 = kombinert.getSegment(LocalDateInterval(d2.plusDays(1), d3))
        val verdier3 = kombinert.getSegment(LocalDateInterval(d3.plusDays(1), d4))
        val verdier4 = kombinert.getSegment(LocalDateInterval(d4.plusDays(1), d5))
        assert(verdier1.value.size.equals(1))
        assert(verdier2.value.size.equals(3))
        assert(verdier3.value.size.equals(2))
        assert(verdier4.value.size.equals(1))

        assert(verdier1.value[0].vilkårType.equals(Vilkår.UNDER_18_ÅR))
        assert(verdier2.value[0].vilkårType.equals(Vilkår.UNDER_18_ÅR))
        assert(verdier2.value[1].vilkårType.equals(Vilkår.BOSATT_I_RIKET))
        assert(verdier2.value[2].vilkårType.equals(Vilkår.LOVLIG_OPPHOLD))
        assert(verdier3.value[0].vilkårType.equals(Vilkår.BOSATT_I_RIKET))
        assert(verdier3.value[1].vilkårType.equals(Vilkår.LOVLIG_OPPHOLD))
        assert(verdier4.value[0].vilkårType.equals(Vilkår.LOVLIG_OPPHOLD))
    }

    //TODO: Håndtering av motstridende samme vilkår
}
