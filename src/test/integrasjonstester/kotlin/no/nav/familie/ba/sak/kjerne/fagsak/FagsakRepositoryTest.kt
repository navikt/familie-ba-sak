package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktivering
import no.nav.familie.ba.sak.kjerne.minside.MinsideAktiveringRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.skjermetbarnsøker.SkjermetBarnSøkerRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class FagsakRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val skjermetBarnSøkerRepository: SkjermetBarnSøkerRepository,
    @Autowired private val databaseCleanupService: DatabaseCleanupService,
    @Autowired private val minsideAktiveringRepository: MinsideAktiveringRepository,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun beforeEach() {
        databaseCleanupService.truncate()
    }

    @Nested
    inner class FinnFagsakerSomSkalAvsluttes {
        @Test
        fun `skal finne fagsak med behandling som er avsluttet, som ikke ikke avslått eller henlagt og som ikke lenger har løpende andeler`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LØPENDE))

            val behandlinger =
                listOf(
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                        resultat = Behandlingsresultat.INNVILGET,
                        status = BehandlingStatus.AVSLUTTET,
                        aktivertTid = LocalDateTime.of(2025, 2, 11, 0, 0, 0),
                        aktiv = false,
                    ),
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        behandlingType = BehandlingType.REVURDERING,
                        resultat = Behandlingsresultat.AVSLÅTT,
                        status = BehandlingStatus.AVSLUTTET,
                        aktivertTid = LocalDateTime.of(2025, 3, 11, 0, 0, 0),
                        aktiv = false,
                    ),
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        behandlingType = BehandlingType.REVURDERING,
                        resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                        status = BehandlingStatus.AVSLUTTET,
                        aktivertTid = LocalDateTime.of(2025, 4, 11, 0, 0, 0),
                        aktiv = false,
                    ),
                )

            behandlinger.forEach { behandling ->
                val lagretBehandling = behandlingRepository.saveAndFlush(behandling)
                tilkjentYtelseRepository.save(
                    lagTilkjentYtelse(
                        behandling = lagretBehandling,
                        stønadTom = YearMonth.of(2025, 1),
                    ) { emptySet() },
                )
            }

            // Act
            val fagsakerSomSkalAvsluttes = fagsakRepository.finnFagsakerSomSkalAvsluttes()

            // Assert
            assertThat(fagsakerSomSkalAvsluttes).hasSize(1)
            assertThat(fagsakerSomSkalAvsluttes.first()).isEqualTo(fagsak.id)
        }

        @ParameterizedTest
        @EnumSource(
            Behandlingsresultat::class,
            names = ["HENLAGT_FEILAKTIG_OPPRETTET", "HENLAGT_SØKNAD_TRUKKET", "HENLAGT_AUTOMATISK_FØDSELSHENDELSE", "HENLAGT_AUTOMATISK_SMÅBARNSTILLEGG", "HENLAGT_TEKNISK_VEDLIKEHOLD"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal ikke finne fagsak dersom alle avsluttede behandlinger er enten avslått eller henlagt`(
            behandlingsresultat: Behandlingsresultat,
        ) {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LØPENDE))

            val behandlinger =
                listOf(
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        behandlingType = BehandlingType.REVURDERING,
                        resultat = Behandlingsresultat.AVSLÅTT,
                        status = BehandlingStatus.AVSLUTTET,
                        aktivertTid = LocalDateTime.of(2025, 2, 11, 0, 0, 0),
                        aktiv = false,
                    ),
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        behandlingType = BehandlingType.REVURDERING,
                        resultat = Behandlingsresultat.AVSLÅTT,
                        status = BehandlingStatus.AVSLUTTET,
                        aktivertTid = LocalDateTime.of(2025, 4, 11, 0, 0, 0),
                        årsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                        aktiv = false,
                    ),
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        behandlingType = BehandlingType.REVURDERING,
                        resultat = behandlingsresultat,
                        status = BehandlingStatus.AVSLUTTET,
                        aktivertTid = LocalDateTime.of(2025, 3, 11, 0, 0, 0),
                        aktiv = false,
                    ),
                )

            behandlinger.forEach { behandling ->
                val lagretBehandling = behandlingRepository.saveAndFlush(behandling)
                tilkjentYtelseRepository.save(
                    lagTilkjentYtelse(
                        behandling = lagretBehandling,
                        stønadTom = YearMonth.of(2025, 1),
                    ) { emptySet() },
                )
            }

            // Act
            val fagsakerSomSkalAvsluttes = fagsakRepository.finnFagsakerSomSkalAvsluttes()

            // Assert
            assertThat(fagsakerSomSkalAvsluttes).hasSize(0)
        }
    }

    @Test
    fun `skal finne fagsak dersom stønad_tom er senere enn inneværende måned men det kun finnes andeler tilkjent ytelse med prosent satt til 0`() {
        // Arrange
        val aktør = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LØPENDE))
        val behandling =
            behandlingRepository.save(
                lagBehandlingUtenId(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    resultat = Behandlingsresultat.INNVILGET,
                    status = BehandlingStatus.AVSLUTTET,
                    aktivertTid = LocalDateTime.of(2025, 2, 11, 0, 0, 0),
                    aktiv = false,
                ),
            )
        val tilkjentYtelse =
            tilkjentYtelseRepository.save(
                lagTilkjentYtelse(
                    behandling = behandling,
                    stønadTom = YearMonth.now().plusMonths(2),
                ) { emptySet() },
            )
        andelTilkjentYtelseRepository.save(
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = aktør,
                fom = YearMonth.now().minusMonths(1),
                tom = YearMonth.now().plusMonths(2),
                prosent = BigDecimal.valueOf(0),
            ),
        )

        // Act
        val fagsakerSomSkalAvsluttes = fagsakRepository.finnFagsakerSomSkalAvsluttes()

        // Assert
        assertThat(fagsakerSomSkalAvsluttes).hasSize(1)
        assertThat(fagsakerSomSkalAvsluttes.first()).isEqualTo(fagsak.id)
    }

    @Test
    fun `skal ikke finne fagsak dersom stønad_tom er senere enn inneværende måned og det finnes andeler tilkjent ytelse med prosent satt til noe annet enn 0`() {
        // Arrange
        val aktør = aktørIdRepository.save(randomAktør())
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LØPENDE))
        val behandling =
            behandlingRepository.save(
                lagBehandlingUtenId(
                    fagsak = fagsak,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    resultat = Behandlingsresultat.INNVILGET,
                    status = BehandlingStatus.AVSLUTTET,
                    aktivertTid = LocalDateTime.of(2025, 2, 11, 0, 0, 0),
                    aktiv = false,
                ),
            )
        val tilkjentYtelse =
            tilkjentYtelseRepository.save(
                lagTilkjentYtelse(
                    behandling = behandling,
                    stønadTom = YearMonth.now().plusMonths(2),
                ) { emptySet() },
            )
        andelTilkjentYtelseRepository.save(
            lagAndelTilkjentYtelse(
                tilkjentYtelse = tilkjentYtelse,
                behandling = behandling,
                aktør = aktør,
                fom = YearMonth.now().minusMonths(1),
                tom = YearMonth.now().plusMonths(2),
                prosent = BigDecimal.valueOf(100),
            ),
        )

        // Act
        val fagsakerSomSkalAvsluttes = fagsakRepository.finnFagsakerSomSkalAvsluttes()

        // Assert
        assertThat(fagsakerSomSkalAvsluttes).hasSize(0)
    }

    @Nested
    inner class FinnLøpendeFagsakSomIkkeHarFåttMinsideAktivertTest {
        @ParameterizedTest
        @EnumSource(FagsakType::class, names = ["BARN_ENSLIG_MINDREÅRIG", "NORMAL"], mode = EnumSource.Mode.INCLUDE)
        fun `Skal returnere løpende fagsaker av type enslig mindreårig og normal som ikke har fått minside aktivert`(fagsakType: FagsakType) {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LØPENDE, type = fagsakType))

            // Act
            val fagsaker = fagsakRepository.finnLøpendeFagsakSomIkkeHarFåttMinsideAktivert(PageRequest.of(0, 100)).content

            // Assert
            assertThat(fagsaker.size).isEqualTo(1)
            assertThat(fagsaker.single()).isEqualTo(fagsak)
        }

        @ParameterizedTest
        @EnumSource(FagsakType::class, names = ["INSTITUSJON", "SKJERMET_BARN"], mode = EnumSource.Mode.INCLUDE)
        fun `Skal ikke returnere løpende fagsaker som ikke har fått minside aktivert men som er av INSTITUSJON eller SKJERMET_BARN`(fagsakType: FagsakType) {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LØPENDE, type = fagsakType))

            // Act
            val fagsaker = fagsakRepository.finnLøpendeFagsakSomIkkeHarFåttMinsideAktivert(PageRequest.of(0, 100)).content

            // Assert
            assertThat(fagsaker).isEmpty()
        }

        @ParameterizedTest
        @EnumSource(FagsakType::class, names = ["BARN_ENSLIG_MINDREÅRIG", "NORMAL"], mode = EnumSource.Mode.INCLUDE)
        fun `Skal ikke returnere løpende fagsaker av type enslig mindreårig og normal der aktør har fått minside aktivert`(fagsakType: FagsakType) {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LØPENDE, type = fagsakType))

            minsideAktiveringRepository.saveAndFlush(
                MinsideAktivering(
                    aktør = aktør,
                ),
            )

            // Act
            val fagsaker = fagsakRepository.finnLøpendeFagsakSomIkkeHarFåttMinsideAktivert(PageRequest.of(0, 100)).content

            // Assert
            assertThat(fagsaker).isEmpty()
        }
    }
}
