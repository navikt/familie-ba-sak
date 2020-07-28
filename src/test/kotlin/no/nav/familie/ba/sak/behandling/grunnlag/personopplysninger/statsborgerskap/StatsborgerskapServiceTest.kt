package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class StatsborgerskapServiceTest {
    val FOM_1900 = LocalDate.of(1900, Month.JANUARY, 1)
    val FOM_1990 = LocalDate.of(1990, Month.JANUARY, 1)
    val FOM_2000 = LocalDate.of(2000, Month.JANUARY, 1)
    val FOM_2004 = LocalDate.of(2004, Month.JANUARY, 1)
    val FOM_2008 = LocalDate.of(2008, Month.JANUARY, 1)
    val FOM_2010 = LocalDate.of(2010, Month.JANUARY, 1)
    val TOM_2000 = LocalDate.of(1999, Month.DECEMBER, 31)
    val TOM_2004 = LocalDate.of(2003, Month.DECEMBER, 31)
    val TOM_2010 = LocalDate.of(2009, Month.DECEMBER, 31)
    val TOM_9999 = LocalDate.of(9999, Month.DECEMBER, 31)

    private val integrasjonClient = mockk<IntegrasjonClient>()
    private lateinit var statsborgerskapService: StatsborgerskapService

    @BeforeEach
    fun setUp() {
        statsborgerskapService = StatsborgerskapService(integrasjonClient)
        initEuKodeverk()
    }

    @Test
    fun `Sjekk dobbel statsborgerskap for Tredjeland, EØS og NORDEN`() {

        every { integrasjonClient.hentStatsborgerskap(Ident("0011")) }.returns(
                listOf(
                        Statsborgerskap("POL",
                                        gyldigFraOgMed = FOM_1990,
                                        gyldigTilOgMed = TOM_2010),
                        Statsborgerskap("DNK",
                                        gyldigFraOgMed = FOM_2008,
                                        gyldigTilOgMed = null)
                )
        )

        val statsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident("0011"), tilfeldigPerson())

        assertThat(statsborgerskap.size == 3)
        assertStatsborgerskap(statsborgerskap[0], "POL", FOM_1990, TOM_2004, Medlemskap.TREDJELANDSBORGER)
        assertStatsborgerskap(statsborgerskap[1], "POL", FOM_2004, TOM_2010, Medlemskap.EØS)
        assertStatsborgerskap(statsborgerskap[2], "DNK", FOM_2008, null, Medlemskap.NORDEN)
    }

    @Test
    fun `Sjekk statsborgerskap perioder for Norden og ukjent`() {
        every { integrasjonClient.hentStatsborgerskap(Ident("0011")) }.returns(
                listOf(
                        Statsborgerskap("XUK",
                                        gyldigFraOgMed = FOM_1990,
                                        gyldigTilOgMed = TOM_2000),
                        Statsborgerskap("NOR",
                                        gyldigFraOgMed = FOM_2000,
                                        gyldigTilOgMed = null)
                )
        )

        val statsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident("0011"), tilfeldigPerson())

        assertThat(statsborgerskap.size == 2)
        assertStatsborgerskap(statsborgerskap[0], "XUK", FOM_1990, TOM_2000, Medlemskap.UKJENT)
        assertStatsborgerskap(statsborgerskap[1], "NOR", FOM_2000, null, Medlemskap.NORDEN)
    }

    @Test
    fun `Sjekk statsborgerskap perioder for exit eøs`() {

        every { integrasjonClient.hentStatsborgerskap(Ident("0011")) }.returns(
                listOf(
                        Statsborgerskap("GBR",
                                        gyldigFraOgMed = FOM_1990,
                                        gyldigTilOgMed = null)
                )
        )

        val statsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident("0011"), tilfeldigPerson())

        assertThat(statsborgerskap.size == 2)
        assertStatsborgerskap(statsborgerskap[0], "GBR", FOM_1990, TOM_2010, Medlemskap.EØS)
        assertStatsborgerskap(statsborgerskap[1], "GBR", FOM_2010, null, Medlemskap.TREDJELANDSBORGER)
    }

    @Test
    fun `Sjekk statsborgerskap med gyldig fra dato null`() {

        every { integrasjonClient.hentStatsborgerskap(Ident("0011")) }.returns(
                listOf(
                        Statsborgerskap("GBR",
                                        gyldigFraOgMed = null,
                                        gyldigTilOgMed = null)
                )
        )

        val statsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident("0011"), tilfeldigPerson())

        assertThat(statsborgerskap.size == 2)
        assertStatsborgerskap(statsborgerskap[0], "GBR", null, TOM_2010, Medlemskap.EØS)
        assertStatsborgerskap(statsborgerskap[1], "GBR", FOM_2010, null, Medlemskap.TREDJELANDSBORGER)
    }

    @Test
    fun `Sjekk statsborgerskap med EU-medlemskap`() {

        every { integrasjonClient.hentStatsborgerskap(Ident("0011")) }.returns(
                listOf(
                        Statsborgerskap("DEU",
                                        gyldigFraOgMed = null,
                                        gyldigTilOgMed = null)
                )
        )

        val statsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident("0011"), tilfeldigPerson())

        assertThat(statsborgerskap.size == 1)
        assertStatsborgerskap(statsborgerskap[0], "DEU", null, null, Medlemskap.EØS)
    }

    private fun assertStatsborgerskap(statsborgerskap: GrStatsborgerskap,
                                      landkode: String,
                                      fom: LocalDate?,
                                      tom: LocalDate?,
                                      medlemskap: Medlemskap) {
        assertThat(statsborgerskap.landkode).isEqualTo(landkode)
        assertThat(statsborgerskap.gyldigPeriode!!.fom).isEqualTo(fom)
        assertThat(statsborgerskap.gyldigPeriode!!.tom).isEqualTo(tom)
        assertThat(statsborgerskap.medlemskap).isEqualTo(medlemskap)

    }

    private fun initEuKodeverk() {
        val beskrivelsePolen = BeskrivelseDto("POL", "")
        val betydningPolen = BetydningDto(FOM_2004, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen))
        val beskrivelseTyskland = BeskrivelseDto("DEU", "")
        val betydningTyskland = BetydningDto(FOM_1900, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland))
        val beskrivelseDanmark = BeskrivelseDto("DEN", "")
        val betydningDanmark = BetydningDto(FOM_1990, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark))
        val beskrivelseUK = BeskrivelseDto("GBR", "")
        val betydningUK = BetydningDto(FOM_1900, TOM_2010, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseUK))

        val kodeverkLand = KodeverkDto(mapOf(
                "POL" to listOf(betydningPolen),
                "DEU" to listOf(betydningTyskland),
                "DEN" to listOf(betydningDanmark),
                "GBR" to listOf(betydningUK)))

        every { integrasjonClient.hentAlleEØSLand() }
                .returns(kodeverkLand)
    }
}
