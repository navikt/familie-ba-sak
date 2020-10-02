package no.nav.familie.ba.sak.behandling

import io.mockk.every
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.pdl.internal.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev", "mock-pdl")
@Tag("integration")
class TilgangControllerTest (
        @Autowired
        private val tilgangController: TilgangController,

        @Autowired
        private val mockPersonopplysningerService: PersonopplysningerService,

        @Autowired
        private val mockIntegrasjonClient: IntegrasjonClient
) {
    @Test
    fun testHarTilgangTilKode6Person() {
        every {
            mockPersonopplysningerService.hentAdressebeskyttelseSomSystembruker(any())
        } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        every {
            mockIntegrasjonClient.sjekkTilgangTilPersoner(listOf(any()))
        } returns listOf(Tilgang(harTilgang = true))
        val response = tilgangController.hentTilgangOgDiskresjonskode(FnrGenerator.generer())
        Assertions.assertThat(response.body!!.data!!.adressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)

    }


}