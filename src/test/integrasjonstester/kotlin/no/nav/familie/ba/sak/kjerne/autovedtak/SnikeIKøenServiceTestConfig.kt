package no.nav.familie.ba.sak.kjerne.autovedtak

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.logg.LoggRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDateTime

@Profile("snike-i-koen-test-config")
@Primary
@TestConfiguration
class SnikeIKøenServiceTestConfig(
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired
    private val loggRepository: LoggRepository,
    @Autowired
    private val loggService: LoggService,
    @Autowired
    private val påVentService: SettPåVentService,
    @Autowired
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) : SnikeIKøenService(behandlingHentOgPersisterService, påVentService, loggService, tilbakestillBehandlingService) {
    override fun kanSnikeForbi(aktivOgÅpenBehandling: Behandling): Boolean {
        mockEndringstidspunkt(aktivOgÅpenBehandling)
        return super.kanSnikeForbi(aktivOgÅpenBehandling)
    }

    private fun mockEndringstidspunkt(aktivOgÅpenBehandling: Behandling) {
        aktivOgÅpenBehandling.endretTidspunkt = endringstidspunktMock
        loggRepository.hentLoggForBehandling(aktivOgÅpenBehandling.id).forEach {
            loggRepository.deleteById(it.id)
            loggRepository.saveAndFlush(it.copy(opprettetTidspunkt = endringstidspunktMock))
        }
    }

    companion object {
        var endringstidspunktMock = LocalDateTime.now()
    }
}
