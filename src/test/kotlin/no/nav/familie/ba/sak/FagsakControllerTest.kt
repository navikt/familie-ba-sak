package no.nav.familie.ba.sak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.Fagsak
import no.nav.familie.ba.sak.behandling.domene.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.sikkerhet.OIDCUtil
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class FagsakControllerTest(
        @Autowired
        private val oidcUtil: OIDCUtil,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val taskRepository: TaskRepository
) {

    @Test
    @Tag("integration")
    fun `Test hent html vedtak`() {
        val mockBehandlingLager: BehandlingService = mockk()
        every { mockBehandlingLager.hentHtmlVedtakForBehandling(any()) } returns Ressurs.success("mock_html")

        val fagsakController = FagsakController(oidcUtil, fagsakService, mockBehandlingLager, taskRepository)
        val response = fagsakController.hentHtmlVedtak(1)
        assert(response.status == Ressurs.Status.SUKSESS)
    }


    @Test
    @Tag("integration")
    fun `Test opphør vedtak`() {
        val mockBehandlingLager: BehandlingService = mockk()

        val fagsak = Fagsak(1, AktørId("1"), PersonIdent("1"))
        val behandling = Behandling(1,fagsak,null,BehandlingType.MIGRERING_FRA_INFOTRYGD,"1",status = BehandlingStatus.IVERKSATT)
        val vedtak = Vedtak(1, behandling, "sb", LocalDate.now(),"",VedtakResultat.INNVILGET)

        every { mockBehandlingLager.hentBehandlingHvisEksisterer(any())} returns behandling
        every { mockBehandlingLager.hentVedtakHvisEksisterer(any())} returns vedtak
        val fagsakController = FagsakController(oidcUtil, fagsakService, mockBehandlingLager, taskRepository)

        val response = fagsakController.opphørMigrertVedtak(1)
        assert(response.statusCode ==HttpStatus.OK)
    }

}
