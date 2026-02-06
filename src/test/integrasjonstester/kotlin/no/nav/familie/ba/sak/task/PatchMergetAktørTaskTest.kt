package no.nav.familie.ba.sak.task

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.fake.FakePdlIdentRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLoggRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class PatchMergetAktørTaskTest(
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fakePdlIdentRestKlient: FakePdlIdentRestKlient,
    @Autowired private val aktørMergeLoggRepository: AktørMergeLoggRepository,
    @Autowired private val entityManager: EntityManager,
) : AbstractSpringIntegrationTest() {
    private val patchMergetAktørTask =
        PatchMergetAktørTask(
            pdlIdentRestKlient = fakePdlIdentRestKlient,
            aktørMergeLoggRepository = aktørMergeLoggRepository,
            aktørIdRepository = aktørIdRepository,
        )

    @Test
    @Transactional
    fun `patcher aktør i fagsak dersom man kaller patch-metode`() {
        // Arrange
        val gammelAktør = aktørIdRepository.saveAndFlush(lagAktør())

        val fagsak = fagsakRepository.saveAndFlush(lagFagsakUtenId(aktør = gammelAktør))

        val nyAktørId = randomAktør().aktørId
        fakePdlIdentRestKlient.leggTilIdent(
            nyAktørId,
            listOf(
                IdentInformasjon(
                    ident = nyAktørId,
                    historisk = false,
                    gruppe = "AKTORID",
                ),
                IdentInformasjon(
                    ident = gammelAktør.aktivFødselsnummer(),
                    historisk = false,
                    gruppe = "FOLKEREGISTERIDENT",
                ),
            ),
        )

        // Act
        patchMergetAktørTask.doTask(
            Task(
                payload =
                    objectMapper.writeValueAsString(
                        PatchMergetAktørDto(
                            fagsakId = fagsak.id,
                            gammelAktørId = gammelAktør.aktørId,
                            nyAktørId = nyAktørId,
                            skalSjekkeAtGammelAktørIdErHistoriskAvNyAktørId = false,
                        ),
                    ),
                type = PatchMergetAktørTask.TASK_STEP_TYPE,
            ),
        )

        aktørMergeLoggRepository.flush()
        // PatchMergetAktørTask bruker on cascade update (ikke JPA), så vi må tømme entityManager for å hente oppdatert data
        entityManager.clear()

        // Assert
        val oppdatertFagsak = fagsakRepository.findByIdOrNull(fagsak.id)

        assertThat(oppdatertFagsak?.aktør?.aktørId).isEqualTo(nyAktørId)

        val aktørMergeLogg =
            aktørMergeLoggRepository.findAll().singleOrNull { it.fagsakId == fagsak.id }

        assertThat(aktørMergeLogg).isNotNull
        assertThat(aktørMergeLogg?.historiskAktørId).isEqualTo(gammelAktør.aktørId)
        assertThat(aktørMergeLogg?.nyAktørId).isEqualTo(nyAktørId)
    }

    @Test
    fun `kaster feil dersom gammel aktør ikke er historisk av ny dersom flagg for sjekk er på`() {
        // Arrange
        val gammelAktør = aktørIdRepository.saveAndFlush(lagAktør())

        val fagsak = fagsakRepository.saveAndFlush(lagFagsakUtenId(aktør = gammelAktør))

        val nyAktørId = randomAktør().aktørId
        fakePdlIdentRestKlient.leggTilIdent(
            nyAktørId,
            listOf(
                IdentInformasjon(
                    ident = nyAktørId,
                    historisk = false,
                    gruppe = "AKTORID",
                ),
                IdentInformasjon(
                    ident = gammelAktør.aktivFødselsnummer(),
                    historisk = false,
                    gruppe = "FOLKEREGISTERIDENT",
                ),
            ),
        )

        // Act & Assert
        val feil =
            assertThrows<Feil> {
                patchMergetAktørTask.doTask(
                    Task(
                        payload =
                            objectMapper.writeValueAsString(
                                PatchMergetAktørDto(
                                    fagsakId = fagsak.id,
                                    gammelAktørId = gammelAktør.aktørId,
                                    nyAktørId = nyAktørId,
                                    skalSjekkeAtGammelAktørIdErHistoriskAvNyAktørId = true,
                                ),
                            ),
                        type = PatchMergetAktørTask.TASK_STEP_TYPE,
                    ),
                )
            }

        assertThat(feil.message).isEqualTo("AktørId som skal patches finnes ikke som historisk ident av ny ident")
    }

    @Test
    fun `kaster feil dersom man gir inn samme aktørId som ny og gammel`() {
        // Arrange
        val gammelAktør = aktørIdRepository.saveAndFlush(lagAktør())

        val fagsak = fagsakRepository.saveAndFlush(lagFagsakUtenId(aktør = gammelAktør))

        // Act & Assert
        val feil =
            assertThrows<IllegalArgumentException> {
                patchMergetAktørTask.doTask(
                    Task(
                        payload =
                            objectMapper.writeValueAsString(
                                PatchMergetAktørDto(
                                    fagsakId = fagsak.id,
                                    gammelAktørId = gammelAktør.aktørId,
                                    nyAktørId = gammelAktør.aktørId,
                                    skalSjekkeAtGammelAktørIdErHistoriskAvNyAktørId = true,
                                ),
                            ),
                        type = PatchMergetAktørTask.TASK_STEP_TYPE,
                    ),
                )
            }

        assertThat(feil.message).isEqualTo("id som skal patches er lik id som det skal patches til")
    }
}
