package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg.domene

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SvalbardtilleggKjøringRepositoryTest(
    @Autowired private val svalbardtilleggKjøringRepository: SvalbardtilleggKjøringRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setup() {
        svalbardtilleggKjøringRepository.deleteAll()
    }

    @Nested
    inner class SaveAndFlush {
        @Test
        fun `skal kunne lagre SvalbardtilleggKjøring`() {
            // Arrange
            val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }
            val svalbardtilleggKjøring = SvalbardtilleggKjøring(fagsakId = fagsak.id)

            // Act
            val lagretSvalbardtilleggKjøring = svalbardtilleggKjøringRepository.saveAndFlush(svalbardtilleggKjøring)

            // Assert
            assertThat(lagretSvalbardtilleggKjøring).isNotNull()
            assertThat(lagretSvalbardtilleggKjøring.id).isNotEqualTo(0L)
            assertThat(lagretSvalbardtilleggKjøring.fagsakId).isEqualTo(fagsak.id)
        }
    }

    @Nested
    inner class FindByFagsakId {
        @Test
        fun `skal returnere SvalbardtilleggKjøring når den finnes`() {
            // Arrange
            val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }
            val svalbardtilleggKjøring = SvalbardtilleggKjøring(fagsakId = fagsak.id)
            svalbardtilleggKjøringRepository.saveAndFlush(svalbardtilleggKjøring)

            // Act
            val hentetSvalbardtilleggKjøring = svalbardtilleggKjøringRepository.findByFagsakId(fagsak.id)

            // Assert
            assertThat(hentetSvalbardtilleggKjøring).isNotNull()
            assertThat(hentetSvalbardtilleggKjøring!!.fagsakId).isEqualTo(fagsak.id)
        }

        @Test
        fun `skal returnere null når SvalbardtilleggKjøring ikke finnes`() {
            // Act
            val hentetSvalbardtilleggKjøring = svalbardtilleggKjøringRepository.findByFagsakId(0)

            // Assert
            assertThat(hentetSvalbardtilleggKjøring).isNull()
        }
    }

    @Nested
    inner class FindByFagsakIdIn {
        @Test
        fun `skal returnere alle SvalbardtilleggKjøring som er knyttet til de innsendte fagsak IDene`() {
            // Arrange
            val aktør1 = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak1 = lagFagsakUtenId(aktør = aktør1).also { fagsakRepository.saveAndFlush(it) }
            svalbardtilleggKjøringRepository.saveAndFlush(SvalbardtilleggKjøring(fagsakId = fagsak1.id))

            val aktør2 = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak2 = lagFagsakUtenId(aktør = aktør2).also { fagsakRepository.saveAndFlush(it) }
            svalbardtilleggKjøringRepository.saveAndFlush(SvalbardtilleggKjøring(fagsakId = fagsak2.id))

            val aktør3 = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
            val fagsak3 = lagFagsakUtenId(aktør = aktør3).also { fagsakRepository.saveAndFlush(it) }

            val fagsakIder = setOf(fagsak1.id, fagsak2.id, fagsak3.id)

            // Act
            val hentetSvalbardtilleggKjøring = svalbardtilleggKjøringRepository.findByFagsakIdIn(fagsakIder)

            // Assert
            assertThat(hentetSvalbardtilleggKjøring).hasSize(2)
            assertThat(hentetSvalbardtilleggKjøring).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.fagsakId).isEqualTo(fagsak1.id)
            }
            assertThat(hentetSvalbardtilleggKjøring).anySatisfy {
                assertThat(it.id).isNotNull()
                assertThat(it.fagsakId).isEqualTo(fagsak2.id)
            }
        }

        @Test
        fun `skal returnere tom liste når man sender inn et tomt set av fagsak IDer`() {
            // Arrange
            val fagsakIder = emptySet<Long>()

            // Act
            val hentetSvalbardtilleggKjøring = svalbardtilleggKjøringRepository.findByFagsakIdIn(fagsakIder)

            // Assert
            assertThat(hentetSvalbardtilleggKjøring).isEmpty()
        }

        @Test
        fun `skal returnere tom liste hvis ingen SvalbardtilleggKjøring finnes for fagsak IDene`() {
            // Arrange
            val fagsakIder = emptySet<Long>()

            // Act
            val hentetSvalbardtilleggKjøring = svalbardtilleggKjøringRepository.findByFagsakIdIn(fagsakIder)

            // Assert
            assertThat(hentetSvalbardtilleggKjøring).isEmpty()
        }
    }
}
