package no.nav.familie.ba.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.unleash.UnleashService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import randomFnr
import java.lang.Integer.min
import java.time.LocalDate

@TestConfiguration
class ClientMocks {
    @Bean
    @Primary
    @Profile("mock-pdl-test-søk")
    fun mockPDL(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>()

        val farId = "12345678910"
        val morId = "21345678910"
        val barnId = "31245678910"

        val farAktør = tilAktør(farId)
        val morAktør = tilAktør(morId)
        val barnAktør = tilAktør(barnId)

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(any())
        } returns personInfo.getValue(INTEGRASJONER_FNR)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(farAktør)
        } returns PersonInfo(fødselsdato = LocalDate.of(1969, 5, 1), kjønn = Kjønn.MANN, navn = "Far Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(morAktør)
        } returns PersonInfo(fødselsdato = LocalDate.of(1979, 5, 1), kjønn = Kjønn.KVINNE, navn = "Mor Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnAktør)
        } returns
            PersonInfo(
                fødselsdato = LocalDate.of(2009, 5, 1),
                kjønn = Kjønn.MANN,
                navn = "Barn Mocksen",
                forelderBarnRelasjon =
                    setOf(
                        ForelderBarnRelasjon(
                            farAktør,
                            FORELDERBARNRELASJONROLLE.FAR,
                            "Far Mocksen",
                            LocalDate.of(1969, 5, 1),
                        ),
                        ForelderBarnRelasjon(
                            morAktør,
                            FORELDERBARNRELASJONROLLE.MOR,
                            "Mor Mocksen",
                            LocalDate.of(1979, 5, 1),
                        ),
                    ),
            )

        every {
            mockPersonopplysningerService.hentGjeldendeStatsborgerskap(any())
        } answers {
            Statsborgerskap(
                "NOR",
                LocalDate.of(1990, 1, 25),
                LocalDate.of(1990, 1, 25),
                null,
            )
        }

        every {
            mockPersonopplysningerService.hentGjeldendeOpphold(any())
        } answers {
            Opphold(
                type = OPPHOLDSTILLATELSE.PERMANENT,
                oppholdFra = LocalDate.of(1990, 1, 25),
                oppholdTil = LocalDate.of(2499, 1, 1),
            )
        }

        every {
            mockPersonopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(any())
        } returns "NO"

        val ukjentAktør = tilAktør(ukjentId)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(ukjentAktør)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND, "ikke funnet")

        val feilId = "41235678910"
        val feilIdAktør = tilAktør(feilId)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(feilIdAktør)
        } throws IntegrasjonException("feil id")

        return mockPersonopplysningerService
    }

    @Bean
    @Primary
    fun mockEnvService(): EnvService {
        val mockEnvService = mockk<EnvService>(relaxed = true)

        every {
            mockEnvService.erProd()
        } answers {
            true
        }

        every {
            mockEnvService.erPreprod()
        } answers {
            true
        }

        every {
            mockEnvService.erDev()
        } answers {
            true
        }

        return mockEnvService
    }

    companion object {
        fun clearUnleashServiceMocks(mockUnleashService: UnleashService) {
            val mockUnleashServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true

            val featureSlot = slot<String>()
            every {
                mockUnleashService.isEnabled(toggleId = capture(featureSlot))
            } answers {
                System.getProperty(featureSlot.captured)?.toBoolean() ?: mockUnleashServiceAnswer
            }
            every {
                mockUnleashService.isEnabled(toggleId = capture(featureSlot), defaultValue = any())
            } answers {
                System.getProperty(featureSlot.captured)?.toBoolean() ?: mockUnleashServiceAnswer
            }

            every {
                mockUnleashService.isEnabled(toggleId = capture(featureSlot), properties = any())
            } answers {
                System.getProperty(featureSlot.captured)?.toBoolean() ?: mockUnleashServiceAnswer
            }
        }

        fun clearPdlIdentRestClient(
            mockPdlIdentRestClient: PdlIdentRestClient,
        ) {
            clearMocks(mockPdlIdentRestClient)

            val identSlot = slot<String>()
            every {
                mockPdlIdentRestClient.hentIdenter(capture(identSlot), true)
            } answers {
                listOf(
                    IdentInformasjon(identSlot.captured, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(randomFnr(), true, "FOLKEREGISTERIDENT"),
                )
            }

            val identSlot2 = slot<String>()
            every {
                mockPdlIdentRestClient.hentIdenter(capture(identSlot2), false)
            } answers {
                listOf(
                    IdentInformasjon(
                        identSlot2.captured.substring(0, min(11, identSlot2.captured.length)),
                        false,
                        "FOLKEREGISTERIDENT",
                    ),
                    IdentInformasjon(
                        identSlot2.captured.substring(0, min(11, identSlot2.captured.length)) + "00",
                        false,
                        "AKTORID",
                    ),
                )
            }
        }

        fun clearPdlMocks(
            mockPersonopplysningerService: PersonopplysningerService,
        ) {
            clearMocks(mockPersonopplysningerService)
        }

        const val BARN_DET_IKKE_GIS_TILGANG_TIL_FNR = "12345678912"
        const val INTEGRASJONER_FNR = "10000111111"
        val bostedsadresse =
            Bostedsadresse(
                matrikkeladresse =
                    Matrikkeladresse(
                        matrikkelId = 123L,
                        bruksenhetsnummer = "H301",
                        tilleggsnavn = "navn",
                        postnummer = "0202",
                        kommunenummer = "2231",
                    ),
            )
        private val sivilstandHistorisk =
            listOf(
                Sivilstand(type = SIVILSTANDTYPE.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8)),
                Sivilstand(type = SIVILSTANDTYPE.SKILT, gyldigFraOgMed = LocalDate.now().minusMonths(4)),
            )

        val personInfo =
            mapOf(
                INTEGRASJONER_FNR to
                    PersonInfo(
                        fødselsdato = LocalDate.of(1965, 2, 19),
                        bostedsadresser = mutableListOf(bostedsadresse),
                        kjønn = Kjønn.KVINNE,
                        navn = "Mor Integrasjon person",
                        sivilstander = sivilstandHistorisk,
                    ),
            )
        val ukjentId = randomFnr()
    }
}

fun tilAktør(
    fnr: String,
    toSisteSiffrer: String = "00",
) = Aktør(fnr + toSisteSiffrer).also {
    it.personidenter.add(Personident(fnr, aktør = it))
}

val TEST_PDF = ClientMocks::class.java.getResource("/dokument/mockvedtak.pdf").readBytes()
