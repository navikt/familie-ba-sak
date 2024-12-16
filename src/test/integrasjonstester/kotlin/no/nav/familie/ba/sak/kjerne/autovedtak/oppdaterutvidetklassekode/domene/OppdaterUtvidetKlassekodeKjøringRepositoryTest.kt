package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode.domene

import no.nav.familie.ba.sak.common.lagFagsak
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class OppdaterUtvidetKlassekodeKjøringRepositoryTest(
    @Autowired private val oppdaterUtvidetKlassekodeKjøringRepository: OppdaterUtvidetKlassekodeKjøringRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val aktørIdRepository: AktørIdRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class FinnRelevanteOppdaterUtvidetKlassekodeKjøringer {
        @Test
        fun `skal finne de x antall første radene hvor brukerNyKlassekode er false og status er 'IKKE_UTFØRT'`() {
            // Arrange
            val oppdaterUtvidetKlassekodeKjøringer = mutableListOf<OppdaterUtvidetKlassekodeKjøring>()
            for (index in 1..20) {
                val aktør = aktørIdRepository.saveAndFlush(randomAktør())
                val fagsak = fagsakRepository.saveAndFlush(lagFagsak(aktør = aktør))
                if (index.mod(2) == 0) {
                    oppdaterUtvidetKlassekodeKjøringer.add(OppdaterUtvidetKlassekodeKjøring(fagsakId = fagsak.id))
                } else {
                    oppdaterUtvidetKlassekodeKjøringer.add(OppdaterUtvidetKlassekodeKjøring(fagsakId = fagsak.id, brukerNyKlassekode = true, status = Status.UTFØRT))
                }
            }
            oppdaterUtvidetKlassekodeKjøringRepository.saveAllAndFlush(oppdaterUtvidetKlassekodeKjøringer)

            // Act
            val relevanteOppdaterUtvidetKlassekodeKjøringer = oppdaterUtvidetKlassekodeKjøringRepository.finnRelevanteOppdaterUtvidetKlassekodeKjøringer(PageRequest.of(0, 20))

            // Assert
            assertThat(relevanteOppdaterUtvidetKlassekodeKjøringer).hasSize(10)
            assertThat(relevanteOppdaterUtvidetKlassekodeKjøringer.all { !it.brukerNyKlassekode && it.status == Status.IKKE_UTFØRT }).isTrue
        }
    }

    @Nested
    inner class SettBrukerNyKlassekodeTilTrueOgStatusTilUtført {
        @Test
        fun `skal sette brukerNyKlassekode til true og status til utført`() {
            // Arrange
            val aktør = aktørIdRepository.saveAndFlush(randomAktør())
            val fagsak = fagsakRepository.saveAndFlush(lagFagsak(aktør = aktør))
            oppdaterUtvidetKlassekodeKjøringRepository.saveAndFlush(OppdaterUtvidetKlassekodeKjøring(fagsakId = fagsak.id))

            // Act
            oppdaterUtvidetKlassekodeKjøringRepository.settBrukerNyKlassekodeTilTrueOgStatusTilUtført(fagsakId = fagsak.id)
            oppdaterUtvidetKlassekodeKjøringRepository.flush()

            // Assert
            val oppdaterUtvidetKlassekodeKjøring = oppdaterUtvidetKlassekodeKjøringRepository.findAll().single { it.fagsakId == fagsak.id }
            assertThat(oppdaterUtvidetKlassekodeKjøring.brukerNyKlassekode).isTrue
            assertThat(oppdaterUtvidetKlassekodeKjøring.status).isEqualTo(Status.UTFØRT)
        }
    }

    @Nested
    inner class OppdaterStatus {
        @ParameterizedTest
        @EnumSource(Status::class, names = ["IKKE_UTFØRT", "UTFØRES", "UTFØRT"], mode = EnumSource.Mode.INCLUDE)
        fun `skal oppdatere status`(status: Status) {
            // Arrange
            val aktør = aktørIdRepository.saveAndFlush(randomAktør())
            val fagsak = fagsakRepository.saveAndFlush(lagFagsak(aktør = aktør))
            oppdaterUtvidetKlassekodeKjøringRepository.saveAndFlush(OppdaterUtvidetKlassekodeKjøring(fagsakId = fagsak.id))

            // Act
            oppdaterUtvidetKlassekodeKjøringRepository.oppdaterStatus(fagsakId = fagsak.id, status = status)
            oppdaterUtvidetKlassekodeKjøringRepository.flush()

            // Assert
            val oppdaterUtvidetKlassekodeKjøring = oppdaterUtvidetKlassekodeKjøringRepository.findAll().single { it.fagsakId == fagsak.id }
            assertThat(oppdaterUtvidetKlassekodeKjøring.status).isEqualTo(status)
        }
    }
}
