package no.nav.familie.ba.sak.kjerne.steg

import defaultFagsak
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonRepository
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrerInstitusjonStegTest {
    private val fagsakRepositoryMock: FagsakRepository = mockk()
    private val loggServiceMock: LoggService = mockk()
    private val behandlingHentOgPersisterServiceMock: BehandlingHentOgPersisterService = mockk()
    private val fagsakServiceMock: FagsakService = mockk(relaxed = true)
    private val institusjonRepositoryMock: InstitusjonRepository = mockk()

    private lateinit var institusjonService: InstitusjonService
    private lateinit var registrerInstitusjon: RegistrerInstitusjon

    @BeforeAll
    fun setUp() {
        institusjonService =
            InstitusjonService(
                fagsakRepository = fagsakRepositoryMock,
                samhandlerKlient = mockk(relaxed = true),
                institusjonRepository = institusjonRepositoryMock,
            )
        registrerInstitusjon =
            RegistrerInstitusjon(
                institusjonService,
                loggServiceMock,
                behandlingHentOgPersisterServiceMock,
                fagsakServiceMock,
            )
    }

    @BeforeEach
    fun init() {
        clearMocks(loggServiceMock)
    }

    @Test
    fun `utførStegOgAngiNeste() skal lagre institusjon og verge`() {
        val behandling = lagBehandling(fagsak = defaultFagsak().copy(type = FagsakType.INSTITUSJON))
        val fagsakSlot = slot<Fagsak>()
        every { fagsakRepositoryMock.finnFagsak(any()) } returns behandling.fagsak
        every { fagsakServiceMock.lagre(capture(fagsakSlot)) } returns behandling.fagsak
        every { loggServiceMock.opprettRegistrerInstitusjonLogg(any()) } just runs
        every { institusjonRepositoryMock.findByOrgNummer(any()) } returns
            Institusjon(
                orgNummer = "12345",
                tssEksternId = "cool tsr",
            )
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling
        val institusjon = Institusjon(orgNummer = "12345", tssEksternId = "cool tsr")

        registrerInstitusjon.utførStegOgAngiNeste(
            behandling,
            institusjon,
        )

        assertThat(fagsakSlot.captured.institusjon!!.orgNummer).isEqualTo(institusjon.orgNummer)
        verify(exactly = 1) {
            loggServiceMock.opprettRegistrerInstitusjonLogg(any())
        }
    }

    @Test
    fun `utførStegOgAngiNeste() skal returnere REGISTRERE_SØKNAD som neste steg`() {
        val behandling =
            lagBehandling(
                fagsak =
                    defaultFagsak().copy(
                        type = FagsakType.INSTITUSJON,
                        institusjon = Institusjon(orgNummer = "12345", tssEksternId = "tss"),
                    ),
            )
        every { fagsakRepositoryMock.finnFagsak(any()) } returns behandling.fagsak
        every { fagsakRepositoryMock.save(any()) } returns behandling.fagsak
        every { loggServiceMock.opprettRegistrerInstitusjonLogg(any()) } just runs
        every { institusjonRepositoryMock.findByOrgNummer("12345") } returns behandling.fagsak.institusjon
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling
        val institusjon = Institusjon(orgNummer = "12345", tssEksternId = "cool tsr")

        val nesteSteg =
            registrerInstitusjon.utførStegOgAngiNeste(
                behandling,
                institusjon,
            )

        assertThat(nesteSteg).isEqualTo(StegType.REGISTRERE_SØKNAD)
    }
}
