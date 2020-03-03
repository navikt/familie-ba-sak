package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.dokument.DokGenKlient
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonTjeneste
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@TestConfiguration
class FamilieIntegrasjonerTestConfig {

    @Bean
    @Profile("mock-familie-integrasjoner")
    @Primary
    fun mockFamilieIntegrasjoner(): IntegrasjonTjeneste {
        //eliminate complain from Mockito of null parameter
        fun <T> any(): T = Mockito.any<T>()

        val familieIntegrasjonService = Mockito.mock(IntegrasjonTjeneste::class.java)
        Mockito.`when`(familieIntegrasjonService.hentAktørId(any())).thenReturn(AktørId("1"))
        Mockito.`when`(familieIntegrasjonService.hentPersoninfoFor(any())).thenReturn(Personinfo(
                LocalDate.now()
        ))
        return familieIntegrasjonService
    }

}