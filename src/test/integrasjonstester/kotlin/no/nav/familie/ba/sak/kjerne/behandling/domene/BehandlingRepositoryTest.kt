package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.UTREDES
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BehandlingRepositoryTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentRepository: TilkjentYtelseRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class FinnSisteIverksatteBehandling {
        val tilfeldigPerson = tilfeldigPerson()
        val tilfeldigPerson2 = tilfeldigPerson()
        lateinit var fagsak: Fagsak
        lateinit var fagsak2: Fagsak

        @BeforeEach
        fun setUp() {
            fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson.aktør.aktivFødselsnummer())
            fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson2.aktør.aktivFødselsnummer())
        }

        @Test
        fun `skal finne siste iverksatte behandlingen som har utbetalingsoppdrag, som er avsluttet`() {
            opprettBehandling(fagsak, AVSLUTTET, LocalDateTime.now().minusDays(3))
                .medTilkjentYtelse(true)
            val behandling2 =
                opprettBehandling(fagsak, AVSLUTTET, LocalDateTime.now().minusDays(2))
                    .medTilkjentYtelse(true)
            opprettBehandling(fagsak, IVERKSETTER_VEDTAK, LocalDateTime.now().minusDays(1))
                .medTilkjentYtelse(true)

            val behandling4 =
                opprettBehandling(fagsak2, AVSLUTTET, LocalDateTime.now())
                    .medTilkjentYtelse(true)

            assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)!!).isEqualTo(behandling2)
            assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak2.id)!!).isEqualTo(behandling4)
        }

        @Test
        fun `skal finne siste iverksatte behandlingen som har utbetalingsoppdrag`() {
            opprettBehandling(fagsak, AVSLUTTET, LocalDateTime.now().minusDays(3))
                .medTilkjentYtelse(true)
            val behandling3 =
                opprettBehandling(fagsak, AVSLUTTET, LocalDateTime.now().minusDays(1))
                    .medTilkjentYtelse(true)

            opprettBehandling(fagsak, AVSLUTTET).medTilkjentYtelse()
            opprettBehandling(fagsak, IVERKSETTER_VEDTAK).medTilkjentYtelse()

            assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)!!).isEqualTo(behandling3)
        }
    }

    @Nested
    inner class FinnSisteIverksatteBehandlingForFagsaker {
        @Test
        fun `skal finne siste iverksatte behandlingener`() {
            // Arrange
            val dagensDato = LocalDateTime.of(2025, 10, 16, 12, 0, 0)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak1 = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, type = FagsakType.NORMAL))
            val behandling1 = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak1, status = AVSLUTTET, aktivertTid = dagensDato.minusSeconds(1), aktiv = true))
            tilkjentRepository.save(lagInitiellTilkjentYtelse(behandling = behandling1, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling1.id)))
            val behandling2 = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak1, status = AVSLUTTET, aktivertTid = dagensDato.minusSeconds(2), aktiv = false))
            tilkjentRepository.save(lagInitiellTilkjentYtelse(behandling = behandling2, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling2.id)))

            val fagsak2 = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, type = FagsakType.BARN_ENSLIG_MINDREÅRIG))
            val behandling3 = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak2, status = AVSLUTTET, aktivertTid = dagensDato.minusSeconds(1), aktiv = true))
            tilkjentRepository.save(lagInitiellTilkjentYtelse(behandling = behandling3, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling3.id)))
            val behandling4 = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak2, status = AVSLUTTET, aktivertTid = dagensDato.minusSeconds(2), aktiv = false))
            tilkjentRepository.save(lagInitiellTilkjentYtelse(behandling = behandling4, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling4.id)))

            val fagsaker = setOf(fagsak1.id, fagsak2.id)

            // Act
            val behandlinger = behandlingRepository.finnSisteIverksatteBehandlingForFagsaker(fagsaker)

            // Assert
            assertThat(behandlinger).anySatisfy { assertThat(it.id).isEqualTo(behandling1.id) }
            assertThat(behandlinger).anySatisfy { assertThat(it.id).isEqualTo(behandling3.id) }
            assertThat(behandlinger).noneSatisfy { assertThat(it.id).isEqualTo(behandling2.id) }
            assertThat(behandlinger).noneSatisfy { assertThat(it.id).isEqualTo(behandling4.id) }
        }

        @Test
        fun `skal filtrer bort behandling som ikke er avsluttet`() {
            // Arrange
            val dagensDato = LocalDateTime.of(2025, 10, 16, 12, 0, 0)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, type = FagsakType.NORMAL))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak, status = UTREDES, aktivertTid = dagensDato, aktiv = true))
            tilkjentRepository.save(lagInitiellTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id)))

            val fagsaker = setOf(fagsak.id)

            // Act
            val behandlinger = behandlingRepository.finnSisteIverksatteBehandlingForFagsaker(fagsaker)

            // Assert
            assertThat(behandlinger).noneSatisfy { assertThat(it.id).isEqualTo(behandling.id) }
            assertThat(behandlinger).noneSatisfy { assertThat(it.fagsak.id).isEqualTo(fagsak.id) }
        }

        @Test
        fun `skal filtrer bort behandlinger på fagsaker som er arkivert`() {
            // Arrange
            val dagensDato = LocalDateTime.of(2025, 10, 16, 12, 0, 0)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, type = FagsakType.NORMAL, arkivert = true))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak, status = AVSLUTTET, aktivertTid = dagensDato, aktiv = true))
            tilkjentRepository.save(lagInitiellTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id)))

            val fagsaker = setOf(fagsak.id)

            // Act
            val behandlinger = behandlingRepository.finnSisteIverksatteBehandlingForFagsaker(fagsaker)

            // Assert
            assertThat(behandlinger).noneSatisfy { assertThat(it.id).isEqualTo(behandling.id) }
            assertThat(behandlinger).noneSatisfy { assertThat(it.fagsak.id).isEqualTo(fagsak.id) }
        }

        @Test
        fun `skal filtrer bort behandlinger som ikke har utbetalingsoppdrag`() {
            // Arrange
            val dagensDato = LocalDateTime.of(2025, 10, 16, 12, 0, 0)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, type = FagsakType.NORMAL, arkivert = false))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak, status = AVSLUTTET, aktivertTid = dagensDato, aktiv = true))

            val fagsaker = setOf(fagsak.id)

            // Act
            val behandlinger = behandlingRepository.finnSisteIverksatteBehandlingForFagsaker(fagsaker)

            // Assert
            assertThat(behandlinger).noneSatisfy { assertThat(it.id).isEqualTo(behandling.id) }
            assertThat(behandlinger).noneSatisfy { assertThat(it.fagsak.id).isEqualTo(fagsak.id) }
        }
    }

    @Nested
    inner class FinnFagsakIderForBehandlinger {
        @Test
        fun `skal finne fagsakIder relatert til behandlingsIder`() {
            val fagsak1 = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson().aktør.aktivFødselsnummer())
            val fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(tilfeldigPerson().aktør.aktivFødselsnummer())

            // Arrange
            val behandling1 = opprettBehandling(fagsak1, AVSLUTTET)
            val behandling2 = opprettBehandling(fagsak2, AVSLUTTET)

            // Act
            val fagsakIder = behandlingRepository.finnFagsakIderForBehandlinger(listOf(behandling1.id, behandling2.id))

            // Assert
            assertThat(fagsakIder).hasSize(2)
            assertThat(fagsakIder).containsExactlyInAnyOrder(fagsak1.id, fagsak2.id)
        }
    }

    private fun opprettBehandling(
        fagsak: Fagsak,
        behandlingStatus: BehandlingStatus,
        aktivertTidspunkt: LocalDateTime = LocalDateTime.now(),
        aktiv: Boolean = false,
    ): Behandling {
        val behandling =
            lagBehandlingUtenId(fagsak = fagsak, status = behandlingStatus)
                .copy(
                    id = 0,
                    aktiv = aktiv,
                    aktivertTidspunkt = aktivertTidspunkt,
                )
        val oppdaterteSteg = behandling.behandlingStegTilstand.map { it.copy(behandling = behandling) }
        behandling.behandlingStegTilstand.clear()
        behandling.behandlingStegTilstand.addAll(oppdaterteSteg)
        return behandlingRepository.saveAndFlush(behandling).let {
            behandlingRepository.finnBehandling(it.id)
        }
    }

    private fun Behandling.medTilkjentYtelse(medUtbetalingsoppdrag: Boolean = false) =
        this.also {
            val tilkjentYtelse =
                lagInitiellTilkjentYtelse(
                    behandling = it,
                    utbetalingsoppdrag =
                        if (medUtbetalingsoppdrag) {
                            lagMinimalUtbetalingsoppdragString(behandlingId = it.id)
                        } else {
                            null
                        },
                )
            tilkjentRepository.saveAndFlush(tilkjentYtelse)
        }
}
