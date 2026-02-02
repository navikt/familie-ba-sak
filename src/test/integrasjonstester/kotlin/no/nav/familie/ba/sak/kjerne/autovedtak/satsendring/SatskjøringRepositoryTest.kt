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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.YearMonth

class SatskjøringRepositoryTest(
    @Autowired private val satskjøringRepository: SatskjøringRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull henter satskjøring med riktig tidspunkt og som har feiltype`() {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        val satsTidspunkt = YearMonth.of(2025, 11)
        val satskjøring =
            satskjøringRepository.saveAndFlush(
                Satskjøring(
                    fagsakId = fagsakId,
                    satsTidspunkt = satsTidspunkt,
                    feiltype = "feiltype",
                ),
            )

        val satskjøringer =
            satskjøringRepository.findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(
                satsTidspunkt,
            )

        assertThat(satskjøringer).contains(satskjøring)
        assertThat(satskjøringer).allMatch { satskjøring ->
            @Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
            satskjøring.satsTidspunkt == satsTidspunkt &&
                satskjøring.feiltype != null &&
                satskjøring.ferdigTidspunkt == null
        }
    }

    @Test
    fun `findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull henter ikke satskjøring hvis satsTidspunkt ikke samsvarer`() {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        satskjøringRepository.saveAndFlush(
            Satskjøring(
                fagsakId = fagsakId,
                satsTidspunkt = YearMonth.of(2025, 11).minusMonths(1),
            ),
        )

        val satskjøringer =
            satskjøringRepository.findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(
                YearMonth.of(2025, 11),
            )

        assertThat(satskjøringer).hasSize(0)
    }

    @Test
    fun `findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull henter ikke satskjøring hvor ferdigTidspunkt ikke er null`() {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        val satskjøring =
            satskjøringRepository.saveAndFlush(
                Satskjøring(
                    fagsakId = fagsakId,
                    satsTidspunkt = YearMonth.of(2025, 11),
                    ferdigTidspunkt = LocalDateTime.now(),
                    feiltype = "feiltype",
                ),
            )

        val satskjøringer =
            satskjøringRepository.findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(
                YearMonth.of(2025, 11),
            )

        assertThat(satskjøringer).allMatch { it.ferdigTidspunkt == null }
        assertThat(satskjøringer).doesNotContain(satskjøring)
    }

    @Test
    fun `findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull henter ikke satskjøring hvis feiltype er null`() {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        val satskjøring =
            satskjøringRepository.saveAndFlush(
                Satskjøring(
                    fagsakId = fagsakId,
                    satsTidspunkt = YearMonth.of(2025, 11),
                ),
            )

        val satskjøringer =
            satskjøringRepository.findBySatsTidspunktAndFerdigTidspunktIsNullAndFeiltypeIsNotNull(
                YearMonth.of(2025, 11),
            )

        assertThat(satskjøringer).doesNotContain(satskjøring)
        assertThat(satskjøringer).allMatch { it.feiltype != null }
    }

    @Test
    fun `finnPåFeilTypeOgFerdigTidNull finner ikke satskjøringer med ferdigTid != Null`() {
        // Arrange
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        val fagsakId = lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }.id

        satskjøringRepository.saveAndFlush(
            Satskjøring(
                fagsakId = fagsakId,
                satsTidspunkt = YearMonth.of(2025, 11),
                feiltype = "feiltype",
                ferdigTidspunkt = LocalDateTime.now(),
            ),
        )

        // Act
        val satskjøringer =
            satskjøringRepository.finnPåFeilTypeOgFerdigTidNull(
                feiltype = "feiltype",
                satsTidspunkt = YearMonth.of(2025, 11),
            )

        // Assert
        assertThat(satskjøringer.map { it.fagsakId }).doesNotContain(fagsakId)
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
                    satsTidspunkt = YearMonth.of(2025, 11),
                    feiltype = "feiltype",
                    ferdigTidspunkt = null,
                ),
                Satskjøring(
                    fagsakId = fagsakId2,
                    satsTidspunkt = YearMonth.of(2025, 11),
                    feiltype = "feiltype",
                    ferdigTidspunkt = LocalDateTime.now(),
                ),
            ),
        )

        // Act
        val satskjøringer =
            satskjøringRepository.finnPåFeilTypeOgFerdigTidNull(
                feiltype = "feiltype",
                satsTidspunkt = YearMonth.of(2025, 11),
            )

        // Assert
        assertThat(satskjøringer).hasSize(1)
        assertThat(satskjøringer.single().ferdigTidspunkt).isNull()
        assertThat(satskjøringer.single().fagsakId).isEqualTo(fagsakId)
    }
}
