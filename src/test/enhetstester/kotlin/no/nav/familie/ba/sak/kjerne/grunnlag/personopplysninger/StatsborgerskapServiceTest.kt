package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.GBR_EØS_TOM
import no.nav.familie.ba.sak.datagenerator.POL_EØS_FOM
import no.nav.familie.ba.sak.datagenerator.lagKodeverkLand
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnNåværendeMedlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnSterkesteMedlemskap
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class StatsborgerskapServiceTest {
    private val integrasjonClient = mockk<IntegrasjonClient>()
    private val kodeverkService = KodeverkService(integrasjonClient)

    private lateinit var statsborgerskapService: StatsborgerskapService

    @BeforeEach
    fun setUp() {
        statsborgerskapService = StatsborgerskapService(integrasjonClient, kodeverkService)
        every { integrasjonClient.hentAlleEØSLand() } returns lagKodeverkLand()
    }

    @Test
    fun `Skal generere GrStatsborgerskap med flere perioder fordi Polen ble medlem av EØS`() {
        val statsborgerskapPolenFom = LocalDate.of(1990, Month.JANUARY, 1)
        val stasborgerskapPolenTom = LocalDate.of(2009, Month.DECEMBER, 31)

        val statsborgerskapMedGyldigFom =
            Statsborgerskap(
                "POL",
                bekreftelsesdato = null,
                gyldigFraOgMed = statsborgerskapPolenFom,
                gyldigTilOgMed = stasborgerskapPolenTom,
            )

        val grStatsborgerskap =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap = statsborgerskapMedGyldigFom,
                person = lagPerson(),
            )

        assertEquals(2, grStatsborgerskap.size)
        assertEquals(
            statsborgerskapPolenFom,
            grStatsborgerskap
                .sortedBy { it.gyldigPeriode?.fom }
                .first()
                .gyldigPeriode
                ?.fom,
        )
        val dagenFørPolenBleMedlemAvEØS = POL_EØS_FOM.minusDays(1)
        assertEquals(
            dagenFørPolenBleMedlemAvEØS,
            grStatsborgerskap
                .sortedBy { it.gyldigPeriode?.fom }
                .first()
                .gyldigPeriode
                ?.tom,
        )
        assertEquals(
            Medlemskap.TREDJELANDSBORGER,
            grStatsborgerskap.sortedBy { it.gyldigPeriode?.fom }.first().medlemskap,
        )
        assertEquals(
            POL_EØS_FOM,
            grStatsborgerskap
                .sortedBy { it.gyldigPeriode?.fom }
                .last()
                .gyldigPeriode
                ?.fom,
        )
        assertEquals(Medlemskap.EØS, grStatsborgerskap.sortedBy { it.gyldigPeriode?.fom }.last().medlemskap)
    }

    @Test
    fun `Skal evaluere polske statsborgere med ukjent periode som EØS-borgere`() {
        val statsborgerPolenUtenPeriode =
            Statsborgerskap(
                "POL",
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )

        val grStatsborgerskapUtenPeriode =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap = statsborgerPolenUtenPeriode,
                person = lagPerson(),
            )
        assertEquals(1, grStatsborgerskapUtenPeriode.size)
        assertEquals(Medlemskap.EØS, grStatsborgerskapUtenPeriode.single().medlemskap)
        assertTrue(grStatsborgerskapUtenPeriode.single().gjeldendeNå())
    }

    @Test
    fun `Lovlig opphold - valider at alle gjeldende medlemskap blir returnert`() {
        val person =
            lagPerson()
                .also {
                    it.statsborgerskap =
                        mutableListOf(
                            GrStatsborgerskap(
                                gyldigPeriode = DatoIntervallEntitet(tom = null, fom = null),
                                landkode = "DNK",
                                medlemskap = Medlemskap.NORDEN,
                                person = it,
                            ),
                            GrStatsborgerskap(
                                gyldigPeriode =
                                    DatoIntervallEntitet(
                                        tom = null,
                                        fom = LocalDate.now().minusYears(1),
                                    ),
                                landkode = "DEU",
                                medlemskap = Medlemskap.EØS,
                                person = it,
                            ),
                            GrStatsborgerskap(
                                gyldigPeriode =
                                    DatoIntervallEntitet(
                                        tom = LocalDate.now().minusYears(2),
                                        fom = LocalDate.now().minusYears(2),
                                    ),
                                landkode = "POL",
                                medlemskap = Medlemskap.EØS,
                                person = it,
                            ),
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

    @Test
    fun `Skal evaluere britiske statsborgere med ukjent periode som tredjelandsborgere`() {
        val statsborgerStorbritanniaUtenPeriode =
            Statsborgerskap(
                "GBR",
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )

        val grStatsborgerskapUtenPeriode =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap = statsborgerStorbritanniaUtenPeriode,
                person = lagPerson(),
            )
        assertEquals(1, grStatsborgerskapUtenPeriode.size)
        assertEquals(Medlemskap.TREDJELANDSBORGER, grStatsborgerskapUtenPeriode.single().medlemskap)
        assertTrue(grStatsborgerskapUtenPeriode.single().gjeldendeNå())
    }

    @Test
    fun `Skal evaluere britiske statsborgere etter brexit som tredjelandsborgere`() {
        val statsborgerStorbritanniaMedPeriodeEtterBrexit =
            Statsborgerskap(
                "GBR",
                gyldigFraOgMed = LocalDate.of(2022, 3, 1),
                gyldigTilOgMed = LocalDate.now(),
                bekreftelsesdato = null,
            )
        val grStatsborgerskapEtterBrexit =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap = statsborgerStorbritanniaMedPeriodeEtterBrexit,
                person = lagPerson(),
            )
        assertEquals(1, grStatsborgerskapEtterBrexit.size)
        assertEquals(Medlemskap.TREDJELANDSBORGER, grStatsborgerskapEtterBrexit.single().medlemskap)
        assertTrue(grStatsborgerskapEtterBrexit.single().gjeldendeNå())
    }

    @Test
    fun `Skal evaluere britiske statsborgere under Brexit som først EØS, nå tredjelandsborgere`() {
        val datoFørBrexit = LocalDate.of(1989, 3, 1)
        val datoEtterBrexit = LocalDate.of(2020, 5, 1)

        val statsborgerStorbritanniaMedPeriodeUnderBrexit =
            Statsborgerskap(
                "GBR",
                gyldigFraOgMed = datoFørBrexit,
                gyldigTilOgMed = datoEtterBrexit,
                bekreftelsesdato = null,
            )
        val grStatsborgerskapUnderBrexit =
            statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                statsborgerskap = statsborgerStorbritanniaMedPeriodeUnderBrexit,
                person = lagPerson(),
            )
        assertEquals(2, grStatsborgerskapUnderBrexit.size)
        assertEquals(datoFørBrexit, grStatsborgerskapUnderBrexit.first().gyldigPeriode?.fom)
        assertEquals(GBR_EØS_TOM, grStatsborgerskapUnderBrexit.first().gyldigPeriode?.tom)
        assertEquals(Medlemskap.EØS, grStatsborgerskapUnderBrexit.sortedBy { it.gyldigPeriode?.fom }.first().medlemskap)
        assertEquals(
            Medlemskap.TREDJELANDSBORGER,
            grStatsborgerskapUnderBrexit.sortedBy { it.gyldigPeriode?.fom }.last().medlemskap,
        )
    }

    @Test
    fun `hentSterkesteMedlemskap - skal finne sterkeste medlemskap i statsborgerperioden`() {
        val statsborgerStorbritannia =
            Statsborgerskap(
                "GBR",
                gyldigFraOgMed = LocalDate.of(1990, 4, 1),
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )
        val statsborgerPolen =
            Statsborgerskap(
                "POL",
                gyldigFraOgMed = LocalDate.of(1990, 4, 1),
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )
        val statsborgerSerbia =
            Statsborgerskap(
                "SRB",
                gyldigFraOgMed = LocalDate.of(1990, 4, 1),
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )
        val statsborgerNorge =
            Statsborgerskap(
                "NOR",
                gyldigFraOgMed = LocalDate.of(1990, 4, 1),
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )

        assertEquals(Medlemskap.EØS, statsborgerskapService.hentSterkesteMedlemskap(statsborgerStorbritannia))
        assertEquals(Medlemskap.EØS, statsborgerskapService.hentSterkesteMedlemskap(statsborgerPolen))
        assertEquals(Medlemskap.TREDJELANDSBORGER, statsborgerskapService.hentSterkesteMedlemskap(statsborgerSerbia))
        assertEquals(Medlemskap.NORDEN, statsborgerskapService.hentSterkesteMedlemskap(statsborgerNorge))
    }

    @Test
    fun `hentSterkesteMedlemskap - om statsborgerperiode er ukjent vurderer vi basert på dagens medlemskap`() {
        val statsborgerStorbritanniaMedNullDatoer =
            Statsborgerskap(
                "GBR",
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )
        val statsborgerPolenMedNullDatoer =
            Statsborgerskap(
                "POL",
                gyldigFraOgMed = null,
                gyldigTilOgMed = null,
                bekreftelsesdato = null,
            )
        assertEquals(
            Medlemskap.TREDJELANDSBORGER,
            statsborgerskapService.hentSterkesteMedlemskap(statsborgerStorbritanniaMedNullDatoer),
        )
        assertEquals(Medlemskap.EØS, statsborgerskapService.hentSterkesteMedlemskap(statsborgerPolenMedNullDatoer))
    }
}
