package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.RolleTilgangskontrollFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
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
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RollestyringMotDatabaseTest(
        @Autowired
        private val databaseCleanupService: DatabaseCleanupService,

        @Autowired
        private val fagsakService: FagsakService
) {
    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal kaste feil når innlogget bruker ikke har skrivetilgang`() {
        val fnr = randomFnr()

        assertThrows<RolleTilgangskontrollFeil> { fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr) }
    }
}