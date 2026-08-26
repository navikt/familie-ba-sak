package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class FagsakRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) : AbstractSpringIntegrationTest() {
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
            assertThat(fagsakerSomSkalAvsluttes).contains(fagsak.id)
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
            assertThat(fagsakerSomSkalAvsluttes).doesNotContain(fagsak.id)
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
            assertThat(fagsakerSomSkalAvsluttes).contains(fagsak.id)
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
            assertThat(fagsakerSomSkalAvsluttes).doesNotContain(fagsak.id)
        }
    }

    @Nested
    inner class FinnAvsluttedeFagsakerSomSkalLåses {
        @Test
        fun `skal returnere fagsak når yngste barn fylte 18 år for mer enn 1 år siden`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val behandling = opprettBehandling(fagsak = fagsak)
            lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(19).minusDays(1)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).contains(fagsak.id)
        }

        @Test
        fun `skal returnere fagsak når yngste barn fylte 18 år for nøyaktig 1 år siden`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val behandling = opprettBehandling(fagsak = fagsak)
            lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(19)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).contains(fagsak.id)
        }

        @Test
        fun `skal ikke returnere fagsak når yngste barn fylte 18 år for under 1 år siden`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val behandling = opprettBehandling(fagsak = fagsak)
            lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(19).plusDays(1)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal bruke yngste barn når fagsaken har flere barn`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val behandling = opprettBehandling(fagsak = fagsak)
            lagrePersonopplysningGrunnlag(
                behandling = behandling,
                barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25), LocalDate.now().minusYears(10)),
            )

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal ikke returnere fagsak som ikke er avsluttet`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.LØPENDE)
            val behandling = opprettBehandling(fagsak = fagsak)
            lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal ikke returnere arkivert fagsak`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET, arkivert = true)
            val behandling = opprettBehandling(fagsak = fagsak)
            lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal ikke returnere fagsak når siste behandling ikke er avsluttet`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val behandling = opprettBehandling(fagsak = fagsak, behandlingStatus = BehandlingStatus.UTREDES)
            lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal ignorere henlagte behandlinger`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val behandling = opprettBehandling(fagsak = fagsak, behandlingResultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET)
            lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal bruke barna i siste vedtatte behandling når fagsaken har flere behandlinger`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val eldsteBehandling = opprettBehandling(fagsak = fagsak, aktivertTid = LocalDateTime.now().minusDays(2), aktiv = false)
            lagrePersonopplysningGrunnlag(behandling = eldsteBehandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25)))
            val sisteBehandling = opprettBehandling(fagsak = fagsak, aktivertTid = LocalDateTime.now().minusDays(1), aktiv = true)
            lagrePersonopplysningGrunnlag(behandling = sisteBehandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(10)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal returnere fagsaken kun én gang selv om fagsaken har flere behandlinger`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val eldsteBehandling = opprettBehandling(fagsak = fagsak, aktivertTid = LocalDateTime.now().minusDays(2), aktiv = false)
            lagrePersonopplysningGrunnlag(behandling = eldsteBehandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25)))
            val sisteBehandling = opprettBehandling(fagsak = fagsak, aktivertTid = LocalDateTime.now().minusDays(1), aktiv = true)
            lagrePersonopplysningGrunnlag(behandling = sisteBehandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25)))

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).containsOnlyOnce(fagsak.id)
        }

        @Test
        fun `skal ikke returnere fagsak uten barn`() {
            // Arrange
            val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
            val behandling = opprettBehandling(fagsak = fagsak)
            lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = emptyList())

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 100)

            // Assert
            assertThat(fagsakerSomSkalLåses).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal ikke returnere flere fagsaker enn maksAntall`() {
            // Arrange
            repeat(3) {
                val fagsak = opprettFagsak(fagsakStatus = FagsakStatus.AVSLUTTET)
                val behandling = opprettBehandling(fagsak = fagsak)
                lagrePersonopplysningGrunnlag(behandling = behandling, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(25)))
            }

            // Act
            val fagsakerSomSkalLåses = fagsakRepository.finnAvsluttedeFagsakerSomSkalLåses(maksAntall = 2)

            // Assert
            assertThat(fagsakerSomSkalLåses).hasSize(2)
        }
    }

    private fun opprettFagsak(
        fagsakStatus: FagsakStatus,
        arkivert: Boolean = false,
    ): Fagsak {
        val søker = aktørIdRepository.save(randomAktør())
        return fagsakRepository.save(lagFagsakUtenId(aktør = søker, status = fagsakStatus, arkivert = arkivert))
    }

    private fun opprettBehandling(
        fagsak: Fagsak,
        behandlingStatus: BehandlingStatus = BehandlingStatus.AVSLUTTET,
        behandlingResultat: Behandlingsresultat = Behandlingsresultat.INNVILGET,
        aktivertTid: LocalDateTime = LocalDateTime.now(),
        aktiv: Boolean = true,
    ): Behandling =
        behandlingRepository.save(
            lagBehandlingUtenId(
                fagsak = fagsak,
                status = behandlingStatus,
                resultat = behandlingResultat,
                aktivertTid = aktivertTid,
                aktiv = aktiv,
            ),
        )

    private fun lagrePersonopplysningGrunnlag(
        behandling: Behandling,
        barnasFødselsdatoer: List<LocalDate>,
    ) {
        val barnAktører = barnasFødselsdatoer.map { aktørIdRepository.save(randomAktør()) }
        personopplysningGrunnlagRepository.save(
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                barnasIdenter = barnAktører.map { it.aktivFødselsnummer() },
                barnasFødselsdatoer = barnasFødselsdatoer,
                søkerAktør = behandling.fagsak.aktør,
                barnAktør = barnAktører,
            ),
        )
    }
}
