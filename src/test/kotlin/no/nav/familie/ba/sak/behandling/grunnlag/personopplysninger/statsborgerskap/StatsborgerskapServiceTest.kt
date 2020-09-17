package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.ClientMocks.Companion.FOM_1990
import no.nav.familie.ba.sak.config.ClientMocks.Companion.FOM_2000
import no.nav.familie.ba.sak.config.ClientMocks.Companion.FOM_2004
import no.nav.familie.ba.sak.config.ClientMocks.Companion.FOM_2008
import no.nav.familie.ba.sak.config.ClientMocks.Companion.FOM_2010
import no.nav.familie.ba.sak.config.ClientMocks.Companion.TOM_2000
import no.nav.familie.ba.sak.config.ClientMocks.Companion.TOM_2004
import no.nav.familie.ba.sak.config.ClientMocks.Companion.TOM_2010
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
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
    private val integrasjonClient = mockk<IntegrasjonClient>()

    private val personopplysningerService= mockk<PersonopplysningerService>()

    private lateinit var statsborgerskapService: StatsborgerskapService

    @BeforeEach
    fun setUp() {
        statsborgerskapService = StatsborgerskapService(integrasjonClient, personopplysningerService)
        ClientMocks.initEuKodeverk(integrasjonClient)
    }

    @Test
    fun `Sjekk dobbel statsborgerskap for Tredjeland, EØS og NORDEN`() {

        every { personopplysningerService.hentStatsborgerskap(Ident("0011")) }.returns(
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
        every { personopplysningerService.hentStatsborgerskap(Ident("0011")) }.returns(
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

        every { personopplysningerService.hentStatsborgerskap(Ident("0011")) }.returns(
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

        every { personopplysningerService.hentStatsborgerskap(Ident("0011")) }.returns(
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

        every { personopplysningerService.hentStatsborgerskap(Ident("0011")) }.returns(
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
}
