package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseGenerator
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilRegelverkService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utbetaling.UtbetalingTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.util.mockPeriodeBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.OvergangsstønadService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.tidslinje.tomTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class KompetanseServiceTest {
    val mockKompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse> = mockPeriodeBarnSkjemaRepository()
    val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService = mockk()
    val utbetalingTidslinjeService: UtbetalingTidslinjeService = mockk()
    val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService = mockk()
    val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    val overgangsstønadServiceMock: OvergangsstønadService = mockk()
    val vilkårsvurderingServiceMock: VilkårsvurderingService = mockk()
    val unleashServiceMock: UnleashNextMedContextService = mockk()
    val tilkjentYtelseGenerator = TilkjentYtelseGenerator(overgangsstønadServiceMock, vilkårsvurderingServiceMock, unleashServiceMock)
    val clockProvider = TestClockProvider()

    val kompetanseService =
        KompetanseService(
            mockKompetanseRepository,
            emptyList(),
        )

    val tilpassKompetanserTilRegelverkService =
        TilpassKompetanserTilRegelverkService(
            vilkårsvurderingTidslinjeService = vilkårsvurderingTidslinjeService,
            utbetalingTidslinjeService = utbetalingTidslinjeService,
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            kompetanseRepository = mockKompetanseRepository,
            endringsabonnenter = emptyList(),
            clockProvider = clockProvider,
        )

    @BeforeEach
    fun init() {
        mockKompetanseRepository.deleteAll()
        every { overgangsstønadServiceMock.hentOgLagrePerioderMedOvergangsstønadForBehandling(any(), any()) } returns mockkObject()
        every { overgangsstønadServiceMock.hentPerioderMedFullOvergangsstønad(any<Behandling>()) } answers { emptyList() }
        every { unleashServiceMock.isEnabled(FeatureToggle.SKAL_INKLUDERE_ÅRSAK_ENDRE_MOTTAKER_I_INITIELL_GENERERING_AV_ANDELER) } returns true
    }

    @Test
    fun `bare reduksjon av periode skal ikke føre til endring i kompetansen`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)

        val lagretKompetanse =
            kompetanse(jan(2020), behandlingId, "SSSSSSSS", barn1)
                .lagreTil(mockKompetanseRepository)

        val oppdatertKompetanse = kompetanse(jan(2020), "  SSSSS  ", barn1)
        kompetanseService.oppdaterKompetanse(behandlingId, oppdatertKompetanse)

        val forventedeKompetanser = listOf(lagretKompetanse)

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))
    }

    @Test
    fun `oppdatering som splitter kompetanse fulgt av sletting skal returnere til utgangspunktet`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val lagretKompetanse =
            kompetanse(jan(2020), behandlingId, "---------", barn1, barn2, barn3)
                .lagreTil(mockKompetanseRepository)

        val oppdatertKompetanse = kompetanse(jan(2020), "  PP", barn2, barn3)

        kompetanseService.oppdaterKompetanse(behandlingId, oppdatertKompetanse)

        val forventedeKompetanser =
            KompetanseBuilder(jan(2020), behandlingId)
                .medKompetanse("--", barn1, barn2, barn3)
                .medKompetanse("  --", barn1)
                .medKompetanse("  PP", barn2, barn3)
                .medKompetanse("    -----", barn1, barn2, barn3)
                .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))

        val kompetanseSomSkalSlettes = kompetanseService.finnKompetanse(behandlingId, oppdatertKompetanse)
        kompetanseService.slettKompetanse(behandlingId, kompetanseSomSkalSlettes.id)

        assertEqualsUnordered(listOf(lagretKompetanse), kompetanseService.hentKompetanser(behandlingId))
    }

    @Test
    fun `oppdatering som endrer deler av en kompetanse, skal resultarere i en splitt`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("SSS", barn1)
            .medKompetanse("---------", barn2, barn3)
            .medKompetanse("   SSSS", barn1)
            .lagreTil(mockKompetanseRepository)

        val oppdatertKompetanse = kompetanse(jan(2020), "PP", barn1)
        kompetanseService.oppdaterKompetanse(behandlingId, oppdatertKompetanse)

        val forventedeKompetanser =
            KompetanseBuilder(jan(2020), behandlingId)
                .medKompetanse("PP", barn1)
                .medKompetanse("  SSSSS", barn1)
                .medKompetanse("---------", barn2, barn3)
                .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, kompetanseService.hentKompetanser(behandlingId))
    }

    @Test
    fun `skal kunne sende inn oppdatering som overlapper flere kompetanser`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("SSS", barn1)
            .medKompetanse("---------", barn2, barn3)
            .medKompetanse("   SSSS", barn1)
            .lagreTil(mockKompetanseRepository)

        val oppdatertKompetanse = kompetanse(mar(2020), "PPP", barn1, barn2, barn3)
        kompetanseService.oppdaterKompetanse(behandlingId, oppdatertKompetanse)

        val forventedeKompetanser =
            KompetanseBuilder(jan(2020), behandlingId)
                .medKompetanse("SS   SS", barn1)
                .medKompetanse("  PPP", barn1, barn2, barn3)
                .medKompetanse("--   ----", barn2, barn3)
                .byggKompetanser()

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal kunne lukke åpen kompetanse ved å sende inn identisk skjema med til-og-med-dato`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        // Åpen (til-og-med er null) kompetanse med sekundærland for tre barn
        KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("S>", barn1, barn2, barn3)
            .lagreTil(mockKompetanseRepository)

        // Endrer kun til-og-med dato fra uendelig (null) til en gitt dato
        val oppdatertKompetanse = kompetanse(jan(2020), "SSS", barn1, barn2, barn3)
        kompetanseService.oppdaterKompetanse(behandlingId, oppdatertKompetanse)

        // Forventer tomt skjema fra oppdatert dato og fremover
        val forventedeKompetanser =
            KompetanseBuilder(jan(2020), behandlingId)
                .medKompetanse("SSS->", barn1, barn2, barn3)
                .byggKompetanser()

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal kunne forkorte til-og-med ved å sende inn identisk skjema med tidligere til-og-med-dato`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        // Kompetanse med sekundærland for tre barn med til-og-med-dato
        KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("SSSSSSS", barn1, barn2, barn3)
            .lagreTil(mockKompetanseRepository)

        // Endrer kun til-og-med dato til tidligere tidspunkt
        val oppdatertKompetanse = kompetanse(jan(2020), "SSS", barn1, barn2, barn3)
        kompetanseService.oppdaterKompetanse(behandlingId, oppdatertKompetanse)

        // Forventer tomt skjema fra oppdatert dato og fremover til orignal til-og-med
        val forventedeKompetanser =
            KompetanseBuilder(jan(2020), behandlingId)
                .medKompetanse("SSS----", barn1, barn2, barn3)
                .byggKompetanser()

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal opprette tomt skjema for barn som fjernes fra ellers uendret skjema`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        // Åpen (til-og-med er null) kompetanse med sekundærland for tre barn
        KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("S>", barn1, barn2, barn3)
            .lagreTil(mockKompetanseRepository)

        // Fjerner ett barn fra gjeldende skjema, ellers likt
        val oppdatertKompetanse = kompetanse(jan(2020), "S>", barn1, barn2)
        kompetanseService.oppdaterKompetanse(behandlingId, oppdatertKompetanse)

        // Forventer tomt skjema for samme periode for barnet som ble fjernet
        val forventedeKompetanser =
            KompetanseBuilder(jan(2020), behandlingId)
                .medKompetanse("S>", barn1, barn2)
                .medKompetanse("->", barn3)
                .byggKompetanser()

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `kompetanse skal vare uendelig når til regelverk-tidslinjer fortsetter etter nåtidspunktet`() {
        val behandlingId = BehandlingId(10L)

        val treMånederSiden = YearMonth.now().minusMonths(3)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = treMånederSiden.toLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = treMånederSiden.toLocalDate())

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder()
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

        val forventedeKompetanser =
            KompetanseBuilder(treMånederSiden.nesteMåned(), behandlingId)
                .medKompetanse("->", barn1, barn2)
                .byggKompetanser()

        val vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering()
        val vilkårsvurderingTidslinjer =
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurdering,
                søkerOgBarn =
                    lagTestPersonopplysningGrunnlag(behandlingId.id, søker, barn1, barn2)
                        .tilPersonEnkelSøkerOgBarn(),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId.id, personer = mutableSetOf(søker, barn1, barn2)),
            )

        every { vilkårsvurderingTidslinjeService.hentTidslinjerThrows(behandlingId) } returns vilkårsvurderingTidslinjer
        every { vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId) } returns tomTidslinje()
        every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId.id) } returns emptyList()
        every { utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(behandlingId, emptyList()) } returns emptyMap()
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId.id) } returns tilkjentYtelse.andelerTilkjentYtelse.toList().map { AndelTilkjentYtelseMedEndreteUtbetalinger(it, emptyList()) }

        tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(behandlingId)

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `kompetanse skal ha sluttdato når til regelverk-tidslinjer avsluttes før nåtidspunktet`() {
        val behandlingId = BehandlingId(10L)

        val seksMånederSiden = YearMonth.now().minusMonths(6)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = seksMånederSiden.toLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = seksMånederSiden.toLocalDate())

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder()
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

        val forventedeKompetanser =
            KompetanseBuilder(seksMånederSiden.nesteMåned(), behandlingId)
                .medKompetanse("--", barn1, barn2) // Begge barna har 3 mnd EØS-regelverk før nå-tidspunktet
                .medKompetanse("  ->", barn1) // Bare barn 1 har EØS-regelverk etter nå-tidspunktet
                .byggKompetanser()

        val vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering()
        val vilkårsvurderingTidslinjer =
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurdering,
                søkerOgBarn =
                    lagTestPersonopplysningGrunnlag(behandlingId.id, søker, barn1, barn2)
                        .tilPersonEnkelSøkerOgBarn(),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId.id, personer = mutableSetOf(søker, barn1, barn2)),
            )

        every { vilkårsvurderingTidslinjeService.hentTidslinjerThrows(behandlingId) } returns vilkårsvurderingTidslinjer
        every { vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId) } returns tomTidslinje()
        every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId.id) } returns emptyList()
        every { utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(behandlingId, emptyList()) } returns emptyMap()
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId.id) } returns tilkjentYtelse.andelerTilkjentYtelse.toList().map { AndelTilkjentYtelseMedEndreteUtbetalinger(it, emptyList()) }

        tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(behandlingId)

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)
        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal tilpasse kompetanser til endrede regelverk-tidslinjer`() {
        val behandlingId = BehandlingId(10L)

        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).toLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).toLocalDate())
        val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).toLocalDate())

        KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("SS   SS", barn1)
            .medKompetanse("  PPP", barn1, barn2, barn3)
            .medKompetanse("--   ----", barn2, barn3)
            .lagreTil(mockKompetanseRepository)

        val vilkårsvurderingBygger =
            VilkårsvurderingBuilder()
                .forPerson(søker, jan(2020))
                .medVilkår("EEEEEEEEEEE", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD)
                .forPerson(barn1, jan(2020))
                .medVilkår("+++++++++++", Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
                .medVilkår("EEEEEEEEEEE", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD, Vilkår.BOR_MED_SØKER)
                .forPerson(barn2, jan(2020))
                .medVilkår("  ++++", Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
                .medVilkår("  EEEE", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD, Vilkår.BOR_MED_SØKER)
                .forPerson(barn3, jan(2020))
                .medVilkår("+>", Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
                .medVilkår("N>", Vilkår.BOSATT_I_RIKET, Vilkår.LOVLIG_OPPHOLD, Vilkår.BOR_MED_SØKER)

        val vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering()
        val vilkårsvurderingTidslinjer =
            VilkårsvurderingTidslinjer(
                vilkårsvurdering = vilkårsvurdering,
                søkerOgBarn =
                    lagTestPersonopplysningGrunnlag(behandlingId.id, søker, barn1, barn2, barn3)
                        .tilPersonEnkelSøkerOgBarn(),
            )

        every { vilkårsvurderingServiceMock.hentAktivForBehandlingThrows(any()) } returns vilkårsvurdering

        val tilkjentYtelse =
            tilkjentYtelseGenerator.genererTilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId.id, personer = mutableSetOf(søker, barn1, barn2, barn3)),
            )

        every { vilkårsvurderingTidslinjeService.hentTidslinjerThrows(behandlingId) } returns vilkårsvurderingTidslinjer
        every { vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId) } returns tomTidslinje()
        every { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId.id) } returns emptyList()
        every { utbetalingTidslinjeService.hentEndredeUtbetalingsPerioderSomKreverKompetanseTidslinjer(behandlingId, emptyList()) } returns emptyMap()
        every { andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId.id) } returns tilkjentYtelse.andelerTilkjentYtelse.toList().map { AndelTilkjentYtelseMedEndreteUtbetalinger(it, emptyList()) }

        tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(behandlingId)

        val faktiskeKompetanser = kompetanseService.hentKompetanser(behandlingId)

        val forventedeKompetanser =
            KompetanseBuilder(jan(2020), behandlingId)
                .medKompetanse(" SP  SS----", barn1)
                .medKompetanse("     -", barn2)
                .medKompetanse("   PP ", barn1, barn2)
                .byggKompetanser()

        assertEqualsUnordered(forventedeKompetanser, faktiskeKompetanser)
    }

    @Test
    fun `skal kopiere over kompetanse-skjema fra forrige behandling til ny behandling`() {
        val behandlingId1 = BehandlingId(10L)
        val behandlingId2 = BehandlingId(11L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val kompetanser =
            KompetanseBuilder(jan(2020), behandlingId1)
                .medKompetanse(
                    "SSS",
                    barn1,
                    annenForeldersAktivitetsland = null,
                    erAnnenForelderOmfattetAvNorskLovgivning = true,
                ).medKompetanse(
                    "---------",
                    barn2,
                    barn3,
                    annenForeldersAktivitetsland = null,
                    erAnnenForelderOmfattetAvNorskLovgivning = false,
                ).medKompetanse(
                    "   SSSS",
                    barn1,
                    annenForeldersAktivitetsland = null,
                    erAnnenForelderOmfattetAvNorskLovgivning = true,
                ).lagreTil(mockKompetanseRepository)

        kompetanseService.kopierOgErstattKompetanser(behandlingId1, behandlingId2)

        val kompetanserBehandling2 = kompetanseService.hentKompetanser(behandlingId2)

        assertEqualsUnordered(kompetanser, kompetanserBehandling2)

        kompetanserBehandling2.forEach {
            assertEquals(behandlingId2.id, it.behandlingId)
        }

        val kompetanserBehandling1 = kompetanseService.hentKompetanser(behandlingId1)

        kompetanserBehandling1.forEach {
            assertEquals(behandlingId1.id, it.behandlingId)
        }

        assertEqualsUnordered(kompetanser, kompetanserBehandling1)
    }

    @Test
    fun `skal kopiere kompetanser fra en behandling til en annen behandling, og overskrive eksisterende`() {
        val behandlingId1 = BehandlingId(10L)
        val behandlingId2 = BehandlingId(22L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val kompetanser1 =
            KompetanseBuilder(jan(2020), behandlingId1)
                .medKompetanse("SS   SS", barn1)
                .medKompetanse("  PPP", barn1, barn2, barn3)
                .medKompetanse("--   ----", barn2, barn3)
                .lagreTil(mockKompetanseRepository)

        KompetanseBuilder(jan(2020), behandlingId2)
            .medKompetanse("PPPSSSPPPPPPP", barn1, barn2, barn3)
            .lagreTil(mockKompetanseRepository)

        kompetanseService.kopierOgErstattKompetanser(behandlingId1, behandlingId2)

        val kompetanserBehandling2EtterEndring = kompetanseService.hentKompetanser(behandlingId2)

        assertEqualsUnordered(kompetanser1, kompetanserBehandling2EtterEndring)

        kompetanserBehandling2EtterEndring.forEach {
            assertEquals(behandlingId2.id, it.behandlingId)
        }

        val kompetanserBehandling1EtterEndring = kompetanseService.hentKompetanser(behandlingId1)

        assertEqualsUnordered(kompetanser1, kompetanserBehandling1EtterEndring)

        kompetanserBehandling1EtterEndring.forEach {
            assertEquals(behandlingId1.id, it.behandlingId)
        }
    }
}

fun kompetanse(
    tidspunkt: YearMonth,
    behandlingId: BehandlingId,
    s: String,
    vararg barn: Person,
) = KompetanseBuilder(tidspunkt, behandlingId).medKompetanse(s, *barn).byggKompetanser().first()

fun kompetanse(
    tidspunkt: YearMonth,
    s: String,
    vararg barn: Person,
) = KompetanseBuilder(tidspunkt).medKompetanse(s, *barn).byggKompetanser().first()

private fun KompetanseService.finnKompetanse(
    behandlingId: BehandlingId,
    kompetanse: Kompetanse,
): Kompetanse =
    this
        .hentKompetanser(behandlingId)
        .first { it == kompetanse }

fun Kompetanse.lagreTil(kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>): Kompetanse = kompetanseRepository.saveAll(listOf(this)).first()
