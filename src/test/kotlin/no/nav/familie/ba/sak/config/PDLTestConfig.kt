package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.domene.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.integrasjoner.domene.Familierelasjoner
import no.nav.familie.ba.sak.integrasjoner.domene.Personident
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@TestConfiguration
class PDLTestConfig {

    @Profile("mock-pdl")
    @Bean
    @Primary
    fun mockPDL() : IntegrasjonClient{
        val mockIntegrasjonClient= mockk<IntegrasjonClient>()


        val farId= "12345678910"
        val morId= "21345678910"
        val barnId= "31245678910"

        every {
            mockIntegrasjonClient.hentPersoninfoFor(farId)
        } returns Personinfo(fødselsdato = LocalDate.of(1969, 5, 1), kjønn = Kjønn.MANN, navn = "Far Mocksen")

        every {
            mockIntegrasjonClient.hentPersoninfoFor(morId)
        } returns Personinfo(fødselsdato = LocalDate.of(1979, 5, 1), kjønn = Kjønn.KVINNE, navn = "Mor Mocksen")

        every {
            mockIntegrasjonClient.hentPersoninfoFor(barnId)
        } returns Personinfo(fødselsdato = LocalDate.of(2009, 5, 1), kjønn = Kjønn.MANN, navn = "Barn Mocksen",
                familierelasjoner = setOf(
                        Familierelasjoner(Personident(farId), FAMILIERELASJONSROLLE.FAR, "Far Mocksen", LocalDate.of(1969, 5, 1)),
                        Familierelasjoner(Personident(morId), FAMILIERELASJONSROLLE.MOR, "Mor Mocksen", LocalDate.of(1979, 5, 1))
                ))

        return mockIntegrasjonClient
    }
}