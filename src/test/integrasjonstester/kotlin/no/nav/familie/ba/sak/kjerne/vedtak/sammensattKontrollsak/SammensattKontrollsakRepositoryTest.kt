package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

class SammensattKontrollsakRepositoryTest(
    @Autowired
    private val aktørIdRepository: AktørIdRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val sammensattKontrollsakRepository: SammensattKontrollsakRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `save skal kaste feil dersom vi forsøker å lagre ny SammensattKontrollsak og det allerede finnes en SammensattKontrollsak tilknyttet behandling`() {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

        sammensattKontrollsakRepository.saveAndFlush(SammensattKontrollsak(behandlingId = behandling.id, fritekst = "Fritekst"))

        assertThrows<DataIntegrityViolationException> { sammensattKontrollsakRepository.saveAndFlush(SammensattKontrollsak(behandlingId = behandling.id, fritekst = "Fritekst")) }
    }

    @Test
    fun `save skal kaste feil dersom vi forsøker å lagre ny SammensattKontrollsak for en behandlingsId som ikke finnes`() {
        assertThrows<DataIntegrityViolationException> { sammensattKontrollsakRepository.saveAndFlush(SammensattKontrollsak(behandlingId = 50, fritekst = "Fritekst")) }
    }
}
