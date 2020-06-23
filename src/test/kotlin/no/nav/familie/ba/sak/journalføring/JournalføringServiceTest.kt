package no.nav.familie.ba.sak.journalføring

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("dev")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JournalføringServiceTest(

        @Autowired
        private val behandlingService: BehandlingService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val journalføringService: JournalføringService,

        @Autowired
        private val journalføringRepository: JournalføringRepository

) {



    @Test
    fun `lagrer journalpostreferanse til behandling og fagsak til journalpost`() {

        val søkerFnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsak(PersonIdent(søkerFnr))
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val (sak, behandlinger) = journalføringService
                .lagreJournalpostOgKnyttFagsakTilJournalpost(listOf(behandling.id.toString()), "12345")

        val journalposter = journalføringRepository.findByBehandlingId(behandlingId = behandling.id)

        assertEquals(1, journalposter.size)
        assertEquals(fagsak.id.toString(), sak.fagsakId)
        assertEquals(1, behandlinger.size)

    }

    @Test
    fun `ferdigstill skal oppdatere journalpost med GENERELL_SAKSTYPE hvis knyttTilFagsak er false`() {
        val søkerFnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsak(PersonIdent(søkerFnr))
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val (sak, behandlinger) = journalføringService
                .lagreJournalpostOgKnyttFagsakTilJournalpost(listOf(), "12345")

        assertNull(sak.fagsakId)
        assertEquals(Sakstype.GENERELL_SAK.type, sak.sakstype)
        assertEquals(0, behandlinger.size)

    }

}