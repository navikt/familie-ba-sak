package no.nav.familie.ba.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.lang.Integer.min
import java.time.LocalDate

@TestConfiguration
class ClientMocks {
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
        fun clearFeatureToggleMocks(mockFeatureToggleService: FeatureToggleService) {
            val mockFeatureToggleServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true

            val featureSlotString = slot<String>()
            val featureSlot = slot<FeatureToggle>()
            every {
                mockFeatureToggleService.isEnabled(toggleId = capture(featureSlotString))
            } answers {
                System.getProperty(featureSlotString.captured)?.toBoolean() ?: mockFeatureToggleServiceAnswer
            }

            every {
                mockFeatureToggleService.isEnabled(toggle = capture(featureSlot))
            } answers {
                System.getProperty(featureSlot.captured.navn)?.toBoolean() ?: mockFeatureToggleServiceAnswer
            }

            every {
                mockFeatureToggleService.isEnabled(toggle = capture(featureSlot), behandlingId = any<Long>())
            } answers {
                System.getProperty(featureSlot.captured.navn)?.toBoolean() ?: mockFeatureToggleServiceAnswer
            }

            every {
                mockFeatureToggleService.isEnabled(toggle = capture(featureSlot), defaultValue = any<Boolean>())
            } answers {
                System.getProperty(featureSlot.captured.navn)?.toBoolean() ?: mockFeatureToggleServiceAnswer
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
