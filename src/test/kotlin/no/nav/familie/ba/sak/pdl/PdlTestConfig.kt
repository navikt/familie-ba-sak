package no.nav.familie.ba.sak.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.pdl.internal.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.pdl.internal.Familierelasjon
import no.nav.familie.ba.sak.pdl.internal.PersonInfo
import no.nav.familie.ba.sak.pdl.internal.Personident
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
class PdlTestConfig {

    @Bean
    @Profile("mock-pdl")
    @Primary
    fun pdlRestClientMock(): PdlRestClient {
        val klient = mockk<PdlRestClient>(relaxed = true)

        every {
            klient.hentPerson(any(), any(), any())
        } returns PersonInfo(fødselsdato = LocalDate.of(1980,5, 12),
                             navn = "Kari Normann",
                             kjønn = Kjønn.KVINNE,
                             familierelasjoner = setOf(Familierelasjon(personIdent = Personident(id = "12345678910"),
                                                                        relasjonsrolle = FAMILIERELASJONSROLLE.BARN)),
                             adressebeskyttelseGradering = null,
                             sivilstand = SIVILSTAND.UGIFT)
        return klient
    }
}