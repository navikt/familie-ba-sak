package no.nav.familie.ba.sak.auditlogger

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.FagsakController
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.util.lagRandomSaksnummer
import no.nav.familie.ba.sak.util.randomFnr
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("dev")
@Tag("integration")
class AuditLoggerTest(
        @Autowired
        private val fagsakController: FagsakController,

        @Autowired
        private val behandlingService: BehandlingService) {

    @Test
    fun `Skal auditlogge henting av fagsak`() {
        val logger: Logger = LoggerFactory.getLogger("auditLogger" + "." + FagsakController::class.java.name) as Logger

        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()

        logger.addAppender(listAppender)

        val fnr = randomFnr()
        val fagsak = behandlingService.hentEllerOpprettFagsakForPersonIdent(fnr)
        behandlingService.opprettNyBehandlingPåFagsak(fagsak,
                                                      null,
                                                      BehandlingType.FØRSTEGANGSBEHANDLING,
                                                      lagRandomSaksnummer(),
                                                      BehandlingKategori.NASJONAL,
                                                      BehandlingUnderkategori.ORDINÆR)

        fagsakController.hentFagsak(fagsak.id!!)

        val logsList = listAppender.list
        Assertions.assertEquals(1, logsList.size)
        Assertions.assertEquals("action=FAGSAK actionType=READ FAGSAK_ID=1 ANSVALIG_SAKSBEHANDLER=DEV_preferred_username",
                                logsList[0]
                                        .message)
    }
}