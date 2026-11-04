package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegistrerInstitusjonStegTest {
    private val loggServiceMock: LoggService = mockk()
    private val behandlingHentOgPersisterServiceMock: BehandlingHentOgPersisterService = mockk()
    private val institusjonServiceMock: InstitusjonService = mockk()

    private val registrerInstitusjon =
        RegistrerInstitusjon(
            institusjonServiceMock,
            loggServiceMock,
            behandlingHentOgPersisterServiceMock,
        )

    @BeforeEach
    fun init() {
        clearMocks(loggServiceMock, behandlingHentOgPersisterServiceMock, institusjonServiceMock)
        every { loggServiceMock.opprettRegistrerInstitusjonLogg(any()) } just runs
        every { institusjonServiceMock.lagreInstitusjonsinfo(any()) } just runs
    }

    @Test
    fun `utførStegOgAngiNeste() skal bruke institusjonen på fagsaken uten å endre fagsaken`() {
        // Arrange
        val institusjon = Institusjon(orgNummer = "12345", tssEksternId = "tss")
        val behandling =
            lagBehandling(
                fagsak = defaultFagsak().copy(type = FagsakType.INSTITUSJON, institusjon = institusjon),
            )
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling

        // Act
        registrerInstitusjon.utførStegOgAngiNeste(behandling, "")

        // Assert
        assertThat(behandling.fagsak.institusjon).isSameAs(institusjon)
        verify(exactly = 0) { institusjonServiceMock.hentEllerOpprettInstitusjon(any(), any()) }
        verify(exactly = 1) { loggServiceMock.opprettRegistrerInstitusjonLogg(behandling) }
        verify(exactly = 1) { institusjonServiceMock.lagreInstitusjonsinfo(behandling.id) }
    }

    @Test
    fun `utførStegOgAngiNeste() skal kaste feil når fagsaken mangler institusjon`() {
        // Arrange
        val behandling =
            lagBehandling(
                fagsak = defaultFagsak().copy(type = FagsakType.INSTITUSJON, institusjon = null),
            )

        // Act & Assert
        val feil = assertThrows<Feil> { registrerInstitusjon.utførStegOgAngiNeste(behandling, "") }
        assertThat(feil.message).contains("mangler institusjon")
        verify(exactly = 0) { loggServiceMock.opprettRegistrerInstitusjonLogg(any()) }
        verify(exactly = 0) { institusjonServiceMock.lagreInstitusjonsinfo(any()) }
    }

    @Test
    fun `utførStegOgAngiNeste() skal ikke lagre institusjonsinfo når behandlingen er låst`() {
        // Arrange
        val behandling =
            lagBehandling(
                fagsak =
                    defaultFagsak().copy(
                        type = FagsakType.INSTITUSJON,
                        institusjon = Institusjon(orgNummer = "12345", tssEksternId = "tss"),
                    ),
                status = BehandlingStatus.FATTER_VEDTAK,
            )
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling

        // Act
        registrerInstitusjon.utførStegOgAngiNeste(behandling, "")

        // Assert
        verify(exactly = 1) { loggServiceMock.opprettRegistrerInstitusjonLogg(behandling) }
        verify(exactly = 0) { institusjonServiceMock.lagreInstitusjonsinfo(any()) }
    }

    @Test
    fun `utførStegOgAngiNeste() skal returnere REGISTRERE_SØKNAD som neste steg`() {
        // Arrange
        val behandling =
            lagBehandling(
                fagsak =
                    defaultFagsak().copy(
                        type = FagsakType.INSTITUSJON,
                        institusjon = Institusjon(orgNummer = "12345", tssEksternId = "tss"),
                    ),
            )
        every { behandlingHentOgPersisterServiceMock.hent(any()) } returns behandling

        // Act
        val nesteSteg = registrerInstitusjon.utførStegOgAngiNeste(behandling, "")

        // Assert
        assertThat(nesteSteg).isEqualTo(StegType.REGISTRERE_SØKNAD)
    }
}
