package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class JournalføringServiceTest(

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val journalføringService: JournalføringService,

    @Autowired
    private val journalføringRepository: JournalføringRepository

) : AbstractSpringIntegrationTest() {

    @Test
    fun `lagrer journalpostreferanse til behandling og fagsak til journalpost`() {

        val søkerFnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsak(PersonIdent(søkerFnr))
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val (sak, behandlinger) = journalføringService
            .lagreJournalpostOgKnyttFagsakTilJournalpost(listOf(behandling.id.toString()), "12345")

        val journalposter = journalføringRepository.findByBehandlingId(behandlingId = behandling.id)

        assertEquals(1, journalposter.size)
        assertEquals(DbJournalpostType.I, journalposter.first().type)
        assertEquals(fagsak.id.toString(), sak.fagsakId)
        assertEquals(1, behandlinger.size)
    }

    @Test
    fun `ferdigstill skal oppdatere journalpost med GENERELL_SAKSTYPE hvis knyttTilFagsak er false`() {
        val søkerFnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsak(PersonIdent(søkerFnr))
        behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val (sak, behandlinger) = journalføringService
            .lagreJournalpostOgKnyttFagsakTilJournalpost(listOf(), "12345")

        assertNull(sak.fagsakId)
        assertEquals(Sakstype.GENERELL_SAK.type, sak.sakstype)
        assertEquals(0, behandlinger.size)
    }
}
