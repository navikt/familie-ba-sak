package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.domene.SvalbardtilleggKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.domene.SvalbardtilleggKjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDate

class AutovedtakSvalbardtilleggTaskOppretterTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val opprettTaskService = mockk<OpprettTaskService>()
    private val svalbardtilleggKjøringRepository = mockk<SvalbardtilleggKjøringRepository>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val pdlRestClient = mockk<SystemOnlyPdlRestClient>()

    private val autovedtakSvalbardtilleggTaskOppretter =
        AutovedtakSvalbardtilleggTaskOppretter(
            fagsakRepository = fagsakRepository,
            opprettTaskService = opprettTaskService,
            svalbardtilleggKjøringRepository = svalbardtilleggKjøringRepository,
            persongrunnlagService = persongrunnlagService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            pdlRestClient = pdlRestClient,
        )

    @Nested
    inner class OpprettTasker {
        @BeforeEach
        fun setup() {
            every { svalbardtilleggKjøringRepository.saveAll(any<List<SvalbardtilleggKjøring>>()) } returnsArgument 0
        }

        @Test
        fun `skal ikke opprette tasker når ingen løpende fagsaker for kjøring blir funnet`() {
            // Arrange
            val pageable = Pageable.ofSize(2)
            val fagsakIder = emptySet<Long>()

            every { fagsakRepository.finnLøpendeFagsakerForSvalbardtilleggKjøring(any()) } returns
                PageImpl(
                    fagsakIder.toList(),
                    pageable,
                    1,
                )

            every {
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder)
            } returns emptyMap()

            every {
                persongrunnlagService.hentAktivForBehandlinger(emptyList())
            } returns emptyMap()

            every {
                opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(emptySet())
            } just runs

            // Act
            autovedtakSvalbardtilleggTaskOppretter.opprettTasker(2)

            // Assert
            verify(exactly = 1) { svalbardtilleggKjøringRepository.saveAll(emptyList()) }
            verify(exactly = 1) { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder) }
            verify(exactly = 1) { persongrunnlagService.hentAktivForBehandlinger(any()) }
            verify(exactly = 0) { pdlRestClient.hentAdresserForPersoner(any()) }
            verify(exactly = 1) { opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(emptySet()) }
        }

        @Test
        fun `skal opprette tasker for løpende fagsaker som ikke alt er kjørt`() {
            // Arrange
            val pageable = Pageable.ofSize(2)

            val person1 = lagPerson()
            val person2 = lagPerson()

            val fagsak1 = lagFagsak(id = 1, aktør = person1.aktør)
            val behandling1 = lagBehandling(id = 1, fagsak = fagsak1)
            val persongrunnlag1 = lagTestPersonopplysningGrunnlag(behandling1.id, person1)

            val fagsak2 = lagFagsak(id = 2, aktør = person2.aktør)
            val behandling2 = lagBehandling(id = 2, fagsak = fagsak2)
            val persongrunnlag2 = lagTestPersonopplysningGrunnlag(behandling2.id, person2)

            val fagsakIder = setOf(fagsak1.id, fagsak2.id)

            every { fagsakRepository.finnLøpendeFagsakerForSvalbardtilleggKjøring(any()) } returns
                PageImpl(
                    fagsakIder.toList(),
                    pageable,
                    1,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder) } returns
                mapOf(
                    fagsak1.id to behandling1,
                    fagsak2.id to behandling2,
                )

            every {
                persongrunnlagService.hentAktivForBehandlinger(
                    listOf(
                        behandling1.id,
                        behandling2.id,
                    ),
                )
            } returns mapOf(behandling1.id to persongrunnlag1, behandling2.id to persongrunnlag2)

            every { pdlRestClient.hentAdresserForPersoner(any()) } returns
                mapOf(
                    person1.aktør.aktivFødselsnummer() to
                        PdlAdresserPerson(
                            oppholdsadresse =
                                listOf(
                                    lagOppholdsadresse(
                                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                                        gyldigTilOgMed = LocalDate.of(2025, 12, 31),
                                        vegadresse = lagVegadresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                                    ),
                                ),
                        ),
                    person2.aktør.aktivFødselsnummer() to
                        PdlAdresserPerson(
                            oppholdsadresse =
                                listOf(
                                    lagOppholdsadresse(
                                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                                        gyldigTilOgMed = LocalDate.of(2025, 12, 31),
                                        vegadresse = lagVegadresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                                    ),
                                ),
                        ),
                )

            every { opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(fagsakIder) } just runs

            // Act
            autovedtakSvalbardtilleggTaskOppretter.opprettTasker(2)

            // Assert
            verify(exactly = 1) { svalbardtilleggKjøringRepository.saveAll(any<List<SvalbardtilleggKjøring>>()) }
            verify(exactly = 1) { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder) }
            verify(exactly = 1) { persongrunnlagService.hentAktivForBehandlinger(listOf(behandling1.id, behandling2.id)) }
            verify(exactly = 1) { pdlRestClient.hentAdresserForPersoner(any()) }
            verify(exactly = 1) { opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(fagsakIder) }
        }

        @Test
        fun `skal filtrere bort fagsak som ikke har en iverksatt behandling`() {
            // Arrange
            val pageable = Pageable.ofSize(2)

            val person1 = lagPerson()
            val person2 = lagPerson()

            val fagsak1 = lagFagsak(id = 1L, aktør = person1.aktør)
            val behandling1 = lagBehandling(id = 1L, fagsak = fagsak1)
            val persongrunnlag1 = lagTestPersonopplysningGrunnlag(behandling1.id, person1)

            val fagsak2 = lagFagsak(id = 2, aktør = person2.aktør)

            val fagsakIder = setOf(fagsak1.id, fagsak2.id)

            every { fagsakRepository.finnLøpendeFagsakerForSvalbardtilleggKjøring(any()) } returns
                PageImpl(
                    fagsakIder.toList(),
                    pageable,
                    1,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder) } returns mapOf(fagsak1.id to behandling1)

            every { persongrunnlagService.hentAktivForBehandlinger(listOf(behandling1.id)) } returns mapOf(behandling1.id to persongrunnlag1)

            every { pdlRestClient.hentAdresserForPersoner(any()) } returns
                mapOf(
                    person1.aktør.aktivFødselsnummer() to
                        PdlAdresserPerson(
                            oppholdsadresse =
                                listOf(
                                    lagOppholdsadresse(
                                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                                        gyldigTilOgMed = LocalDate.of(2025, 12, 31),
                                        vegadresse = lagVegadresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                                    ),
                                ),
                        ),
                    person2.aktør.aktivFødselsnummer() to
                        PdlAdresserPerson(
                            oppholdsadresse =
                                listOf(
                                    lagOppholdsadresse(
                                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                                        gyldigTilOgMed = LocalDate.of(2025, 12, 31),
                                        vegadresse = lagVegadresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                                    ),
                                ),
                        ),
                )

            every { opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(setOf(fagsak1.id)) } just runs

            // Act
            autovedtakSvalbardtilleggTaskOppretter.opprettTasker(2)

            // Assert
            verify(exactly = 1) { svalbardtilleggKjøringRepository.saveAll(any<List<SvalbardtilleggKjøring>>()) }
            verify(exactly = 1) { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder) }
            verify(exactly = 1) { persongrunnlagService.hentAktivForBehandlinger(listOf(behandling1.id)) }
            verify(exactly = 1) { pdlRestClient.hentAdresserForPersoner(any()) }
            verify(exactly = 1) { opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(setOf(fagsak1.id)) }
        }

        @Test
        fun `skal kaste exception om aktivt grunnlag ikke blir funnet for en behandling`() {
            // Arrange
            val pageable = Pageable.ofSize(2)

            val person1 = lagPerson()
            val person2 = lagPerson()

            val fagsak1 = lagFagsak(id = 1, aktør = person1.aktør)
            val behandling1 = lagBehandling(id = 1, fagsak = fagsak1)
            val persongrunnlag1 = lagTestPersonopplysningGrunnlag(behandling1.id, person1)

            val fagsak2 = lagFagsak(id = 2, aktør = person2.aktør)
            val behandling2 = lagBehandling(id = 2, fagsak = fagsak2)

            val fagsakIder = setOf(fagsak1.id, fagsak2.id)

            every { fagsakRepository.finnLøpendeFagsakerForSvalbardtilleggKjøring(any()) } returns
                PageImpl(
                    fagsakIder.toList(),
                    pageable,
                    1,
                )

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder) } returns mapOf(fagsak1.id to behandling1, fagsak2.id to behandling2)

            every { persongrunnlagService.hentAktivForBehandlinger(listOf(behandling1.id, behandling2.id)) } returns mapOf(behandling1.id to persongrunnlag1)

            every { pdlRestClient.hentAdresserForPersoner(any()) } returns
                mapOf(
                    person1.aktør.aktivFødselsnummer() to
                        PdlAdresserPerson(
                            oppholdsadresse =
                                listOf(
                                    lagOppholdsadresse(
                                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                                        gyldigTilOgMed = LocalDate.of(2025, 12, 31),
                                        vegadresse = lagVegadresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                                    ),
                                ),
                        ),
                    person2.aktør.aktivFødselsnummer() to
                        PdlAdresserPerson(
                            oppholdsadresse =
                                listOf(
                                    lagOppholdsadresse(
                                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                                        gyldigTilOgMed = LocalDate.of(2025, 12, 31),
                                        vegadresse = lagVegadresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                                    ),
                                ),
                        ),
                )

            every { opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(any()) } just runs

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    autovedtakSvalbardtilleggTaskOppretter.opprettTasker(2)
                }
            assertThat(exception).hasMessageContaining("Forventet personopplysningsgrunnlag for behandling 2 ikke funnet")
        }
    }
}
