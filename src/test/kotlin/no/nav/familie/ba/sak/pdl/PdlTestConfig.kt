package no.nav.familie.ba.sak.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.pdl.internal.Familierelasjon
import no.nav.familie.ba.sak.pdl.internal.Person
import no.nav.familie.ba.sak.pdl.internal.Personident
import no.nav.familie.kontrakter.felles.personinfo.SIVILSTAND
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class PdlTestConfig {

    @Bean
    @Profile("mock-pdl")
    @Primary
    fun pdlRestClientMock(): PdlRestClient {
        val klient = mockk<PdlRestClient>(relaxed = true)

        every {
            klient.hentPerson(any(), any(), any())
        } returns Person(fødselsdato = "1980-05-12",
                              navn = "Kari Normann",
                              kjønn = "KVINNE",
                              familierelasjoner = setOf(Familierelasjon(personIdent = Personident(id = "12345678910"),
                                                                        relasjonsrolle = "BARN")),
                              adressebeskyttelseGradering = null,
                              sivilstand = SIVILSTAND.UGIFT)
        return klient
    }
}