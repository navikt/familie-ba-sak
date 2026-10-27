package no.nav.familie.ba.sak.internal.tabulærtest

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.datagenerator.lagKompetanse
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.datagenerator.lagValutakurs
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class GenererTabulærTestServiceTest {
    private val behandlingService = mockk<BehandlingHentOgPersisterService>()
    private val vilkårService = mockk<VilkårService>()
    private val persongrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val andelRepository = mockk<AndelTilkjentYtelseRepository>()
    private val endretUtbetalingRepository = mockk<EndretUtbetalingAndelRepository>()
    private val kompetanseRepository = mockk<KompetanseRepository>()
    private val utenlandskPeriodebeløpRepository = mockk<UtenlandskPeriodebeløpRepository>()
    private val valutakursRepository = mockk<ValutakursRepository>()
    private val søknadGrunnlagService = mockk<SøknadGrunnlagService>()
    private val service =
        GenererTabulærTestService(
            behandlingService,
            vilkårService,
            persongrunnlagRepository,
            andelRepository,
            endretUtbetalingRepository,
            kompetanseRepository,
            utenlandskPeriodebeløpRepository,
            valutakursRepository,
            søknadGrunnlagService,
        )
    private val behandling = lagBehandling(id = 123456789345)
    private val forrigeBehandling = lagBehandling(fagsak = behandling.fagsak, id = 123456789012)
    private val grunnlag =
        lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = "12345678901",
            barnasIdenter = listOf("12345678902", "12345678903"),
            barnasFødselsdatoer = listOf(LocalDate.of(2019, 1, 1), LocalDate.of(2021, 1, 1)),
        )
    private val forrigeGrunnlag =
        lagTestPersonopplysningGrunnlag(
            forrigeBehandling.id,
            *grunnlag.personer.filter { it.fødselsdato.year != 2021 }.toTypedArray(),
        )
    private val fom = YearMonth.of(2023, 1)
    private val tom = YearMonth.of(2023, 12)

    @BeforeEach
    fun settOpp() {
        every { behandlingService.hent(behandling.id) } returns behandling
        every { behandlingService.hentForrigeBehandlingSomErVedtatt(behandling) } returns forrigeBehandling
        every { søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling, any()) } returns grunnlag.barna.map { it.aktør }

        listOf(behandling to grunnlag, forrigeBehandling to forrigeGrunnlag).forEach { (behandling, persongrunnlag) ->
            val barn = persongrunnlag.barna.toSet()
            val barnAktører = barn.map { it.aktør }.toSet()
            every { persongrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns persongrunnlag
            every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns
                lagVilkårsvurdering(behandling = behandling) { vurdering ->
                    persongrunnlag.personer
                        .map {
                            lagPersonResultat(vurdering, it, periodeFom = fom.atDay(1), periodeTom = tom.atEndOfMonth())
                        }.toSet()
                }
            every { andelRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
                barn.map { lagAndelTilkjentYtelse(fom, tom, behandling = behandling, person = it) }
            every { endretUtbetalingRepository.findByBehandlingId(behandling.id) } returns
                listOf(lagEndretUtbetalingAndel(behandlingId = behandling.id, aktører = barnAktører, fom = fom, tom = tom))
            every { kompetanseRepository.finnFraBehandlingId(behandling.id) } returns
                listOf(
                    lagKompetanse(
                        behandlingId = behandling.id,
                        fom = fom,
                        tom = tom,
                        barnAktører = barnAktører,
                        søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                        annenForeldersAktivitet = KompetanseAktivitet.ARBEIDER,
                        søkersAktivitetsland = "NOR",
                        annenForeldersAktivitetsland = "SWE",
                        barnetsBostedsland = "NOR",
                        kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                        erAnnenForelderOmfattetAvNorskLovgivning = false,
                    ),
                )
            every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandling.id) } returns
                listOf(
                    lagUtenlandskPeriodebeløp(
                        behandlingId = behandling.id,
                        fom = fom,
                        tom = tom,
                        barnAktører = barnAktører,
                        beløp = BigDecimal(100),
                        valutakode = "SEK",
                        intervall = Intervall.MÅNEDLIG,
                        utbetalingsland = "SWE",
                    ),
                )
            every { valutakursRepository.finnFraBehandlingId(behandling.id) } returns
                listOf(
                    lagValutakurs(
                        behandlingId = behandling.id,
                        fom = fom,
                        tom = tom,
                        barnAktører = barnAktører,
                        valutakursdato = fom.atDay(1),
                        valutakode = "SEK",
                        kurs = BigDecimal.ONE,
                        vurderingsform = Vurderingsform.MANUELL,
                    ),
                )
        }
    }

    @Test
    fun `skal bruke standardvalg når bare behandlingId er oppgitt`() {
        // Arrange
        val valg = GenererTabulærTestDto(behandling.id)

        // Act
        val tekst = service.genererTest(valg)

        // Assert
        assertThat(tekst).contains("Gitt følgende fagsaker", "Gitt følgende behandlinger", "Og følgende persongrunnlag")
        Datavalg.entries.forEach { datavalg ->
            datavalg.tittelTekst.forEach { tittelTekst ->
                assertThat(tekst.contains(tittelTekst)).describedAs(tittelTekst).isEqualTo(datavalg.medSomStandard)
            }
        }
    }

    @ParameterizedTest(name = "{0}: inkluder={1}")
    @MethodSource("datavalg")
    fun `skal følge eksplisitt inn- og utvalg`(
        datavalg: Datavalg,
        medSomStandard: Boolean,
    ) {
        // Arrange
        val valg = datavalg.sett(GenererTabulærTestDto(behandling.id), medSomStandard)

        // Act
        val tekst = service.genererTest(valg)

        // Assert
        datavalg.tittelTekst.forEach { tittelTekst ->
            assertThat(tekst.contains(tittelTekst)).describedAs(tittelTekst).isEqualTo(medSomStandard)
        }
    }

    @Test
    fun `skal inkludere forrige vedtatte behandling og dens grunnlag som standard`() {
        // Arrange
        val valg = GenererTabulærTestDto(behandling.id)

        // Act
        val tekst = service.genererTest(valg)

        // Assert
        val behandlinger = tekst.raderEtter("Gitt følgende behandlinger")
        assertThat(behandlinger.map { it["BehandlingId"] }).containsExactly("1", "2")
        assertThat(behandlinger.map { it["ForrigeBehandlingId"] }).containsExactly("", "1")
        assertThat(tekst.raderEtter("Og følgende persongrunnlag").map { it["BehandlingId"] }).containsOnly("1", "2")
        assertThat(tekst.raderEtter("Og med andeler tilkjent ytelse").map { it["BehandlingId"] }).containsOnly("1", "2")
        assertThat(tekst).contains("Og legg til nye vilkårresultater for behandling 1", "Og legg til nye vilkårresultater for behandling 2")
    }

    @Test
    fun `skal utelate forrige behandling og dens grunnlag når den velges bort`() {
        // Arrange
        val valg = GenererTabulærTestDto(behandling.id, inkluderForrigeBehandling = false)

        // Act
        val tekst = service.genererTest(valg)

        // Assert
        sjekkBareAktuellBehandling(tekst)
    }

    @Test
    fun `skal lage oppsett uten forrige behandling når ingen er vedtatt`() {
        // Arrange
        every { behandlingService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null

        // Act
        val tekst = service.genererTest(GenererTabulærTestDto(behandling.id))

        // Assert
        sjekkBareAktuellBehandling(tekst)
    }

    @Test
    fun `skal bruke konsistente anonyme referanser i alle databolker og behandlinger`() {
        // Arrange
        val tabulærTestValg =
            GenererTabulærTestDto(
                behandlingId = behandling.id,
                inkluderKompetanser = true,
                inkluderUtenlandskePeriodebeløp = true,
                inkluderValutakurser = true,
            )

        // Act
        val testSomString = service.genererTest(tabulærTestValg)

        // Assert
        assertThat(testSomString).doesNotContain(behandling.id.toString(), forrigeBehandling.id.toString(), *grunnlag.personer.map { it.aktør.aktørId }.toTypedArray(), *forrigeGrunnlag.personer.map { it.aktør.aktørId }.toTypedArray())
    }

    private fun sjekkBareAktuellBehandling(tekst: String) {
        val behandlinger = tekst.raderEtter("Gitt følgende behandlinger")
        assertThat(behandlinger).hasSize(1)
        assertThat(behandlinger.single()["BehandlingId"]).isEqualTo("1")
        assertThat(behandlinger.single()["ForrigeBehandlingId"]).isEmpty()
        assertThat(tekst.raderEtter("Og følgende persongrunnlag")).hasSize(3)
        assertThat(tekst.raderEtter("Og følgende persongrunnlag").map { it["BehandlingId"] }).containsOnly("1")
        assertThat(tekst.raderEtter("Og med andeler tilkjent ytelse")).hasSize(2)
        assertThat(tekst.raderEtter("Og med andeler tilkjent ytelse").map { it["BehandlingId"] }).containsOnly("1")
        assertThat(tekst).contains("Og legg til nye vilkårresultater for behandling 1")
        assertThat(tekst).doesNotContain("Og legg til nye vilkårresultater for behandling 2")
    }

    private fun String.raderEtter(steg: String): List<Map<String, String>> {
        val tabell =
            lineSequence()
                .map { it.trim() }
                .dropWhile { it != steg }
                .drop(1)
                .dropWhile { it.isBlank() }
                .takeWhile { it.startsWith("|") || it.isBlank() }
                .filter { it.startsWith("|") }
                .map { linje -> linje.removeSurrounding("|").split("|").map { it.trim() } }
                .toList()
        val kolonner = tabell.first()
        return tabell.drop(1).map { kolonner.zip(it).toMap() }
    }

    enum class Datavalg(
        val medSomStandard: Boolean,
        vararg val tittelTekst: String,
    ) {
        DAGENS_DATO(true, "Og dagens dato er"),
        PERSONER_FREMSTILT_KRAV_FOR(true, "Og med personer fremstilt krav for"),
        VILKÅRSVURDERING(true, "Og lag personresultater", "Og legg til nye vilkårresultater"),
        ANDELER_TILKJENT_YTELSE(true, "Og med andeler tilkjent ytelse"),
        ENDREDE_UTBETALINGER(true, "Og med endrede utbetalinger"),
        KOMPETANSER(false, "Og med kompetanser"),
        UTENLANDSKE_PERIODEBELØP(false, "Og med utenlandsk periodebeløp"),
        VALUTAKURSER(false, "Og med valutakurser"),
        ;

        fun sett(
            valg: GenererTabulærTestDto,
            inkluder: Boolean,
        ): GenererTabulærTestDto =
            when (this) {
                DAGENS_DATO -> valg.copy(inkluderDagensDato = inkluder)
                PERSONER_FREMSTILT_KRAV_FOR -> valg.copy(inkluderPersonerFremstiltKravFor = inkluder)
                VILKÅRSVURDERING -> valg.copy(inkluderVilkårsvurdering = inkluder)
                ANDELER_TILKJENT_YTELSE -> valg.copy(inkluderAndelerTilkjentYtelse = inkluder)
                ENDREDE_UTBETALINGER -> valg.copy(inkluderEndredeUtbetalinger = inkluder)
                KOMPETANSER -> valg.copy(inkluderKompetanser = inkluder)
                UTENLANDSKE_PERIODEBELØP -> valg.copy(inkluderUtenlandskePeriodebeløp = inkluder)
                VALUTAKURSER -> valg.copy(inkluderValutakurser = inkluder)
            }
    }

    companion object {
        @JvmStatic
        fun datavalg(): List<Arguments> = Datavalg.entries.flatMap { valg -> listOf(true, false).map { Arguments.of(valg, it) } }
    }
}
