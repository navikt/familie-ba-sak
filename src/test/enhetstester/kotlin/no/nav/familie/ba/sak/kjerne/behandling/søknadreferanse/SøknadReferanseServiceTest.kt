import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse.SøknadReferanse
import no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse.SøknadReferanseRepository
import no.nav.familie.ba.sak.kjerne.behandling.søknadreferanse.SøknadReferanseService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SøknadReferanseServiceTest {
    private val søknadReferanseRepository = mockk<SøknadReferanseRepository>(relaxed = true)
    private val søknadReferanseService = SøknadReferanseService(søknadReferanseRepository)
    private val behandlingId = 0L
    private val journalpostId = "123456789"

    @BeforeEach
    fun setUp() {
        every { søknadReferanseRepository.save(any()) } returns mockk()
        every { søknadReferanseRepository.findByBehandlingId(any()) } returns mockk()
    }

    @Test
    fun lagreSøknadReferanse() {
        søknadReferanseService.lagreSøknadReferanse(behandlingId = behandlingId, journalpostId = journalpostId)
        verify(exactly = 1) { søknadReferanseRepository.save(SøknadReferanse(behandlingId = behandlingId, journalpostId = journalpostId)) }
    }

    @Test
    fun hentSøknadReferanse() {
        søknadReferanseService.hentSøknadReferanse(behandlingId = behandlingId)
        verify(exactly = 1) { søknadReferanseRepository.findByBehandlingId(behandlingId) }
    }
}
