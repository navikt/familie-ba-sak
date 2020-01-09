package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.FagsakService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.sikkerhet.OIDCUtil
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class FagsakControllerTest (@Autowired private val oidcUtil: OIDCUtil, @Autowired private val fagsakService: FagsakService){

    @Test
    @Tag("integration")
    fun `Test hent html vedtak`(){
        val mockBehandlingLager= mock(BehandlingService::class.java)
        `when`(mockBehandlingLager.hentHtmlVedtakForBehandling(ArgumentMatchers.anyLong())).thenReturn(Ressurs.success(("mock_html")))
        val fagsakController= FagsakController(oidcUtil, fagsakService, mockBehandlingLager)
        val response= fagsakController.hentHtmlVedtak(1)
        assert(response.status== Ressurs.Status.SUKSESS)
    }
}