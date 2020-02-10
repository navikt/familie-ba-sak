package no.nav.familie.ba.sak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.vedtak.BarnBeregning
import no.nav.familie.ba.sak.behandling.domene.vedtak.NyttVedtak
import no.nav.familie.ba.sak.behandling.domene.vedtak.VedtakResultat
import no.nav.familie.ba.sak.util.DbContainerInitializer
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-dokgen-negative")
@Tag("integration")
class BehandlingNegativeIntegrationTest(@Autowired
                                        private val behandlingService: BehandlingService) {

    @Test
    @Tag("integration")
    fun `Hent HTML vedtaksbrev Negative'`() {
        val failRess = behandlingService.hentHtmlVedtakForBehandling(100)
        Assertions.assertEquals(Ressurs.Status.FEILET, failRess.status)

        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent("6")
        val behandling = behandlingService.opprettNyBehandlingPåFagsak(fagsak, "sdf", BehandlingType.FØRSTEGANGSBEHANDLING, "sak1")
        Assertions.assertNotNull(behandling.fagsak.id)
        Assertions.assertNotNull(behandling.id)

        behandlingService.nyttVedtakForAktivBehandling(
                fagsakId = behandling.fagsak.id ?: 1L,
                nyttVedtak = NyttVedtak("sakstype",
                                        arrayOf(BarnBeregning(fødselsnummer = "123456789011",
                                                              beløp = 1054,
                                                              stønadFom = LocalDate.now())),
                                        resultat = VedtakResultat.INNVILGET),
                ansvarligSaksbehandler = "ansvarligSaksbehandler"
        )

        val htmlRess = behandlingService.hentHtmlVedtakForBehandling(behandling.id!!)
        assert(htmlRess.status == Ressurs.Status.FEILET)
    }
}
