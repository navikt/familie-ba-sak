package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FinnmarkstilleggKjøringRepositoryTest(
    @Autowired private val finnmarkstilleggKjøringRepository: FinnmarkstilleggKjøringRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setup() {
        finnmarkstilleggKjøringRepository.deleteAll()
    }

    @Test
    fun `skal kunne lagre FinnmarkstilleggKjøring`() {
        // Arrange
        val aktør = tilAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsak = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }

        // Act
        val lagretFinnmarkstilleggKjøring = FinnmarkstilleggKjøring(fagsakId = fagsak.id).also { finnmarkstilleggKjøringRepository.saveAndFlush(it) }

        // Assert
        assertThat(lagretFinnmarkstilleggKjøring).isNotNull()
        assertThat(lagretFinnmarkstilleggKjøring.id).isNotEqualTo(0L)
        assertThat(lagretFinnmarkstilleggKjøring.fagsakId).isEqualTo(fagsak.id)
    }

    @Test
    fun `findByFagsakId skal returnere FinnmarkstilleggKjøring når den finnes`() {
        // Arrange
        val aktør = tilAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsak = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }

        FinnmarkstilleggKjøring(fagsakId = fagsak.id).also { finnmarkstilleggKjøringRepository.saveAndFlush(it) }

        // Act
        val hentetFinnmarkstilleggKjøring = finnmarkstilleggKjøringRepository.findByFagsakId(fagsak.id)

        // Assert
        assertThat(hentetFinnmarkstilleggKjøring).isNotNull()
        assertThat(hentetFinnmarkstilleggKjøring!!.fagsakId).isEqualTo(fagsak.id)
    }

    @Test
    fun `findByFagsakId skal returnere null når FinnmarkstilleggKjøring ikke finnes`() {
        // Act
        val hentetFinnmarkstilleggKjøring = finnmarkstilleggKjøringRepository.findByFagsakId(0)

        // Assert
        assertThat(hentetFinnmarkstilleggKjøring).isNull()
    }
}
