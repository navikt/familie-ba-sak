package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDate

class AutovedtakFinnmarkstilleggTaskOppretterTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val opprettTaskService = mockk<OpprettTaskService>()
    private val finnmarkstilleggKjøringRepository = mockk<FinnmarkstilleggKjøringRepository>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val pdlRestKlient = mockk<SystemOnlyPdlRestKlient>()

    private val autovedtakFinnmarkstilleggTaskOppretter =
        AutovedtakFinnmarkstilleggTaskOppretter(
            fagsakRepository = fagsakRepository,
            opprettTaskService = opprettTaskService,
            finnmarkstilleggKjøringService = FinnmarkstilleggKjøringService(finnmarkstilleggKjøringRepository),
            persongrunnlagService = persongrunnlagService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            pdlRestKlient = pdlRestKlient,
        )

    private val søker1 = lagPerson()
    private val søker2 = lagPerson()

    private val behandling1 = lagBehandling(id = 1, fagsak = lagFagsak(id = 1))
    private val behandling2 = lagBehandling(id = 2, fagsak = lagFagsak(id = 2))

    private val eøsBehandling1 = lagBehandling(id = 1, fagsak = lagFagsak(id = 1), behandlingKategori = BehandlingKategori.EØS)
    private val eøsBehandling2 = lagBehandling(id = 2, fagsak = lagFagsak(id = 2), behandlingKategori = BehandlingKategori.EØS)

    private val persongrunnlag1 =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling1.id,
            lagPersoner = { setOf(søker1) },
        )

    private val persongrunnlag2 =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling2.id,
            lagPersoner = { setOf(søker2) },
        )

    private val bostedsadresseIFinnmark =
        PdlAdresserPerson(
            bostedsadresse =
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                        vegadresse =
                            Vegadresse(
                                matrikkelId = null,
                                husnummer = null,
                                husbokstav = null,
                                bruksenhetsnummer = null,
                                adressenavn = null,
                                kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer,
                                tilleggsnavn = null,
                                postnummer = null,
                            ),
                    ),
                ),
            deltBosted = emptyList(),
        )

    private val bostedsadresseIOslo =
        PdlAdresserPerson(
            bostedsadresse =
                listOf(
                    Bostedsadresse(
                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                        vegadresse =
                            Vegadresse(
                                matrikkelId = null,
                                husnummer = null,
                                husbokstav = null,
                                bruksenhetsnummer = null,
                                adressenavn = null,
                                kommunenummer = "0301",
                                tilleggsnavn = null,
                                postnummer = null,
                            ),
                    ),
                ),
            deltBosted = emptyList(),
        )

    @BeforeEach
    fun setup() {
        every { finnmarkstilleggKjøringRepository.saveAll(any<List<FinnmarkstilleggKjøring>>()) } returnsArgument 0
        every { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(any()) } returns mockk()
    }

    @Nested
    inner class OpprettTasker {
        @Test
        fun `skal opprette tasks for fagsaker med personer i Finnmark eller Nord-Troms`() {
            // Arrange
            every {
                fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any())
            } returns PageImpl(listOf(behandling1.fagsak.id, behandling2.fagsak.id), Pageable.ofSize(1000), 2)

            every {
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(
                    setOf(
                        behandling1.fagsak.id,
                        behandling2.fagsak.id,
                    ),
                )
            } returns
                mapOf(
                    behandling1.fagsak.id to behandling1,
                    behandling2.fagsak.id to behandling2,
                )

            every {
                persongrunnlagService.hentAktivForBehandlinger(
                    listOf(
                        behandling1.id,
                        behandling2.id,
                    ),
                )
            } returns
                mapOf(
                    behandling1.id to persongrunnlag1,
                    behandling2.id to persongrunnlag2,
                )

            every {
                pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(
                    listOf(
                        søker1.aktør.aktivFødselsnummer(),
                        søker2.aktør.aktivFødselsnummer(),
                    ),
                )
            } returns
                mapOf(
                    søker1.aktør.aktivFødselsnummer() to bostedsadresseIFinnmark,
                    søker2.aktør.aktivFødselsnummer() to bostedsadresseIOslo,
                )

            // Act
            autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(1000)

            // Assert
            verify(exactly = 1) { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(behandling1.fagsak.id) }
            verify(exactly = 1) { finnmarkstilleggKjøringRepository.saveAll(listOf(FinnmarkstilleggKjøring(fagsakId = behandling2.fagsak.id))) }
        }

        @Test
        fun `skal ikke opprette tasks for fagsaker som har siste iverksatte behandling med kategori EØS `() {
            // Arrange
            every {
                fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any())
            } returns PageImpl(listOf(eøsBehandling1.fagsak.id, eøsBehandling2.fagsak.id), Pageable.ofSize(1000), 2)

            every {
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(
                    setOf(
                        eøsBehandling1.fagsak.id,
                        eøsBehandling2.fagsak.id,
                    ),
                )
            } returns
                mapOf(
                    eøsBehandling1.fagsak.id to eøsBehandling1,
                    eøsBehandling2.fagsak.id to eøsBehandling2,
                )

            every {
                persongrunnlagService.hentAktivForBehandlinger(emptyList())
            } returns emptyMap()

            every {
                pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(
                    listOf(
                        søker1.aktør.aktivFødselsnummer(),
                        søker2.aktør.aktivFødselsnummer(),
                    ),
                )
            } returns
                mapOf(
                    søker1.aktør.aktivFødselsnummer() to bostedsadresseIFinnmark,
                    søker2.aktør.aktivFødselsnummer() to bostedsadresseIOslo,
                )

            // Act
            autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(1000)

            // Assert
            verify(exactly = 0) { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(any()) }
            verify(exactly = 1) { finnmarkstilleggKjøringRepository.saveAll(listOf(FinnmarkstilleggKjøring(fagsakId = eøsBehandling1.fagsak.id), FinnmarkstilleggKjøring(fagsakId = eøsBehandling2.fagsak.id))) }
        }

        @Test
        fun `skal håndtere fagsaker uten iverksatt behandling`() {
            // Arrange
            every {
                fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any())
            } returns PageImpl(listOf(behandling1.fagsak.id), Pageable.ofSize(1000), 1)

            every {
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(setOf(behandling1.fagsak.id))
            } returns emptyMap()

            every {
                persongrunnlagService.hentAktivForBehandlinger(emptyList())
            } returns emptyMap()

            // Act
            autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(1000)

            // Assert
            verify(exactly = 0) { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) }
            verify(exactly = 0) { opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(any()) }
        }

        @Test
        fun `skal håndtere behandling uten persongrunnlag`() {
            val logger = LoggerFactory.getLogger(AutovedtakFinnmarkstilleggTaskOppretter::class.java) as Logger
            val listAppender = ListAppender<ILoggingEvent>().apply { start() }
            logger.addAppender(listAppender)

            // Arrange
            every {
                fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any())
            } returns PageImpl(listOf(behandling1.fagsak.id), Pageable.ofSize(1000), 1)

            every {
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(setOf(behandling1.fagsak.id))
            } returns mapOf(behandling1.fagsak.id to behandling1)

            every {
                persongrunnlagService.hentAktivForBehandlinger(listOf(behandling1.id))
            } returns emptyMap()

            // Act
            autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(1000)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("ERROR")
                assertThat(it.formattedMessage).isEqualTo("Forventet personopplysningsgrunnlag for behandling ${behandling1.id} ikke funnet.")
            }
        }

        @Test
        fun `skal sende spørringer til PDL på maks 1000 personer`() {
            // Arrange
            val fagsakIder = (0L until 1000).toSet()

            every {
                fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(any())
            } returns PageImpl(fagsakIder.toList(), Pageable.ofSize(1000), fagsakIder.size.toLong())

            val behandlinger = fagsakIder.map { lagBehandling(id = it, fagsak = lagFagsak(id = it)) }

            val grunnlag =
                behandlinger.map {
                    val personer = List(5) { lagPerson() }.toTypedArray().toSet()
                    lagPersonopplysningGrunnlag(behandlingId = it.id, lagPersoner = { personer })
                }

            every {
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder)
            } returns behandlinger.associate { it.fagsak.id to it }

            every {
                persongrunnlagService.hentAktivForBehandlinger(behandlinger.map { it.id })
            } returns grunnlag.associate { it.behandlingId to it }

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                assertThat(firstArg<List<String>>()).hasSize(1000)
                emptyMap()
            }

            // Act
            autovedtakFinnmarkstilleggTaskOppretter.opprettTasker(1000)

            // Assert
            verify(exactly = 5) { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) }
        }
    }
}
