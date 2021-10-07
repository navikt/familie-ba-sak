package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDate

@TestConfiguration
class EfSakRestClientMock {

    @Bean
    @Primary
    fun mockEfSakRestClient(): EfSakRestClient {
        val efSakRestClient = mockk<EfSakRestClient>()

        val hentPerioderMedFullOvergangsstønadSlot = slot<String>()
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(capture(hentPerioderMedFullOvergangsstønadSlot)) } answers {
            PerioderOvergangsstønadResponse(
                perioder = listOf(
                    PeriodeOvergangsstønad(
                        personIdent = hentPerioderMedFullOvergangsstønadSlot.captured,
                        fomDato = LocalDate.now().minusYears(2),
                        datakilde = PeriodeOvergangsstønad.Datakilde.EF,
                        tomDato = LocalDate.now().minusMonths(3),
                    )
                )
            )
        }

        return efSakRestClient
    }
}
