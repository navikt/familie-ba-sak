package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg.domene

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SvalbardstilleggKjøringRepositoryTest(
    @Autowired private val svalbardstilleggKjøringRepository: SvalbardstilleggKjøringRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setup() {
        svalbardstilleggKjøringRepository.deleteAll()
    }

    @Nested
    inner class SaveAndFlush {
        @Test
        fun `skal kunne lagre SvalbardstilleggKjøring`() {
            // Arrange
            val aktør = tilAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }
            val svalbardstilleggKjøring = SvalbardstilleggKjøring(fagsakId = fagsak.id)

            // Act
            val lagretSvalbardstilleggKjøring = svalbardstilleggKjøringRepository.saveAndFlush(svalbardstilleggKjøring)

            // Assert
            assertThat(lagretSvalbardstilleggKjøring).isNotNull()
            assertThat(lagretSvalbardstilleggKjøring.id).isNotEqualTo(0L)
            assertThat(lagretSvalbardstilleggKjøring.fagsakId).isEqualTo(fagsak.id)
        }
    }

    @Nested
    inner class FindByFagsakId {
        @Test
        fun `skal returnere SvalbardstilleggKjøring når den finnes`() {
            // Arrange
            val aktør = tilAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }
            val svalbardstilleggKjøring = SvalbardstilleggKjøring(fagsakId = fagsak.id)
            svalbardstilleggKjøringRepository.saveAndFlush(svalbardstilleggKjøring)

            // Act
            val hentetSvalbardstilleggKjøring = svalbardstilleggKjøringRepository.findByFagsakId(fagsak.id)

            // Assert
            assertThat(hentetSvalbardstilleggKjøring).isNotNull()
            assertThat(hentetSvalbardstilleggKjøring!!.fagsakId).isEqualTo(fagsak.id)
        }

        @Test
        fun `skal returnere null når SvalbardstilleggKjøring ikke finnes`() {
            // Act
            val hentetSvalbardstilleggKjøring = svalbardstilleggKjøringRepository.findByFagsakId(0)

            // Assert
            assertThat(hentetSvalbardstilleggKjøring).isNull()
        }
    }

    @Nested
    inner class FindByFagsakIdIn {
        @Test
        fun `skal returnere alle SvalbardstilleggKjøring som er knyttet til de innsendte fagsak IDene`() {
            // Arrange
            val aktør1 = tilAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak1 = lagFagsakUtenId(aktør = aktør1).also { fagsakRepository.saveAndFlush(it) }
            svalbardstilleggKjøringRepository.saveAndFlush(SvalbardstilleggKjøring(fagsakId = fagsak1.id))

            val aktør2 = tilAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak2 = lagFagsakUtenId(aktør = aktør2).also { fagsakRepository.saveAndFlush(it) }
            svalbardstilleggKjøringRepository.saveAndFlush(SvalbardstilleggKjøring(fagsakId = fagsak2.id))

            val aktør3 = tilAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak3 = lagFagsakUtenId(aktør = aktør3).also { fagsakRepository.saveAndFlush(it) }

            val fagsakIder = setOf(fagsak1.id, fagsak2.id, fagsak3.id)

            // Act
            val hentetSvalbardstilleggKjøring = svalbardstilleggKjøringRepository.findByFagsakIdIn(fagsakIder)

            // Assert
            assertThat(hentetSvalbardstilleggKjøring).hasSize(2)
            assertThat(hentetSvalbardstilleggKjøring).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.fagsakId).isEqualTo(fagsak1.id)
            }
            assertThat(hentetSvalbardstilleggKjøring).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.fagsakId).isEqualTo(fagsak2.id)
            }
        }

        @Test
        fun `skal returnere tom liste når man sender inn et tomt set av fagsak IDer`() {
            // Arrange
            val fagsakIder = emptySet<Long>()

            // Act
            val hentetSvalbardstilleggKjøring = svalbardstilleggKjøringRepository.findByFagsakIdIn(fagsakIder)

            // Assert
            assertThat(hentetSvalbardstilleggKjøring).isEmpty()
        }

        @Test
        fun `skal returnere tom liste hvis ingen SvalbardstilleggKjøring finnes for fagsak IDene`() {
            // Arrange
            val fagsakIder = emptySet<Long>()

            // Act
            val hentetSvalbardstilleggKjøring = svalbardstilleggKjøringRepository.findByFagsakIdIn(fagsakIder)

            // Assert
            assertThat(hentetSvalbardstilleggKjøring).isEmpty()
        }
    }
}
