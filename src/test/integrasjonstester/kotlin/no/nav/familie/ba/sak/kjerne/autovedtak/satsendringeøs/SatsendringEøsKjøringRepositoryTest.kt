package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class SatsendringEøsKjøringRepositoryTest(
    @param:Autowired private val satsendringEøsKjøringRepository: SatsendringEøsKjøringRepository,
    @param:Autowired private val fagsakRepository: FagsakRepository,
    @param:Autowired private val aktørIdRepository: AktørIdRepository,
    @param:Autowired private val behandlingRepository: BehandlingRepository,
) : AbstractSpringIntegrationTest() {
    private val satsTidspunkt = YearMonth.of(2025, 9)

    @Test
    fun `findByBehandlingId returnerer kjøring når behandlingId matcher`() {
        // Arrange
        val fagsak = opprettFagsak()
        val behandling = behandlingRepository.saveAndFlush(lagBehandlingUtenId(fagsak = fagsak))

        val lagretKjøring =
            satsendringEøsKjøringRepository.saveAndFlush(
                SatsendringEøsKjøring(
                    fagsakId = fagsak.id,
                    behandlingId = behandling.id,
                    utbetalingsland = "SE",
                    satsTidspunkt = satsTidspunkt,
                ),
            )

        // Act
        val funnet = satsendringEøsKjøringRepository.findByBehandlingId(behandling.id)

        // Assert
        assertThat(funnet).isEqualTo(lagretKjøring)
    }

    @Test
    fun `findByBehandlingId returnerer null når behandlingId ikke finnes`() {
        // Act
        val funnet = satsendringEøsKjøringRepository.findByBehandlingId(999L)

        // Assert
        assertThat(funnet).isNull()
    }

    @Test
    fun `findByFagsakIdAndUtbetalingslandAndSatsTidspunkt returnerer kjøring når alle felter matcher`() {
        // Arrange
        val fagsak = opprettFagsak()
        val lagretKjøring =
            satsendringEøsKjøringRepository.saveAndFlush(
                SatsendringEøsKjøring(
                    fagsakId = fagsak.id,
                    utbetalingsland = "DK",
                    satsTidspunkt = satsTidspunkt,
                ),
            )

        // Act
        val funnet =
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(
                fagsakId = fagsak.id,
                utbetalingsland = "DK",
                satsTidspunkt = satsTidspunkt,
            )

        // Assert
        assertThat(funnet).isEqualTo(lagretKjøring)
    }

    @Test
    fun `findByFagsakIdAndUtbetalingslandAndSatsTidspunkt returnerer null når utbetalingsland ikke matcher`() {
        // Arrange
        val fagsak = opprettFagsak()
        satsendringEøsKjøringRepository.saveAndFlush(
            SatsendringEøsKjøring(
                fagsakId = fagsak.id,
                utbetalingsland = "SE",
                satsTidspunkt = satsTidspunkt,
            ),
        )

        // Act
        val funnet =
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(
                fagsakId = fagsak.id,
                utbetalingsland = "DK",
                satsTidspunkt = satsTidspunkt,
            )

        // Assert
        assertThat(funnet).isNull()
    }

    @Test
    fun `findByFagsakIdAndUtbetalingslandAndSatsTidspunkt returnerer null når satsTidspunkt ikke matcher`() {
        // Arrange
        val fagsak = opprettFagsak()
        satsendringEøsKjøringRepository.saveAndFlush(
            SatsendringEøsKjøring(
                fagsakId = fagsak.id,
                utbetalingsland = "SE",
                satsTidspunkt = satsTidspunkt,
            ),
        )

        // Act
        val funnet =
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(
                fagsakId = fagsak.id,
                utbetalingsland = "SE",
                satsTidspunkt = satsTidspunkt.plusMonths(1),
            )

        // Assert
        assertThat(funnet).isNull()
    }

    @Test
    fun `findByFagsakIdAndUtbetalingslandAndSatsTidspunkt returnerer null når fagsakId ikke matcher`() {
        // Arrange
        val fagsak = opprettFagsak()
        satsendringEøsKjøringRepository.saveAndFlush(
            SatsendringEøsKjøring(
                fagsakId = fagsak.id,
                utbetalingsland = "SE",
                satsTidspunkt = satsTidspunkt,
            ),
        )

        val annenFagsak = opprettFagsak()

        // Act
        val funnet =
            satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(
                fagsakId = annenFagsak.id,
                utbetalingsland = "SE",
                satsTidspunkt = satsTidspunkt,
            )

        // Assert
        assertThat(funnet).isNull()
    }

    private fun opprettFagsak(): Fagsak {
        val aktør = lagAktør(randomFnr()).also { aktørIdRepository.saveAndFlush(it) }
        return lagFagsakUtenId(aktør = aktør).also { fagsakRepository.saveAndFlush(it) }
    }
}
