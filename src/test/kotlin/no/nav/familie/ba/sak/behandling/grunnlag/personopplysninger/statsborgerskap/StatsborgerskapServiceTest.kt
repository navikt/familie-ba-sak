package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.personinfo.Ident
import no.nav.familie.kontrakter.felles.personinfo.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class StatsborgerskapServiceTest {

    val integrasjonClient = mockk<IntegrasjonClient>()

    lateinit var statsborgerskapService: StatsborgerskapService

    @BeforeEach
    fun setUp() {
        statsborgerskapService = StatsborgerskapService(integrasjonClient)
    }

    @Test
    fun `e`() {

        val beskrivelsePolen = BeskrivelseDto("POL", "")
        val betydningPolen = BetydningDto(LocalDate.of(2004, Month.JANUARY, 1), LocalDate.now(), mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen))
        val beskrivelseTyskland = BeskrivelseDto("DEU","")
        val betydningTyskland = BetydningDto(LocalDate.of(1990, Month.JANUARY, 1), LocalDate.now(), mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland))
        val beskrivelseDanmark = BeskrivelseDto("DEN","")
        val betydningDanmark = BetydningDto(LocalDate.now(), LocalDate.now(), mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark))
        val kodeverkLand = KodeverkDto(mapOf(
                "POL" to listOf(betydningPolen),
                "DEU" to listOf(betydningTyskland),
                "DEN" to listOf(betydningDanmark)))

        val sbDanmark = Statsborgerskap("DEN",
                                      gyldigFraOgMed = LocalDate.of(2018, Month.JANUARY, 1),
                                      gyldigTilOgMed = null)
        val sbFiji = Statsborgerskap("FJI",
                                      gyldigFraOgMed = LocalDate.of(1990, Month.JANUARY, 1),
                                      gyldigTilOgMed = LocalDate.of(2000, Month.DECEMBER, 31))
        val sbPolen = Statsborgerskap("POL",
                                      gyldigFraOgMed = LocalDate.of(2000, Month.JANUARY, 1),
                                      gyldigTilOgMed = LocalDate.of(2010, Month.DECEMBER, 31))
        val sbTyskland = Statsborgerskap("DEU",
                                      gyldigFraOgMed = LocalDate.of(2008, Month.JANUARY, 1),
                                      gyldigTilOgMed = null)


        every { integrasjonClient.hentStatsborgerskap(Ident("0011")) }
                .returns(listOf(sbPolen, sbTyskland, sbDanmark, sbFiji))

        every { integrasjonClient.hentAlleEØSLand() }
                .returns(kodeverkLand)

        val statsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskapOgHistorikk(Ident("0011"))
        //assertThat(statsborgerskap["FJI"].)
    }

}
