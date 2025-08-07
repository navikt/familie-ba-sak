package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class FinnmarkstilleggKjøringRepositoryTest(
    @Autowired private val finnmarkstilleggKjøringRepository: FinnmarkstilleggKjøringRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
) : AbstractSpringIntegrationTest() {
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
        assertThat(hentetFinnmarkstilleggKjøring.startTidspunkt).isNotNull()
        assertThat(hentetFinnmarkstilleggKjøring.ferdigTidspunkt).isNull()
    }

    @Test
    fun `findByFagsakId skal returnere null når FinnmarkstilleggKjøring ikke finnes`() {
        // Act
        val hentetFinnmarkstilleggKjøring = finnmarkstilleggKjøringRepository.findByFagsakId(0)

        // Assert
        assertThat(hentetFinnmarkstilleggKjøring).isNull()
    }

    @Test
    fun `skal kunne lagre og hente FinnmarkstilleggKjøring med ferdigTidspunkt`() {
        // Arrange
        val aktør = tilAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsak = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }

        val startTid = LocalDateTime.now().truncatedTo(SECONDS)
        val ferdigTid = LocalDateTime.now().truncatedTo(SECONDS)

        FinnmarkstilleggKjøring(
            fagsakId = fagsak.id,
            startTidspunkt = startTid,
            ferdigTidspunkt = ferdigTid,
        ).also { finnmarkstilleggKjøringRepository.saveAndFlush(it) }

        // Act
        val hentetFinnmarkstilleggKjøring = finnmarkstilleggKjøringRepository.findByFagsakId(fagsak.id)

        // Assert
        assertThat(hentetFinnmarkstilleggKjøring).isNotNull()
        assertThat(hentetFinnmarkstilleggKjøring!!.fagsakId).isEqualTo(fagsak.id)
        assertThat(hentetFinnmarkstilleggKjøring.startTidspunkt).isEqualTo(startTid)
        assertThat(hentetFinnmarkstilleggKjøring.ferdigTidspunkt).isEqualTo(ferdigTid)
    }
}
