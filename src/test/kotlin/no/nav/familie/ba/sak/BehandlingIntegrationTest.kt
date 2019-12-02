package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingslagerService
import no.nav.familie.ba.sak.util.DbContainerInitializer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension



@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
class BehandlingIntegrationTest  {

    @Autowired
    private lateinit var behandlingslagerService: BehandlingslagerService

    @Test
    fun `Kjør flyway migreringer og sjekk at behandlingslagerservice klarer å lese å skrive til postgresql`() {
        behandlingslagerService.nyBehandling("0","123456789010", "sdf")
        Assertions.assertEquals(1, behandlingslagerService.hentAlleBehandlinger().size)
    }

}