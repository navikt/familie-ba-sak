package no.nav.familie.ba.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBrevmottakerDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrevmottakerHenterTest {
    private val brevmottakerRepository: BrevmottakerRepository = mockk()

    private val brevmottakerHenter = BrevmottakerHenter(brevmottakerRepository = brevmottakerRepository)

    @Nested
    inner class HentBrevmottakere {
        @Test
        fun `skal hente brevmottakere for behandling`() {
            // Arrange
            val behandlingId = 1L
            val brevmottakere =
                listOf(
                    lagBrevmottakerDb(behandlingId = behandlingId, type = MottakerType.FULLMEKTIG),
                    lagBrevmottakerDb(behandlingId = behandlingId, type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE),
                )

            every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns brevmottakere

            // Act
            val resultat = brevmottakerHenter.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(resultat).isEqualTo(brevmottakere)
        }

        @Test
        fun `skal returnere tom liste når behandlingen ikke har brevmottakere`() {
            // Arrange
            val behandlingId = 1L

            every { brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId) } returns emptyList()

            // Act
            val resultat = brevmottakerHenter.hentBrevmottakere(behandlingId)

            // Assert
            assertThat(resultat).isEmpty()
        }
    }
}
