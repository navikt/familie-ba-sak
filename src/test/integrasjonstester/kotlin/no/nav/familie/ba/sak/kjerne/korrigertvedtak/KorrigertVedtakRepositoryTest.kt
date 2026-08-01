package no.nav.familie.ba.sak.kjerne.korrigertvedtak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

class KorrigertVedtakRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val korrigertVedtakRepository: KorrigertVedtakRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `finnAktivtKorrigertVedtakPåBehandling skal returnere null dersom det ikke eksisterer en aktiv korrigering av vedtak på behandling`() {
        // Arrange
        val behandling = opprettBehandling()

        val inaktivKorrigertVedtak =
            KorrigertVedtak(
                vedtaksdato = LocalDate.now().minusDays(6),
                begrunnelse = "Test på inaktiv korrigering",
                behandling = behandling,
                aktiv = false,
            )

        korrigertVedtakRepository.saveAndFlush(inaktivKorrigertVedtak)

        // Act
        val ikkeEksisterendeKorrigertVedtak =
            korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id)

        // Assert
        Assertions.assertNull(ikkeEksisterendeKorrigertVedtak, "Skal ikke finnes aktiv korrigert vedtak på behandling")
    }

    @Test
    fun `finnAktivtKorrigertVedtakPåBehandling skal returnere aktiv korrigert vedtak når det eksisterer en aktiv korrigering av vedtak på behandling`() {
        // Arrange
        val behandling = opprettBehandling()

        val aktivKorrigertVedtak =
            KorrigertVedtak(
                vedtaksdato = LocalDate.now().minusDays(6),
                begrunnelse = "Test på aktiv korrigering",
                behandling = behandling,
                aktiv = true,
            )

        korrigertVedtakRepository.saveAndFlush(aktivKorrigertVedtak)

        // Act
        val eksisterendeKorrigertVedtak =
            korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id)

        // Assert
        Assertions.assertNotNull(
            eksisterendeKorrigertVedtak,
            "Skal finnes aktiv korrigert vedtak på behandling",
        )
    }

    @Test
    fun `Det skal kastes DataIntegrityViolationException dersom det forsøkes å lagre aktivt korrigert vedtak når det allerede finnes en`() {
        // Arrange
        val behandling = opprettBehandling()

        val aktivKorrigertVedtak1 =
            KorrigertVedtak(
                begrunnelse = "Test på aktiv korrigering",
                vedtaksdato = LocalDate.now().minusDays(6),
                behandling = behandling,
                aktiv = true,
            )

        val aktivKorrigertVedtak2 =
            KorrigertVedtak(
                begrunnelse = "Test på aktiv korrigering",
                vedtaksdato = LocalDate.now().minusDays(3),
                behandling = behandling,
                aktiv = true,
            )

        korrigertVedtakRepository.saveAndFlush(aktivKorrigertVedtak1)

        // Act & Assert
        assertThrows<DataIntegrityViolationException> {
            korrigertVedtakRepository.saveAndFlush(aktivKorrigertVedtak2)
        }
    }

    private fun opprettBehandling(): Behandling {
        val søker = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))

        return behandlingRepository.save(lagBehandlingUtenId(fagsak))
    }
}
