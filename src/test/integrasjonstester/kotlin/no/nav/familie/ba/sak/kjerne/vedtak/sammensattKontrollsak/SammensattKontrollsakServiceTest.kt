package no.nav.familie.ba.sak.kjerne.vedtak.sammensattKontrollsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.ekstern.restDomene.OpprettSammensattKontrollsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.SammensattKontrollsakDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.logg.LoggType
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SammensattKontrollsakServiceTest(
    @Autowired
    private val sammensattKontrollsakService: SammensattKontrollsakService,
    @Autowired
    private val aktørIdRepository: AktørIdRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
    @Autowired
    private val loggService: LoggService,
    @Autowired
    private val sammensattKontrollsakRepository: SammensattKontrollsakRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `finnSammensattKontrollsak skal returnere null dersom det ikke finnes noen SammensattKontrollsak knyttet til behandlingsId`() {
        assertThat(sammensattKontrollsakService.finnSammensattKontrollsak(1)).isNull()
    }

    @Test
    fun `finnSammensattKontrollsak skal returnere SammensattKontrollsak dersom det finnes en SammensattKontrollsak knyttet til behandlingsId`() {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

        sammensattKontrollsakRepository.saveAndFlush(SammensattKontrollsak(behandlingId = behandling.id, fritekst = "Fritekst"))

        val sammensattKontrollsak = sammensattKontrollsakRepository.finnSammensattKontrollsakForBehandling(behandling.id)
        assertThat(sammensattKontrollsak).isNotNull
        assertThat(sammensattKontrollsak!!.behandlingId).isEqualTo(behandling.id)
        assertThat(sammensattKontrollsak.fritekst).isEqualTo("Fritekst")
    }

    @Test
    fun `opprettSammensattKontrollsak skal opprette SammensattKontrollsak basert på OpprettSammensattKontrollsakDto`() {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

        val sammensattKontrollsak = sammensattKontrollsakService.opprettSammensattKontrollsak(OpprettSammensattKontrollsakDto(behandlingId = behandling.id, fritekst = "Fritekst"))

        assertThat(sammensattKontrollsak.behandlingId).isEqualTo(behandling.id)
        assertThat(sammensattKontrollsak.fritekst).isEqualTo("Fritekst")

        val loggForBehandling = loggService.hentLoggForBehandling(behandling.id)

        assertThat(loggForBehandling.any { it.type == LoggType.SAMMENSATT_KONTROLLSAK_LAGT_TIL }).isTrue()
    }

    @Test
    fun `oppdaterSammensattKontrollsak skal oppdatere SammensattKontrollsak basert på SammensattKontrollsakDto`() {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

        val eksisterendeSammensattKontrollsak = sammensattKontrollsakService.opprettSammensattKontrollsak(OpprettSammensattKontrollsakDto(behandlingId = behandling.id, fritekst = "Fritekst"))
        val oppdatertSammensattKontrollsak = sammensattKontrollsakService.oppdaterSammensattKontrollsak(SammensattKontrollsakDto(id = eksisterendeSammensattKontrollsak.id, behandlingId = behandling.id, fritekst = "Oppdatert fritekst"))

        assertThat(oppdatertSammensattKontrollsak.id).isEqualTo(eksisterendeSammensattKontrollsak.id)
        assertThat(oppdatertSammensattKontrollsak.behandlingId).isEqualTo(behandling.id)
        assertThat(oppdatertSammensattKontrollsak.fritekst).isEqualTo("Oppdatert fritekst")

        val loggForBehandling = loggService.hentLoggForBehandling(behandling.id)

        assertThat(loggForBehandling.any { it.type == LoggType.SAMMENSATT_KONTROLLSAK_LAGT_TIL }).isTrue()
        assertThat(loggForBehandling.any { it.type == LoggType.SAMMENSATT_KONTROLLSAK_ENDRET }).isTrue()
    }

    @Test
    fun `slettSammensattKontrollsak skal slette SammensattKontrollsak basert på SammensattKontrollsakDto`() {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
        val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

        val eksisterendeSammensattKontrollsak = sammensattKontrollsakService.opprettSammensattKontrollsak(OpprettSammensattKontrollsakDto(behandlingId = behandling.id, fritekst = "Fritekst"))
        sammensattKontrollsakService.slettSammensattKontrollsak(eksisterendeSammensattKontrollsak.id)

        assertThat(sammensattKontrollsakService.finnSammensattKontrollsak(behandlingId = behandling.id)).isNull()

        val loggForBehandling = loggService.hentLoggForBehandling(behandling.id)

        assertThat(loggForBehandling.any { it.type == LoggType.SAMMENSATT_KONTROLLSAK_LAGT_TIL }).isTrue()
        assertThat(loggForBehandling.any { it.type == LoggType.SAMMENSATT_KONTROLLSAK_FJERNET }).isTrue()
    }
}
