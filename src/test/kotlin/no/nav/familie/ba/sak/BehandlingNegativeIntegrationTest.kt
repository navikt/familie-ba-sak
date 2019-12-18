package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingslagerService
import no.nav.familie.ba.sak.behandling.DokGenService
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.FagsakRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.BarnBeregning
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakBarnRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.BehandlingVedtakRepository
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.lang.RuntimeException
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen-negative")
@Tag("integration")
class BehandlingNegativeIntegrationTest (
        @Autowired
        private var behandlingslagerService: BehandlingslagerService
        ){
    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev Negative'`() {
        val failRess = behandlingslagerService.hentHtmlVedtakForBehandling(100);
        Assertions.assertEquals(Ressurs.Status.FEILET, failRess.status)

        val behandling = behandlingslagerService.nyBehandling("0", arrayOf("123456789010"), BehandlingType.FØRSTEGANGSBEHANDLING, "sdf", "sak1")
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        behandlingslagerService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak("sakstype", arrayOf(BarnBeregning(fødselsnummer = "123456789011", beløp = 1054, stønadFom = LocalDate.now()))),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val htmlRess= behandlingslagerService.hentHtmlVedtakForBehandling(behandling.id!!)
        assert(htmlRess.status== Ressurs.Status.FEILET)
    }
}