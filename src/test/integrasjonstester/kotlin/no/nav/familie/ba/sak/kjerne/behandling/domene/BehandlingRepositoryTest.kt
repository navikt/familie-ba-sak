package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus.UTREDES
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class BehandlingRepositoryTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentRepository: TilkjentYtelseRepository,
    @Autowired private val valutakursRepository: ValutakursRepository,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
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
    inner class FinnAlleFagsakerMedLøpendeValutakursIMåned {
        @Test
        fun `skal finne fagsaker med løpende valutakurs i gitt måned`() {
            // Arrange
            val måned = LocalDate.of(2025, 3, 1)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak1 =
                opprettFagsakMedBehandlingValutakursOgAndeler(
                    aktør = aktør,
                    andelFom = YearMonth.of(2025, 1),
                    andelTom = YearMonth.of(2025, 12),
                    valutakursFom = YearMonth.of(2025, 1),
                    valutakursTom = YearMonth.of(2025, 12),
                )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).contains(fagsak1.id)
        }

        @Test
        fun `skal bare se på siste iverksatte behandling per fagsak`() {
            // Arrange
            val måned = LocalDate.of(2025, 3, 1)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LØPENDE, arkivert = false))

            val gammelBehandling =
                behandlingRepository.save(
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        status = AVSLUTTET,
                        aktivertTid = LocalDateTime.now().minusDays(10),
                        aktiv = false,
                    ),
                )

            val gammelTilkjentYtelse =
                tilkjentRepository.save(
                    lagInitiellTilkjentYtelse(
                        behandling = gammelBehandling,
                        utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = gammelBehandling.id),
                    ),
                )

            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    aktør = aktør,
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 12),
                    behandling = gammelBehandling,
                    tilkjentYtelse = gammelTilkjentYtelse,
                ),
            )

            valutakursRepository.save(
                Valutakurs(
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 12),
                    barnAktører = setOf(aktør),
                    valutakursdato = LocalDate.of(2025, 1, 15),
                    valutakode = "EUR",
                    kurs = BigDecimal("10.5"),
                    vurderingsform = Vurderingsform.AUTOMATISK,
                ).apply { behandlingId = gammelBehandling.id },
            )

            val nyBehandling =
                behandlingRepository.save(
                    lagBehandlingUtenId(
                        fagsak = fagsak,
                        status = AVSLUTTET,
                        aktivertTid = LocalDateTime.now().minusDays(1),
                    ),
                )

            val nyTilkjentYtelse =
                tilkjentRepository.save(
                    lagInitiellTilkjentYtelse(
                        behandling = nyBehandling,
                        utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = nyBehandling.id),
                    ),
                )

            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    aktør = aktør,
                    fom = YearMonth.of(2025, 1),
                    tom = YearMonth.of(2025, 12),
                    behandling = nyBehandling,
                    tilkjentYtelse = nyTilkjentYtelse,
                ),
            )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal ekskludere fagsaker som ikke har valutakurs lenger`() {
            // Arrange
            val måned = LocalDate.of(2025, 6, 1)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak =
                opprettFagsakMedBehandlingValutakursOgAndeler(
                    aktør = aktør,
                    andelFom = YearMonth.of(2025, 1),
                    andelTom = YearMonth.of(2025, 12),
                    valutakursFom = YearMonth.of(2025, 1),
                    valutakursTom = YearMonth.of(2025, 3),
                )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal filtrere bort fagsaker som ikke er i status LØPENDE`() {
            // Arrange
            val måned = LocalDate.of(2025, 3, 1)
            val aktør = aktørIdRepository.save(randomAktør())

            opprettFagsakMedBehandlingValutakursOgAndeler(
                aktør = aktør,
                fagsakStatus = FagsakStatus.AVSLUTTET,
                andelFom = YearMonth.of(2025, 1),
                andelTom = YearMonth.of(2025, 12),
                valutakursFom = YearMonth.of(2025, 1),
                valutakursTom = YearMonth.of(2025, 12),
            )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `skal filtrere bort arkiverte fagsaker`() {
            // Arrange
            val måned = LocalDate.of(2025, 3, 1)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak =
                opprettFagsakMedBehandlingValutakursOgAndeler(
                    aktør = aktør,
                    arkivert = true,
                    andelFom = YearMonth.of(2025, 1),
                    andelTom = YearMonth.of(2025, 12),
                    valutakursFom = YearMonth.of(2025, 1),
                    valutakursTom = YearMonth.of(2025, 12),
                )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal filtrere bort behandlinger som ikke er i status AVSLUTTET`() {
            // Arrange
            val måned = LocalDate.of(2025, 3, 1)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak =
                opprettFagsakMedBehandlingValutakursOgAndeler(
                    aktør = aktør,
                    behandlingStatus = UTREDES,
                    andelFom = YearMonth.of(2025, 1),
                    andelTom = YearMonth.of(2025, 12),
                    valutakursFom = YearMonth.of(2025, 1),
                    valutakursTom = YearMonth.of(2025, 12),
                )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal filtrere bort behandlinger uten utbetalingsoppdrag`() {
            // Arrange
            val måned = LocalDate.of(2025, 3, 1)
            val aktør = aktørIdRepository.save(randomAktør())

            val fagsak =
                opprettFagsakMedBehandlingValutakursOgAndeler(
                    aktør = aktør,
                    medUtbetalingsoppdrag = false,
                    andelFom = YearMonth.of(2025, 1),
                    andelTom = YearMonth.of(2025, 12),
                    valutakursFom = YearMonth.of(2025, 1),
                    valutakursTom = YearMonth.of(2025, 12),
                )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).doesNotContain(fagsak.id)
        }

        @Test
        fun `skal filtrere bort fagsaker der tilkjent ytelse er utløpt før gitt måned`() {
            // Arrange
            val måned = LocalDate.of(2025, 6, 1)
            val aktør = aktørIdRepository.save(randomAktør())

            opprettFagsakMedBehandlingValutakursOgAndeler(
                aktør = aktør,
                andelFom = YearMonth.of(2025, 1),
                andelTom = YearMonth.of(2025, 3),
                valutakursFom = YearMonth.of(2025, 1),
                valutakursTom = YearMonth.of(2025, 12),
            )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).isEmpty()
        }

        @Test
        fun `skal finne flere fagsaker med løpende valutakurs`() {
            // Arrange
            val måned = LocalDate.of(2025, 3, 1)
            val aktør1 = aktørIdRepository.save(randomAktør())
            val aktør2 = aktørIdRepository.save(randomAktør())

            val fagsak1 =
                opprettFagsakMedBehandlingValutakursOgAndeler(
                    aktør = aktør1,
                    aktivertTid = LocalDateTime.now().minusDays(2),
                    andelFom = YearMonth.of(2025, 1),
                    andelTom = YearMonth.of(2025, 12),
                    valutakursFom = YearMonth.of(2025, 1),
                    valutakursTom = YearMonth.of(2025, 12),
                )

            val fagsak2 =
                opprettFagsakMedBehandlingValutakursOgAndeler(
                    aktør = aktør2,
                    aktivertTid = LocalDateTime.now().minusDays(1),
                    andelFom = YearMonth.of(2025, 1),
                    andelTom = YearMonth.of(2025, 12),
                    valutakursFom = YearMonth.of(2025, 1),
                    valutakursTom = null,
                    valutakode = "USD",
                    kurs = BigDecimal("11.0"),
                )

            // Act
            val result = behandlingRepository.finnAlleFagsakerMedLøpendeValutakursIMåned(måned)

            // Assert
            assertThat(result).contains(fagsak1.id, fagsak2.id)
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

    private fun opprettFagsakMedBehandlingValutakursOgAndeler(
        aktør: Aktør,
        fagsakStatus: FagsakStatus = FagsakStatus.LØPENDE,
        arkivert: Boolean = false,
        behandlingStatus: BehandlingStatus = AVSLUTTET,
        aktivertTid: LocalDateTime = LocalDateTime.now().minusDays(1),
        medUtbetalingsoppdrag: Boolean = true,
        andelFom: YearMonth? = null,
        andelTom: YearMonth? = null,
        valutakursFom: YearMonth? = null,
        valutakursTom: YearMonth? = null,
        valutakursdato: LocalDate = LocalDate.of(2025, 1, 15),
        valutakode: String = "EUR",
        kurs: BigDecimal = BigDecimal("10.5"),
    ): Fagsak {
        val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = fagsakStatus, arkivert = arkivert))
        val behandling =
            behandlingRepository.save(
                lagBehandlingUtenId(
                    fagsak = fagsak,
                    status = behandlingStatus,
                    aktivertTid = aktivertTid,
                ),
            )

        tilkjentRepository.save(
            lagTilkjentYtelse(
                behandling = behandling,
                stønadFom = andelFom,
                stønadTom = andelTom,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                utbetalingsoppdrag =
                    if (medUtbetalingsoppdrag) {
                        lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id)
                    } else {
                        null
                    },
                lagAndelerTilkjentYtelse = {
                    if (andelFom != null && andelTom != null) {
                        setOf(
                            lagAndelTilkjentYtelse(
                                aktør = aktør,
                                fom = andelFom,
                                tom = andelTom,
                                behandling = behandling,
                                tilkjentYtelse = it,
                            ),
                        )
                    } else {
                        emptySet()
                    }
                },
            ),
        )

        if (valutakursFom != null) {
            valutakursRepository.save(
                Valutakurs(
                    fom = valutakursFom,
                    tom = valutakursTom,
                    barnAktører = setOf(aktør),
                    valutakursdato = valutakursdato,
                    valutakode = valutakode,
                    kurs = kurs,
                    vurderingsform = Vurderingsform.AUTOMATISK,
                ).apply { behandlingId = behandling.id },
            )
        }

        return fagsak
    }
}
