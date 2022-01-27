package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@Tag("integration")
class SettPåVentServiceTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val behandlingRepository: BehandlingRepository,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val mockPersonopplysningerService: PersonopplysningerService,

    @Autowired
    private val settPåVentService: SettPåVentService
) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `se`() {
        // databaseCleanupService.truncate()
        // settPåVentService.settBehandlingPåVent(behandlingId =)

        // val fnr = randomFnr()

        // val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)

        // stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyOrdinærBehandling(fnr))

        val fnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = stegService.håndterNyBehandlingOgSendInfotrygdFeed(nyOrdinærBehandling(fnr))

        settPåVentService.settBehandlingPåVent(
            behandlingId = behandling.id,
            frist = LocalDate.now().plusDays(21),
            årsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )

        behandling
    }
}
