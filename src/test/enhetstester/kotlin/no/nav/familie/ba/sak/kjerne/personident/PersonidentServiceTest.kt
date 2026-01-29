package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FolkeregisteridentifikatorStatus
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FolkeregisteridentifikatorType
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlFolkeregisteridentifikator
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonidentServiceTest {
    private val personidentAleredePersistert = randomFnr()
    private val aktørIdAleredePersistert = lagAktør(personidentAleredePersistert)
    private val personidentAktiv = randomFnr()
    private val aktørIdAktiv = lagAktør(personidentAktiv)
    private val personidentHistorisk = randomFnr()

    private val pdlIdentRestKlient: PdlIdentRestKlient = mockk(relaxed = true)
    private val personidentRepository: PersonidentRepository = mockk()
    private val aktørIdRepository: AktørIdRepository = mockk()
    private val personIdentSlot = slot<Personident>()
    private val aktørSlot = slot<Aktør>()
    private val taskRepositoryMock = mockk<TaskRepositoryWrapper>(relaxed = true)

    private val personidentService =
        PersonidentService(
            personidentRepository = personidentRepository,
            aktørIdRepository = aktørIdRepository,
            pdlIdentRestKlient = pdlIdentRestKlient,
            taskRepository = taskRepositoryMock,
        )

    @BeforeAll
    fun init() {
        every { pdlIdentRestKlient.hentIdenter(personidentAktiv, false) } answers {
            listOf(
                IdentInformasjon(aktørIdAktiv.aktørId, false, "AKTORID"),
                IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
            )
        }
        every { pdlIdentRestKlient.hentIdenter(personidentHistorisk, false) } answers {
            listOf(
                IdentInformasjon(aktørIdAktiv.aktørId, false, "AKTORID"),
                IdentInformasjon(personidentAktiv, false, "FOLKEREGISTERIDENT"),
            )
        }
    }

    @BeforeEach
    fun byggRepositoryMocks() {
        clearMocks(answers = true, firstMock = aktørIdRepository)
        clearMocks(answers = true, firstMock = personidentRepository)
        clearMocks(answers = true, firstMock = taskRepositoryMock)

        every { personidentRepository.saveAndFlush(capture(personIdentSlot)) } answers {
            personIdentSlot.captured
        }

        every { aktørIdRepository.saveAndFlush(capture(aktørSlot)) } answers {
            aktørSlot.captured
        }
    }

    @Nested
    inner class HentAktørTest {
        @Test
        fun `Test aktør id som som er persistert fra før`() {
            every { personidentRepository.findByFødselsnummerOrNull(aktørIdAleredePersistert.aktørId) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdAleredePersistert.aktørId) } answers { aktørIdAleredePersistert }

            val aktør = personidentService.hentAktør(aktørIdAleredePersistert.aktørId)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAleredePersistert.aktørId, aktør.aktørId)
            assertEquals(personidentAleredePersistert, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test personident som er persistert fra før`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentAleredePersistert) } answers { aktørIdAleredePersistert.personidenter.first() }

            val aktør = personidentService.hentAktør(personidentAleredePersistert)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAleredePersistert.aktørId, aktør.aktørId)
            assertEquals(personidentAleredePersistert, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test aktiv personident som er persistert fra før`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(personidentAktiv) } answers { null }

            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers {
                Personident(
                    personidentAktiv,
                    aktørIdAktiv,
                )
            }

            val aktør = personidentService.hentOgLagreAktør(personidentAktiv, false)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAktiv.aktørId, aktør.aktørId)
            assertEquals(personidentAktiv, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test aktør id som som er persistert fra før men aktiv personident som ikke er persistert`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentHistorisk) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(personidentHistorisk) } answers { null }
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdAktiv.aktørId) } answers { aktørIdAktiv }

            val aktør = personidentService.hentOgLagreAktør(personidentHistorisk, true)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAktiv.aktørId, aktør.aktørId)
            assertEquals(personidentAktiv, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test hverken aktør id eller aktiv personident som er persistert fra før`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(personidentAktiv) } answers { null }
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdAktiv.aktørId) } answers { null }

            val aktør = personidentService.hentOgLagreAktør(personidentAktiv, true)

            verify(exactly = 1) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAktiv.aktørId, aktør.aktørId)
            assertEquals(personidentAktiv, aktør.personidenter.single().fødselsnummer)
        }

        @Test
        fun `Test hverken aktør id eller aktiv personident som er persistert fra før men som ikke skal persisteres`() {
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }
            every { aktørIdRepository.findByAktørIdOrNull(personidentAktiv) } answers { null }
            every { personidentRepository.findByFødselsnummerOrNull(personidentAktiv) } answers { null }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdAktiv.aktørId) } answers { null }

            val aktør = personidentService.hentOgLagreAktør(personidentAktiv, false)

            verify(exactly = 0) { aktørIdRepository.saveAndFlush(any()) }
            verify(exactly = 0) { personidentRepository.saveAndFlush(any()) }
            assertEquals(aktørIdAktiv.aktørId, aktør.aktørId)
            assertEquals(personidentAktiv, aktør.personidenter.single().fødselsnummer)
        }
    }

    @Nested
    inner class OpprettTaskForIdentHendelseTest {
        @Test
        fun `Skal opprette task for håndtering av ny ident ved ny fnr men samme aktør`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val aktørIdSomFinnes = lagAktør(personIdentSomFinnes)
            aktørIdSomFinnes.personidenter.add(
                Personident(
                    fødselsnummer = personIdentSomFinnes,
                    aktør = aktørIdSomFinnes,
                ),
            )

            every { pdlIdentRestKlient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlIdentRestKlient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(personIdentSomFinnes, true, "FOLKEREGISTERIDENT"),
                )
            }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdSomFinnes.aktørId) }.answers {
                aktørIdSomFinnes
            }

            val slot = slot<Task>()
            every { taskRepositoryMock.save(capture(slot)) } answers { slot.captured }

            val ident = PersonIdent(personIdentSomSkalLeggesTil)
            personidentService.opprettTaskForIdentHendelse(ident)

            verify(exactly = 1) { taskRepositoryMock.save(any()) }
            assertEquals(ident, objectMapper.readValue(slot.captured.payload, PersonIdent::class.java))
        }

        @Test
        fun `Skal opprette task for håndtering av ny ident ved ny fnr og ny aktør`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val aktørIdGammel = lagAktør(personIdentSomFinnes)
            val aktørIdNy = lagAktør(personIdentSomSkalLeggesTil)
            aktørIdGammel.personidenter.add(
                Personident(
                    fødselsnummer = personIdentSomFinnes,
                    aktør = aktørIdGammel,
                ),
            )

            every { pdlIdentRestKlient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdGammel.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlIdentRestKlient.hentIdenter(personIdentSomSkalLeggesTil, true) } answers {
                listOf(
                    IdentInformasjon(aktørIdGammel.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(aktørIdNy.aktørId, true, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, true, "FOLKEREGISTERIDENT"),
                )
            }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdGammel.aktørId) }.answers {
                aktørIdGammel
            }

            every { aktørIdRepository.findByAktørIdOrNull(aktørIdNy.aktørId) } returns null

            val slot = slot<Task>()
            every { taskRepositoryMock.save(capture(slot)) } answers { slot.captured }

            val ident = PersonIdent(personIdentSomSkalLeggesTil)
            personidentService.opprettTaskForIdentHendelse(ident)

            verify(exactly = 1) { taskRepositoryMock.save(any()) }
            assertEquals(ident, objectMapper.readValue(slot.captured.payload, PersonIdent::class.java))
        }

        @Test
        fun `Skal ikke opprette task for håndtering av ny ident når ident ikke er tilknyttet noen aktører i systemet`() {
            val personIdentSomFinnes = randomFnr()
            val personIdentSomSkalLeggesTil = randomFnr()
            val aktørIdIkkeIBaSak = lagAktør(personIdentSomSkalLeggesTil)
            val aktørIdSomFinnes = lagAktør(personIdentSomFinnes)
            aktørIdSomFinnes.personidenter.add(
                Personident(
                    fødselsnummer = personIdentSomFinnes,
                    aktør = aktørIdSomFinnes,
                ),
            )

            every { pdlIdentRestKlient.hentIdenter(personIdentSomFinnes, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdSomFinnes.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomFinnes, false, "FOLKEREGISTERIDENT"),
                )
            }

            every { pdlIdentRestKlient.hentIdenter(personIdentSomSkalLeggesTil, false) } answers {
                listOf(
                    IdentInformasjon(aktørIdIkkeIBaSak.aktørId, false, "AKTORID"),
                    IdentInformasjon(personIdentSomSkalLeggesTil, false, "FOLKEREGISTERIDENT"),
                )
            }
            every { aktørIdRepository.findByAktørIdOrNull(aktørIdIkkeIBaSak.aktørId) }.answers {
                aktørIdIkkeIBaSak
            }

            val slot = slot<Task>()
            every { taskRepositoryMock.save(capture(slot)) } answers { slot.captured }

            val ident = PersonIdent(personIdentSomSkalLeggesTil)
            personidentService.opprettTaskForIdentHendelse(ident)

            verify(exactly = 0) { taskRepositoryMock.save(any()) }
        }
    }

    @Nested
    inner class LagreHistoriskeIdenterTest {
        @Test
        fun `Skal lagre flere nye historiske identer`() {
            // Arrange
            val aktivIdent = randomFnr()
            val historiskIdent1 = randomFnr()
            val historiskIdent2 = randomFnr()
            val historiskIdent3 = randomFnr()

            val aktør = lagAktør(aktivIdent)
            aktør.personidenter.add(
                Personident(
                    fødselsnummer = aktivIdent,
                    aktør = aktør,
                    aktiv = true,
                ),
            )

            every { personidentRepository.findByFødselsnummerOrNull(aktivIdent) } returns
                aktør.personidenter.first { it.aktiv }

            val pdlIdenter =
                listOf(
                    PdlFolkeregisteridentifikator(
                        historiskIdent1,
                        FolkeregisteridentifikatorStatus.OPPHOERT,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                    PdlFolkeregisteridentifikator(
                        historiskIdent2,
                        FolkeregisteridentifikatorStatus.OPPHOERT,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                    PdlFolkeregisteridentifikator(
                        historiskIdent3,
                        FolkeregisteridentifikatorStatus.OPPHOERT,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                )

            // Act
            personidentService.lagreHistoriskeIdenter(aktivIdent, pdlIdenter)

            // Assert
            verify(exactly = 1) { aktørIdRepository.saveAndFlush(any()) }
            val lagretAktør = aktørSlot.captured
            assertEquals(4, lagretAktør.personidenter.size) // 1 aktiv + 3 historiske
            assertEquals(1, lagretAktør.personidenter.count { it.aktiv })
            assertEquals(3, lagretAktør.personidenter.count { !it.aktiv })

            val historiskeFnr = lagretAktør.personidenter.filter { !it.aktiv }.map { it.fødselsnummer }
            assert(historiskeFnr.contains(historiskIdent1))
            assert(historiskeFnr.contains(historiskIdent2))
            assert(historiskeFnr.contains(historiskIdent3))
        }

        @Test
        fun `Skal ikke lagre historiske identer som allerede finnes`() {
            // Arrange
            val aktivIdent = randomFnr()
            val eksisterendeHistoriskIdent = randomFnr()
            val nyHistoriskIdent = randomFnr()
            val aktør = lagAktør(aktivIdent)

            aktør.personidenter.add(
                Personident(
                    fødselsnummer = aktivIdent,
                    aktør = aktør,
                    aktiv = true,
                ),
            )

            aktør.personidenter.add(
                Personident(
                    fødselsnummer = eksisterendeHistoriskIdent,
                    aktør = aktør,
                    aktiv = false,
                ),
            )

            every { personidentRepository.findByFødselsnummerOrNull(aktivIdent) } returns
                aktør.personidenter.first { it.aktiv }

            val pdlIdenter =
                listOf(
                    PdlFolkeregisteridentifikator(
                        eksisterendeHistoriskIdent,
                        FolkeregisteridentifikatorStatus.OPPHOERT,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                    PdlFolkeregisteridentifikator(
                        nyHistoriskIdent,
                        FolkeregisteridentifikatorStatus.OPPHOERT,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                )

            // Act
            personidentService.lagreHistoriskeIdenter(aktivIdent, pdlIdenter)

            // Assert
            verify(exactly = 1) { aktørIdRepository.saveAndFlush(any()) }
            val lagretAktør = aktørSlot.captured

            assertEquals(3, lagretAktør.personidenter.size)

            val historiskeFnr = lagretAktør.personidenter.filter { !it.aktiv }.map { it.fødselsnummer }
            assert(historiskeFnr.contains(eksisterendeHistoriskIdent))
            assert(historiskeFnr.contains(nyHistoriskIdent))

            assertEquals(1, lagretAktør.personidenter.count { it.fødselsnummer == eksisterendeHistoriskIdent })
        }

        @Test
        fun `Skal ikke lagre aktive identer fra PDL`() {
            // Arrange
            val aktivIdent = randomFnr()
            val nyAktivIdentFraPDL = randomFnr()
            val historiskIdent = randomFnr()
            val aktør = lagAktør(aktivIdent)

            aktør.personidenter.add(
                Personident(
                    fødselsnummer = aktivIdent,
                    aktør = aktør,
                    aktiv = true,
                ),
            )

            every { personidentRepository.findByFødselsnummerOrNull(aktivIdent) } returns
                aktør.personidenter.first { it.aktiv }

            val pdlIdenter =
                listOf(
                    PdlFolkeregisteridentifikator(
                        nyAktivIdentFraPDL,
                        FolkeregisteridentifikatorStatus.I_BRUK,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                    PdlFolkeregisteridentifikator(
                        historiskIdent,
                        FolkeregisteridentifikatorStatus.OPPHOERT,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                )

            // Act
            personidentService.lagreHistoriskeIdenter(aktivIdent, pdlIdenter)

            // Assert
            verify(exactly = 1) { aktørIdRepository.saveAndFlush(any()) }
            val lagretAktør = aktørSlot.captured

            assertEquals(2, lagretAktør.personidenter.size)
            assertEquals(1, lagretAktør.personidenter.count { it.aktiv })

            assert(!lagretAktør.personidenter.any { it.fødselsnummer == nyAktivIdentFraPDL })
            assert(lagretAktør.personidenter.any { it.fødselsnummer == historiskIdent && !it.aktiv })

            val aktivPersonident = lagretAktør.personidenter.find { it.aktiv }
            assertEquals(aktivIdent, aktivPersonident?.fødselsnummer)
        }

        @Test
        fun `Skal filtrere bort identer med null identifikasjonsnummer`() {
            // Arrange
            val aktivIdent = randomFnr()
            val gyldigHistoriskIdent = randomFnr()
            val aktør = lagAktør(aktivIdent)

            aktør.personidenter.add(
                Personident(
                    fødselsnummer = aktivIdent,
                    aktør = aktør,
                    aktiv = true,
                ),
            )

            every { personidentRepository.findByFødselsnummerOrNull(aktivIdent) } returns
                aktør.personidenter.first { it.aktiv }

            val pdlIdenter =
                listOf(
                    PdlFolkeregisteridentifikator(
                        null,
                        FolkeregisteridentifikatorStatus.OPPHOERT,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                    PdlFolkeregisteridentifikator(
                        gyldigHistoriskIdent,
                        FolkeregisteridentifikatorStatus.OPPHOERT,
                        FolkeregisteridentifikatorType.FNR,
                    ),
                )

            // Act
            personidentService.lagreHistoriskeIdenter(aktivIdent, pdlIdenter)

            // Assert
            verify(exactly = 1) { aktørIdRepository.saveAndFlush(any()) }
            val lagretAktør = aktørSlot.captured

            assertEquals(2, lagretAktør.personidenter.size)
            assert(lagretAktør.personidenter.any { it.fødselsnummer == gyldigHistoriskIdent })
        }
    }
}
