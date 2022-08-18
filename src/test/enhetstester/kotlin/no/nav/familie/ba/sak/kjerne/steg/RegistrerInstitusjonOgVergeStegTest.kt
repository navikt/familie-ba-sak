package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerInstitusjonOgVerge
import no.nav.familie.ba.sak.ekstern.restDomene.VergeInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.logg.Logg
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.logg.LoggType
import no.nav.familie.ba.sak.kjerne.verge.VergeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrerInstitusjonOgVergeStegTest {

    private val behandlingServiceMock: BehandlingService = mockk()
    private val fagsakRepositoryMock: FagsakRepository = mockk()
    private val loggServiceMock: LoggService = mockk()
    private val behandlingHentOgPersisterServiceMock: BehandlingHentOgPersisterService = mockk()

    private lateinit var institusjonService: InstitusjonService
    private lateinit var vergeService: VergeService
    private lateinit var registrerInstitusjonOgVerge: RegistrerInstitusjonOgVerge

    @BeforeAll
    fun SetUp() {
        institusjonService =
            InstitusjonService(fagsakRepository = fagsakRepositoryMock, samhandlerKlient = mockk(relaxed = true))
        vergeService = VergeService(behandlingServiceMock)
        registrerInstitusjonOgVerge =
            RegistrerInstitusjonOgVerge(
                institusjonService,
                vergeService,
                loggServiceMock,
                behandlingHentOgPersisterServiceMock
            )
    }

    @Test
    fun `utførStegOgAngiNeste() skal lagre institusjon og verge`() {
        val behandling = lagBehandling(fagsak = defaultFagsak().copy(type = FagsakType.INSTITUSJON))
        val fagsakSlot = slot<Fagsak>()
        val behandlingSlot = slot<Behandling>()
        every { fagsakRepositoryMock.finnFagsak(any()) } returns behandling.fagsak
        every { fagsakRepositoryMock.save(capture(fagsakSlot)) } returns behandling.fagsak
        every { behandlingServiceMock.lagre(capture(behandlingSlot)) } returns behandling
        every { loggServiceMock.opprettRegistrerVergeLogg(any()) } just runs
        every { loggServiceMock.opprettRegistrerInstitusjonLogg(any()) } just runs
        every { loggServiceMock.lagre(any()) } returns Logg(
            behandlingId = behandling.id,
            type = LoggType.VERGE_REGISTRERT,
            tittel = "tittel",
            rolle = BehandlerRolle.SYSTEM,
            tekst = ""
        )
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling
        val restRegistrerInstitusjonOgVerge = RestRegistrerInstitusjonOgVerge(
            vergeInfo = VergeInfo(
                "verge navn",
                "verge adresse",
                "12345678910"
            ),
            institusjonInfo = InstitusjonInfo("12345", "cool tsr")
        )

        registrerInstitusjonOgVerge.utførStegOgAngiNeste(
            behandling,
            restRegistrerInstitusjonOgVerge
        )

        assertThat(fagsakSlot.captured.institusjon!!.orgNummer).isEqualTo(restRegistrerInstitusjonOgVerge.institusjonInfo!!.orgNummer)
        assertThat(behandlingSlot.captured.verge!!.ident).isEqualTo(restRegistrerInstitusjonOgVerge.vergeInfo!!.ident)
        verify(exactly = 1) {
            loggServiceMock.opprettRegistrerVergeLogg(any())
        }
        verify(exactly = 1) {
            loggServiceMock.opprettRegistrerInstitusjonLogg(any())
        }
    }

    @Test
    fun `utførStegOgAngiNeste() skal return REGISTRERE_PERSONGRUNNLAG som neste steg`() {
        val behandling = lagBehandling(fagsak = defaultFagsak().copy(type = FagsakType.INSTITUSJON))
        every { fagsakRepositoryMock.finnFagsak(any()) } returns behandling.fagsak
        every { fagsakRepositoryMock.save(any()) } returns behandling.fagsak
        every { behandlingServiceMock.lagre(any()) } returns behandling
        every { loggServiceMock.opprettRegistrerVergeLogg(any()) } just runs
        every { loggServiceMock.opprettRegistrerInstitusjonLogg(any()) } just runs
        every { loggServiceMock.lagre(any()) } returns Logg(
            behandlingId = behandling.id,
            type = LoggType.VERGE_REGISTRERT,
            tittel = "tittel",
            rolle = BehandlerRolle.SYSTEM,
            tekst = ""
        )
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling
        val restRegistrerInstitusjonOgVerge = RestRegistrerInstitusjonOgVerge(
            vergeInfo = VergeInfo(
                "verge navn",
                "verge adresse",
                "12345678910"
            ),
            institusjonInfo = InstitusjonInfo("12345", "cool tsr")
        )

        val nesteSteg = registrerInstitusjonOgVerge.utførStegOgAngiNeste(
            behandling,
            restRegistrerInstitusjonOgVerge
        )

        assertThat(nesteSteg).isEqualTo(StegType.REGISTRERE_PERSONGRUNNLAG)
    }
}
