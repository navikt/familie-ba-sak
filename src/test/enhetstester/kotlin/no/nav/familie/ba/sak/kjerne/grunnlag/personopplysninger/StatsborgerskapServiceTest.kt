package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.POL_EØS_FOM
import no.nav.familie.ba.sak.datagenerator.lagKodeverkLand
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnNåværendeMedlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.finnSterkesteMedlemskap
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class StatsborgerskapServiceTest {
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val kodeverkService = KodeverkService(integrasjonKlient)
    private val featureToggleService = mockk<FeatureToggleService>()

    private val statsborgerskapService: StatsborgerskapService = StatsborgerskapService(kodeverkService, featureToggleService)

    @BeforeEach
    fun setUp() {
        every { integrasjonKlient.hentAlleEØSLand() } returns lagKodeverkLand()
        every { featureToggleService.isEnabled(FeatureToggle.HARDKODET_EEAFREG_STATSBORGERSKAP) } returns true
    }

    @Nested
    inner class FinnNåværendeMedlemskap {
        @Test
        fun `Lovlig opphold - valider at alle gjeldende medlemskap blir returnert`() {
            // Arrange
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

            // Act
            val medlemskap = finnNåværendeMedlemskap(person.statsborgerskap)

            // Assert
            assertEquals(2, medlemskap.size)
            assertEquals(Medlemskap.NORDEN, medlemskap[0])
            assertEquals(Medlemskap.EØS, medlemskap[1])
        }
    }

    @Nested
    inner class HentStatsborgerskapMedMedlemskap {
        @Nested
        inner class Storbrittannia {
            @Test
            fun `Skal sette riktige tom-datoer dersom storbritannia blir del av EU igjen`() {
                every { integrasjonKlient.hentAlleEØSLand() } returns
                    KodeverkDto(
                        betydninger =
                            mapOf(
                                "GBR" to
                                    listOf(
                                        BetydningDto(
                                            gyldigFra = LocalDate.of(1900, Month.JANUARY, 1),
                                            gyldigTil = LocalDate.of(2025, Month.JANUARY, 31),
                                            beskrivelser = mapOf(),
                                        ),
                                        BetydningDto(
                                            gyldigFra = LocalDate.of(2030, Month.JANUARY, 1),
                                            gyldigTil = LocalDate.of(9999, Month.DECEMBER, 31),
                                            beskrivelser =
                                                mapOf(),
                                        ),
                                    ),
                            ),
                    )

                // Arrange
                val person = lagPerson()
                // Sørger for at barnets fødselsdato er før brexit slik at vi her først får en EØS-periode, etterfulgt av en tredjelandsborger-periode før vi igjen får en EØS-periode.
                val barn = lagPerson(fødselsdato = LocalDate.of(2010, 1, 1))
                val statsborgerskap =
                    Statsborgerskap(
                        land = "GBR",
                        gyldigFraOgMed = LocalDate.of(2000, 1, 1),
                        gyldigTilOgMed = null,
                        bekreftelsesdato = null,
                    )

                // Act
                val grStatsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskap(statsborgerskap = statsborgerskap, person = person, barn.fødselsdato)

                // Assert
                val eøsStatsborgerskapPerioder = grStatsborgerskap.filter { it.medlemskap == Medlemskap.EØS }
                assertEquals(2, eøsStatsborgerskapPerioder.size)

                val tredjelandsStatsborgerskapPerioder = grStatsborgerskap.filter { it.medlemskap == Medlemskap.TREDJELANDSBORGER }
                assertEquals(1, tredjelandsStatsborgerskapPerioder.size)
                assertEquals(KodeverkService.BREXIT_OVERGANGSORDNING_TOM_DATO.plusDays(1), tredjelandsStatsborgerskapPerioder.single().gyldigPeriode?.fom)
            }

            @Test
            fun `Skal evaluere britiske statsborgere med ukjent periode som først EØS og så tredjelandsborgere`() {
                // Arrange
                val statsborgerStorbritanniaUtenPeriode =
                    Statsborgerskap(
                        "GBR",
                        gyldigFraOgMed = null,
                        gyldigTilOgMed = null,
                        bekreftelsesdato = null,
                    )

                val person = lagPerson()
                // Fødselsdato til barn før brexit slik at vi får EØS-periode før brexit og deretter en tredjelandsperiode.
                val barn = lagPerson(fødselsdato = LocalDate.of(2010, 1, 1))

                // Act
                val grStatsborgerskapUtenPeriode =
                    statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                        statsborgerskap = statsborgerStorbritanniaUtenPeriode,
                        person = person,
                        eldsteBarnFødselsdato = barn.fødselsdato,
                    )

                // Assert
                assertEquals(2, grStatsborgerskapUtenPeriode.size)
                assertEquals(Medlemskap.EØS, grStatsborgerskapUtenPeriode.first().medlemskap)
                assertEquals(KodeverkService.BREXIT_OVERGANGSORDNING_TOM_DATO, grStatsborgerskapUtenPeriode.first().gyldigPeriode?.tom)
                assertEquals(Medlemskap.TREDJELANDSBORGER, grStatsborgerskapUtenPeriode.last().medlemskap)
                assertTrue(grStatsborgerskapUtenPeriode.last().gjeldendeNå())
            }

            @Test
            fun `Skal evaluere britiske statsborgere under Brexit som først EØS og så tredjelandsborgere`() {
                // Arrange
                val datoFørBrexit = LocalDate.of(1989, 3, 1)
                val datoEtterBrexit = LocalDate.of(2022, 5, 1)

                val statsborgerStorbritanniaMedPeriodeUnderBrexit =
                    Statsborgerskap(
                        "GBR",
                        gyldigFraOgMed = datoFørBrexit,
                        gyldigTilOgMed = datoEtterBrexit,
                        bekreftelsesdato = null,
                    )

                val person = lagPerson(fødselsdato = datoFørBrexit.minusYears(2))
                // Fødselsdato til barn før brexit slik at vi får EØS-periode før brexit og deretter en tredjelandsperiode.
                val barn = lagPerson(fødselsdato = LocalDate.of(2010, 1, 1))

                // Act
                val grStatsborgerskapUnderBrexit =
                    statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                        statsborgerskap = statsborgerStorbritanniaMedPeriodeUnderBrexit,
                        person = person,
                        eldsteBarnFødselsdato = barn.fødselsdato,
                    )

                // Assert
                assertEquals(2, grStatsborgerskapUnderBrexit.size)
                assertEquals(barn.fødselsdato, grStatsborgerskapUnderBrexit.first().gyldigPeriode?.fom)
                assertEquals(KodeverkService.BREXIT_OVERGANGSORDNING_TOM_DATO, grStatsborgerskapUnderBrexit.first().gyldigPeriode?.tom)
                assertEquals(Medlemskap.EØS, grStatsborgerskapUnderBrexit.sortedBy { it.gyldigPeriode?.fom }.first().medlemskap)
                assertEquals(
                    Medlemskap.TREDJELANDSBORGER,
                    grStatsborgerskapUnderBrexit.sortedBy { it.gyldigPeriode?.fom }.last().medlemskap,
                )
            }
        }

        @Test
        fun `Skal ikke være mulig å få med perioder før eldste barns fødselsdato når statsborgerskap ikke har fom eller tom`() {
            val statsborgerskap =
                Statsborgerskap(
                    land = "DEU",
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                )
            val person = lagPerson()
            val barnFødselsdato = LocalDate.of(2020, 1, 1)
            val barn = lagPerson(fødselsdato = barnFødselsdato)

            // Act
            val grStatsborgerskap =
                statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                    statsborgerskap = statsborgerskap,
                    person = person,
                    eldsteBarnFødselsdato = barn.fødselsdato,
                )

            // Assert
            assertEquals(1, grStatsborgerskap.size)
            assertEquals(barnFødselsdato, grStatsborgerskap.first().gyldigPeriode?.fom)
        }

        @Test
        fun `Skal ikke være mulig å få med perioder før eldste barns fødselsdato når statsborgerskap fom er før eldste barns fødselsdato`() {
            val person = lagPerson()
            val barnFødselsdato = LocalDate.of(2020, 1, 1)
            val barn = lagPerson(fødselsdato = barnFødselsdato)

            val statsborgerskap =
                Statsborgerskap(
                    land = "DEU",
                    gyldigFraOgMed = barnFødselsdato.minusYears(1),
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                )

            // Act
            val grStatsborgerskap =
                statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                    statsborgerskap = statsborgerskap,
                    person = person,
                    eldsteBarnFødselsdato = barn.fødselsdato,
                )

            // Assert
            assertEquals(1, grStatsborgerskap.size)
            assertEquals(barnFødselsdato, grStatsborgerskap.first().gyldigPeriode?.fom)
        }

        @Test
        fun `Skal behandle kodeverk uendelig dato som uendelig og ikke lage tredjelandsperiode på slutten av statsborgerskap`() {
            val person = lagPerson()
            val barn = lagPerson(fødselsdato = LocalDate.of(2010, 1, 1))
            val statsborgerskap =
                Statsborgerskap(
                    land = "DEU",
                    gyldigFraOgMed = LocalDate.of(2000, 1, 1),
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                )

            // Act
            val grStatsborgerskap = statsborgerskapService.hentStatsborgerskapMedMedlemskap(statsborgerskap = statsborgerskap, person = person, eldsteBarnFødselsdato = barn.fødselsdato)

            // Assert
            assertEquals(1, grStatsborgerskap.size)
            assertEquals(grStatsborgerskap.first().medlemskap, Medlemskap.EØS)
        }

        @Test
        fun `Skal generere GrStatsborgerskap med flere perioder fordi Polen ble medlem av EØS`() {
            // Arrange
            val statsborgerskapPolenFom = LocalDate.of(1990, Month.JANUARY, 1)
            val stasborgerskapPolenTom = LocalDate.of(2009, Month.DECEMBER, 31)

            val statsborgerskapMedGyldigFom =
                Statsborgerskap(
                    "POL",
                    bekreftelsesdato = null,
                    gyldigFraOgMed = statsborgerskapPolenFom,
                    gyldigTilOgMed = stasborgerskapPolenTom,
                )

            val person = lagPerson()
            // Sørger for at barnets fødselsdato er før Polen ble medlem av EØS slik at vi får en tredjelands-periode før Polen ble medlem av EØS og deretter en EØS-periode etter at Polen ble medlem av EØS.
            val barn = lagPerson(fødselsdato = LocalDate.of(2003, 1, 1))

            // Act
            val grStatsborgerskap =
                statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                    statsborgerskap = statsborgerskapMedGyldigFom,
                    person = person,
                    eldsteBarnFødselsdato = barn.fødselsdato,
                )

            // Assert
            assertEquals(2, grStatsborgerskap.size)
            assertEquals(
                barn.fødselsdato,
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
        fun `Skal evaluere polske statsborgere med ukjent periode som EØS-borgere fra dato Polen ble medlem av EØS`() {
            // Arrange
            val statsborgerPolenUtenPeriode =
                Statsborgerskap(
                    "POL",
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                )

            val person = lagPerson()
            // Sørger for at barnets fødselsdato er før Polen ble medlem av EØS slik at vi får en tredjelands-periode før Polen ble medlem av EØS og deretter en EØS-periode etter at Polen ble medlem av EØS.
            val barn = lagPerson(fødselsdato = LocalDate.of(2003, 1, 1))

            // Act
            val grStatsborgerskapUtenPeriode =
                statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                    statsborgerskap = statsborgerPolenUtenPeriode,
                    person = person,
                    eldsteBarnFødselsdato = barn.fødselsdato,
                )

            // Assert
            assertEquals(2, grStatsborgerskapUtenPeriode.size)
            assertThat(grStatsborgerskapUtenPeriode.first().medlemskap).isEqualTo(Medlemskap.TREDJELANDSBORGER)
            assertThat(grStatsborgerskapUtenPeriode.last().medlemskap).isEqualTo(Medlemskap.EØS)
            assertTrue(grStatsborgerskapUtenPeriode.last().gjeldendeNå())
        }

        @Test
        fun `Skal evaluere multiple statsborgerskap uten fom og tom`() {
            // Arrange
            val statsborgerStorbritanniaUtenPeriode =
                Statsborgerskap(
                    "GBR",
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                )

            val person = lagPerson()
            val barn = lagPerson(fødselsdato = LocalDate.of(2010, 1, 1))

            // Act
            val grStatsborgerskapUtenPeriode =
                statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                    statsborgerskap = statsborgerStorbritanniaUtenPeriode,
                    person = person,
                    eldsteBarnFødselsdato = barn.fødselsdato,
                )

            // Assert
            assertEquals(2, grStatsborgerskapUtenPeriode.size)
            assertEquals(Medlemskap.EØS, grStatsborgerskapUtenPeriode.first().medlemskap)
            assertEquals(Medlemskap.TREDJELANDSBORGER, grStatsborgerskapUtenPeriode.last().medlemskap)
            assertTrue(grStatsborgerskapUtenPeriode.last().gjeldendeNå())
        }

        @Test
        fun `Dersom person er statsløs skal vi lage én periode fra eldste barns fødselsdato med medlemskap statsløs`() {
            // Arrange
            val statsborgerskapStatsløsUtenPeriode =
                Statsborgerskap(
                    land = StatsborgerskapService.LANDKODE_STATSLØS,
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                )

            val person = lagPerson()
            val barn = lagPerson(fødselsdato = LocalDate.of(2010, 1, 1))

            // Act
            val grStatsborgerskapMedMedlemskap =
                statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                    statsborgerskap = statsborgerskapStatsløsUtenPeriode,
                    person = person,
                    eldsteBarnFødselsdato = barn.fødselsdato,
                )

            // Assert
            assertEquals(1, grStatsborgerskapMedMedlemskap.size)
            val grStatsborgerskap = grStatsborgerskapMedMedlemskap.single()

            assertEquals(Medlemskap.STATSLØS, grStatsborgerskap.medlemskap)
            assertThat(grStatsborgerskap.gyldigPeriode?.fom).isEqualTo(barn.fødselsdato)
            assertTrue(grStatsborgerskap.gjeldendeNå())
        }

        @Test
        fun `Dersom person har ukjent statsborgerskap skal vi lage én periode fra eldste barns fødselsdato med medlemskap ukjent`() {
            // Arrange
            val statsborgerskapStatsløsUtenPeriode =
                Statsborgerskap(
                    land = StatsborgerskapService.LANDKODE_UKJENT,
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    bekreftelsesdato = null,
                )

            val person = lagPerson()
            val barn = lagPerson(fødselsdato = LocalDate.of(2010, 1, 1))

            // Act
            val grStatsborgerskapMedMedlemskap =
                statsborgerskapService.hentStatsborgerskapMedMedlemskap(
                    statsborgerskap = statsborgerskapStatsløsUtenPeriode,
                    person = person,
                    eldsteBarnFødselsdato = barn.fødselsdato,
                )

            // Assert
            assertEquals(1, grStatsborgerskapMedMedlemskap.size)
            val grStatsborgerskap = grStatsborgerskapMedMedlemskap.single()

            assertEquals(Medlemskap.UKJENT, grStatsborgerskap.medlemskap)
            assertThat(grStatsborgerskap.gyldigPeriode?.fom).isEqualTo(barn.fødselsdato)
            assertTrue(grStatsborgerskap.gjeldendeNå())
        }
    }

    @Nested
    inner class HentSterkesteMedlemskapTest {
        @Test
        fun `hentSterkesteMedlemskap - skal finne sterkeste medlemskap i statsborgerperioden`() {
            val statsborgerStorbritannia =
                Statsborgerskap(
                    "GBR",
                    gyldigFraOgMed = LocalDate.of(1990, 4, 1),
                    gyldigTilOgMed = LocalDate.of(2005, 12, 31),
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

            assertEquals(Medlemskap.EØS, statsborgerskapService.hentSterkesteMedlemskapVedTidspunkt(statsborgerStorbritannia))
            assertEquals(Medlemskap.EØS, statsborgerskapService.hentSterkesteMedlemskapVedTidspunkt(statsborgerPolen))
            assertEquals(Medlemskap.TREDJELANDSBORGER, statsborgerskapService.hentSterkesteMedlemskapVedTidspunkt(statsborgerSerbia))
            assertEquals(Medlemskap.NORDEN, statsborgerskapService.hentSterkesteMedlemskapVedTidspunkt(statsborgerNorge))
        }

        @Test
        fun `hentSterkesteMedlemskap - om statsborgerperiode er ukjent vurderer vi som dagens medlemskap`() {
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
                statsborgerskapService.hentSterkesteMedlemskapVedTidspunkt(statsborgerStorbritanniaMedNullDatoer),
            )
            assertEquals(Medlemskap.EØS, statsborgerskapService.hentSterkesteMedlemskapVedTidspunkt(statsborgerPolenMedNullDatoer))
        }
    }

    @Nested
    inner class FinnSterkesteMedlemskapTest {
        @Test
        fun `Lovlig opphold - valider at sterkeste medlemskap blir returnert`() {
            // Arrange
            val medlemskapNorden = listOf(Medlemskap.TREDJELANDSBORGER, Medlemskap.NORDEN, Medlemskap.UKJENT)
            val medlemskapUkjent = listOf(Medlemskap.UKJENT)
            val medlemskapIngen = emptyList<Medlemskap>()

            // Act & Assert
            assertEquals(Medlemskap.NORDEN, medlemskapNorden.finnSterkesteMedlemskap())
            assertEquals(Medlemskap.UKJENT, medlemskapUkjent.finnSterkesteMedlemskap())
            assertEquals(null, medlemskapIngen.finnSterkesteMedlemskap())
        }
    }
}
