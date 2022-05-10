package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.MinnebasertKompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.Tidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class KompetanseServiceTest {

    val minnebasertKompetanseRepository = MinnebasertKompetanseRepository()
    val mockKompetanseRepository = mockk<KompetanseRepository>()
    val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk(relaxed = true)
    val tidslinjeService: TidslinjeService = mockk()

    val kompetanseService = KompetanseService(
        tidslinjeService,
        mockKompetanseRepository,
        tilbakestillBehandlingService,
    )

    @BeforeEach
    fun init() {
        val idSlot = slot<Long>()
        val kompetanseListeSlot = slot<Iterable<Kompetanse>>()

        every { mockKompetanseRepository.findByBehandlingId(capture(idSlot)) } answers {
            minnebasertKompetanseRepository.hentKompetanser(idSlot.captured)
        }

        every { mockKompetanseRepository.getById(capture(idSlot)) } answers {
            minnebasertKompetanseRepository.hentKompetanse(idSlot.captured)
        }

        every { mockKompetanseRepository.saveAll(capture(kompetanseListeSlot)) } answers {
            minnebasertKompetanseRepository.save(kompetanseListeSlot.captured)
        }

        every { mockKompetanseRepository.deleteAll(capture(kompetanseListeSlot)) } answers {
            minnebasertKompetanseRepository.delete(kompetanseListeSlot.captured)
        }
    }

    @Test
    fun `bare reduksjon av periode skal ikke føre til endring i kompetansen`() {
        val behandlingId = 10L
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)

        val lagretKompetanse = kompetanse(jan(2020), behandlingId, "SSSSSSSS", barn1)
            .lagreTil(mockKompetanseRepository)

        val oppdatertKompetanse = kompetanse(jan(2020), "  SSSSS  ", barn1)
        kompetanseService.oppdaterKompetanse(lagretKompetanse.id, oppdatertKompetanse)

        val forventedeKompetanser = listOf(lagretKompetanse)

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))
    }

    @Test
    fun `oppdatering som splitter kompetanse fulgt av sletting skal returnere til utgangspunktet`() {
        val behandlingId = 10L
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val lagretKompetanse = kompetanse(jan(2020), behandlingId, "---------", barn1, barn2, barn3)
            .lagreTil(mockKompetanseRepository)

        val oppdatertKompetanse = kompetanse(jan(2020), "  PP", barn2, barn3)

        kompetanseService.oppdaterKompetanse(lagretKompetanse.id, oppdatertKompetanse)

        val forventedeKompetanser = KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("--", barn1, barn2, barn3)
            .medKompetanse("  --", barn1)
            .medKompetanse("  PP", barn2, barn3)
            .medKompetanse("    -----", barn1, barn2, barn3)
            .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))

        val kompetanseSomSkalSlettes = kompetanseService.finnKompetanse(behandlingId, oppdatertKompetanse)
        kompetanseService.slettKompetanse(kompetanseSomSkalSlettes.id)

        assertEqualsUnordered(listOf(lagretKompetanse), kompetanseService.hentKompetanser(behandlingId))
    }

    @Test
    fun `oppdatering som endrer deler av en kompetanse, skal resultarere i en splitt`() {
        val behandlingId = 10L
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("SSS", barn1)
            .medKompetanse("---------", barn2, barn3)
            .medKompetanse("   SSSS", barn1)
            .lagreTil(mockKompetanseRepository)

        val lagretKompetanse = kompetanseService.finnKompetanse(
            behandlingId, kompetanse(jan(2020), "SSS", barn1)
        )

        val oppdatertKompetanse = kompetanse(jan(2020), "PP", barn1)
        kompetanseService.oppdaterKompetanse(lagretKompetanse.id, oppdatertKompetanse)

        val forventedeKompetanser = KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("PP", barn1)
            .medKompetanse("  SSSSS", barn1)
            .medKompetanse("---------", barn2, barn3)
            .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))
    }

    @Test
    fun `kompetanse skal vare uendelig når til regelverk-tidslinjer fortsetter etter nåtidspunktet`() {
        val behandlingId = 10L

        val treMånederSiden = MånedTidspunkt.nå().flytt(-3)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = treMånederSiden.tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = treMånederSiden.tilLocalDate())

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>()
            .forPerson(søker, treMånederSiden) // Regelverk-tidslinje avslutter ETTER nå-tidspunkt
            .medVilkår("EEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, treMånederSiden) // Regelverk-tidslinje avslutter ETTER nå-tidspunkt
            .medVilkår("+++++++++++", Vilkår.UNDER_18_ÅR)
            .medVilkår("EEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEEEEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++++", Vilkår.GIFT_PARTNERSKAP)
            .forPerson(barn2, treMånederSiden) // Regelverk-tidslinje avslutter ETTER nå-tidspunkt
            .medVilkår("+++++++", Vilkår.UNDER_18_ÅR)
            .medVilkår("EEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()

        val forventedeKompetanser = KompetanseBuilder(treMånederSiden.neste(), behandlingId)
            .medKompetanse("->", barn1, barn2)
            .byggKompetanser()

        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId, søker, barn1, barn2)
        )

        every { tidslinjeService.hentTidslinjerThrows(behandlingId) } returns tidslinjer

        kompetanseService.tilpassKompetanserTilRegelverk(behandlingId)

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `kompetanse skal ha sluttdato når til regelverk-tidslinjer avsluttes før nåtidspunktet`() {
        val behandlingId = 10L

        val seksMånederSiden = MånedTidspunkt.nå().flytt(-6)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = seksMånederSiden.tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = seksMånederSiden.tilLocalDate())

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>()
            .forPerson(søker, seksMånederSiden) // Regelverk-tidslinje avslutter ETTER nå-tidspunkt
            .medVilkår("EEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, seksMånederSiden) // Regelverk-tidslinje avslutter ETTER nå-tidspunkt
            .medVilkår("+++++++++++", Vilkår.UNDER_18_ÅR)
            .medVilkår("EEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEEEEEEEEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++++", Vilkår.GIFT_PARTNERSKAP)
            .forPerson(barn2, seksMånederSiden) // Regelverk-tidslinje avslutter FØR nå-tidspunkt
            .medVilkår("+++", Vilkår.UNDER_18_ÅR)
            .medVilkår("EEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEE", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEE", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++", Vilkår.GIFT_PARTNERSKAP)
            .byggPerson()

        val forventedeKompetanser = KompetanseBuilder(seksMånederSiden.neste(), behandlingId)
            .medKompetanse("---", barn1, barn2) // Begge barna har 3 mnd EØS-regelverk før nå-tidspunktet
            .medKompetanse("   ->", barn1) // Bare barn 1 har EØS-regelverk etter nå-tidspunktet
            .byggKompetanser()

        val tidslinjer = Tidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandlingId, søker, barn1, barn2)
        )

        every { tidslinjeService.hentTidslinjerThrows(behandlingId) } returns tidslinjer

        kompetanseService.tilpassKompetanserTilRegelverk(behandlingId)

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }
}

fun kompetanse(tidspunkt: Tidspunkt<Måned>, behandlingId: Long, s: String, vararg barn: Person) =
    KompetanseBuilder(tidspunkt, behandlingId).medKompetanse(s, *barn).byggKompetanser().first()

fun kompetanse(tidspunkt: Tidspunkt<Måned>, s: String, vararg barn: Person) =
    KompetanseBuilder(tidspunkt).medKompetanse(s, *barn).byggKompetanser().first()

private fun KompetanseService.finnKompetanse(behandlingId: Long, kompetanse: Kompetanse): Kompetanse {
    return this.hentKompetanser(behandlingId)
        .first { it == kompetanse }
}

private fun KompetanseBuilder.lagreTil(kompetanseRepository: KompetanseRepository): List<Kompetanse> {
    val byggKompetanser = this.byggKompetanser()
    return kompetanseRepository.saveAll(byggKompetanser)
}

private fun Kompetanse.lagreTil(kompetanseRepository: KompetanseRepository): Kompetanse {
    return kompetanseRepository.saveAll(listOf(this)).first()
}
