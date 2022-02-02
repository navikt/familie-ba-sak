package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.config.IntegrasjonClientMock
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.FOM_1990
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.FOM_2004
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.TOM_2010
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnNåværendeMedlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnSterkesteMedlemskap
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class StatsborgerskapServiceTest {

    private val integrasjonClient = mockk<IntegrasjonClient>()

    private lateinit var statsborgerskapService: StatsborgerskapService

    @BeforeEach
    fun setUp() {
        statsborgerskapService = StatsborgerskapService(integrasjonClient)
        IntegrasjonClientMock.initEuKodeverk(integrasjonClient)
    }

    @Test
    fun `Skal generere GrStatsborgerskap med flere perioder fordi Polen ble medlem av EØS`() {
        val statsborgerskapMedGyldigFom = Statsborgerskap(
            "POL",
            bekreftelsesdato = null,
            gyldigFraOgMed = FOM_1990,
            gyldigTilOgMed = TOM_2010
        )

        val grStatsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskap(
            statsborgerskap = statsborgerskapMedGyldigFom,
            person = lagPerson()
        )

        assertEquals(2, grStatsborgerskap.size)
        assertEquals(FOM_1990, grStatsborgerskap.sortedBy { it.gyldigPeriode?.fom }.first().gyldigPeriode?.fom)
        assertEquals(
            Medlemskap.TREDJELANDSBORGER,
            grStatsborgerskap.sortedBy { it.gyldigPeriode?.fom }.first().medlemskap
        )

        assertEquals(FOM_2004, grStatsborgerskap.sortedBy { it.gyldigPeriode?.fom }.last().gyldigPeriode?.fom)
        assertEquals(Medlemskap.EØS, grStatsborgerskap.sortedBy { it.gyldigPeriode?.fom }.last().medlemskap)
    }

    @Test
    fun `Lovlig opphold - valider at alle gjeldende medlemskap blir returnert`() {
        val person = lagPerson()
            .also {
                it.statsborgerskap =
                    mutableListOf(
                        GrStatsborgerskap(
                            gyldigPeriode = DatoIntervallEntitet(tom = null, fom = null),
                            landkode = "DNK",
                            medlemskap = Medlemskap.NORDEN,
                            person = it
                        ),
                        GrStatsborgerskap(
                            gyldigPeriode = DatoIntervallEntitet(
                                tom = null,
                                fom = LocalDate.now().minusYears(1)
                            ),
                            landkode = "DEU",
                            medlemskap = Medlemskap.EØS,
                            person = it
                        ),
                        GrStatsborgerskap(
                            gyldigPeriode = DatoIntervallEntitet(
                                tom = LocalDate.now().minusYears(2),
                                fom = LocalDate.now().minusYears(2)
                            ),
                            landkode = "POL",
                            medlemskap = Medlemskap.EØS,
                            person = it
                        )
                    )
            }

        val medlemskap = finnNåværendeMedlemskap(person.statsborgerskap)

        assertEquals(2, medlemskap.size)
        assertEquals(Medlemskap.NORDEN, medlemskap[0])
        assertEquals(Medlemskap.EØS, medlemskap[1])
    }

    @Test
    fun `Lovlig opphold - valider at sterkeste medlemskap blir returnert`() {
        val medlemskapNorden = listOf(Medlemskap.TREDJELANDSBORGER, Medlemskap.NORDEN, Medlemskap.UKJENT)
        val medlemskapUkjent = listOf(Medlemskap.UKJENT)
        val medlemskapIngen = emptyList<Medlemskap>()

        assertEquals(Medlemskap.NORDEN, finnSterkesteMedlemskap(medlemskapNorden))
        assertEquals(Medlemskap.UKJENT, finnSterkesteMedlemskap(medlemskapUkjent))
        assertEquals(null, finnSterkesteMedlemskap(medlemskapIngen))
    }
}
