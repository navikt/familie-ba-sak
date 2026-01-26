package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.YearMonth

class SatskjøringRepositoryTest(
    @Autowired private val satskjøringRepository: SatskjøringRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setUp() {
        satskjøringRepository.deleteAll()
        fagsakRepository.deleteAll()
        aktørIdRepository.deleteAll()
    }

    @Test
    fun `findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull henter satskjøring med riktig tidspunkt og som har feiltype`() {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        satskjøringRepository.saveAndFlush(
            Satskjøring(
                fagsakId = fagsakId,
                satsTidspunkt = YearMonth.now(),
                feiltype = "feiltype",
            ),
        )

        val satskjøringer =
            satskjøringRepository.findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(
                YearMonth.now(),
            )

        assertThat(satskjøringer).hasSize(1).extracting("fagsakId").containsExactly(fagsakId)
    }

    @Test
    fun `findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull henter ikke satskjøring hvis satsTidspunkt ikke samsvarer`() {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        satskjøringRepository.saveAndFlush(
            Satskjøring(
                fagsakId = fagsakId,
                satsTidspunkt = YearMonth.now().minusMonths(1),
            ),
        )

        val satskjøringer =
            satskjøringRepository.findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(
                YearMonth.now(),
            )

        assertThat(satskjøringer).hasSize(0)
    }

    @Test
    fun `findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull henter ikke satskjøring hvor ferdigTidspunkt ikke er null`() {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        satskjøringRepository.saveAndFlush(
            Satskjøring(
                fagsakId = fagsakId,
                satsTidspunkt = YearMonth.now(),
                ferdigTidspunkt = LocalDateTime.now(),
                feiltype = "feiltype",
            ),
        )

        val satskjøringer =
            satskjøringRepository.findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(
                YearMonth.now(),
            )

        assertThat(satskjøringer).hasSize(0)
    }

    @Test
    fun `findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull henter ikke satskjøring hvis feiltype er null`() {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        satskjøringRepository.saveAndFlush(
            Satskjøring(
                fagsakId = fagsakId,
                satsTidspunkt = YearMonth.now(),
            ),
        )

        val satskjøringer =
            satskjøringRepository.findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(
                YearMonth.now(),
            )

        assertThat(satskjøringer).hasSize(0)
    }

    @Test
    fun `finnPåFeilTypeOgFerdigTidNull finner ikke satskjøringer med ferdigTid != Null`() {
        // Arrange
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        satskjøringRepository.saveAndFlush(
            Satskjøring(
                fagsakId = fagsakId,
                satsTidspunkt = YearMonth.now(),
                feiltype = "feiltype",
                ferdigTidspunkt = LocalDateTime.now(),
            ),
        )

        // Act
        val satskjøringer =
            satskjøringRepository.finnPåFeilTypeOgFerdigTidNull(
                feiltype = "feiltype",
                satsTidspunkt = YearMonth.now(),
            )

        // Assert
        assertThat(satskjøringer).isEmpty()
    }

    @Test
    fun `finnPåFeilTypeOgFerdigTidNull finner kun satskjøringer med ferdigTidNull`() {
        // Arrange
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        val aktør2 = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId2 = lagFagsakUtenId(aktør = aktør2).also { fagsakRepository.saveAndFlush(it) }.id

        satskjøringRepository.saveAllAndFlush(
            listOf(
                Satskjøring(
                    fagsakId = fagsakId,
                    satsTidspunkt = YearMonth.now(),
                    feiltype = "feiltype",
                    ferdigTidspunkt = null,
                ),
                Satskjøring(
                    fagsakId = fagsakId2,
                    satsTidspunkt = YearMonth.now(),
                    feiltype = "feiltype",
                    ferdigTidspunkt = LocalDateTime.now(),
                ),
            ),
        )

        // Act
        val satskjøringer =
            satskjøringRepository.finnPåFeilTypeOgFerdigTidNull(
                feiltype = "feiltype",
                satsTidspunkt = YearMonth.now(),
            )

        // Assert
        assertThat(satskjøringer).hasSize(1)
        assertThat(satskjøringer.single().ferdigTidspunkt).isNull()
        assertThat(satskjøringer.single().fagsakId).isEqualTo(fagsakId)
    }
}
