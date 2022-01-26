package no.nav.familie.ba.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.isMockKMock
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ba.sak.config.ClientMocks.Companion.BARN_DET_IKKE_GIS_TILGANG_TIL_FNR
import no.nav.familie.ba.sak.config.ClientMocks.Companion.søkerFnr
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggResponse
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@TestConfiguration
class IntegrasjonClientMock {

    @Bean
    @Primary
    fun mockIntegrasjonClient(): IntegrasjonClient {
        val mockIntegrasjonClient = mockk<IntegrasjonClient>(relaxed = false)

        clearIntegrasjonMocks(mockIntegrasjonClient)

        return mockIntegrasjonClient
    }

    companion object {
        fun clearIntegrasjonMocks(mockIntegrasjonClient: IntegrasjonClient) {
            /**
             * Mulig årsak til at appen må bruke dirties i testene.
             * Denne bønna blir initialisert av mockk, men etter noen av testene
             * er det ikke lenger en mockk bønne!
             */
            if (isMockKMock(mockIntegrasjonClient)) {
                clearMocks(mockIntegrasjonClient)
            }

            every {
                mockIntegrasjonClient.hentMaskertPersonInfoVedManglendeTilgang(any())
            } returns null

            every { mockIntegrasjonClient.hentJournalpost(any()) } answers {
                success(
                    lagTestJournalpost(
                        søkerFnr[0],
                        UUID.randomUUID().toString()
                    )
                )
            }

            every { mockIntegrasjonClient.hentJournalposterForBruker(any()) } answers {
                success(
                    listOf(
                        lagTestJournalpost(
                            søkerFnr[0],
                            UUID.randomUUID().toString()
                        ),
                        lagTestJournalpost(
                            søkerFnr[0],
                            UUID.randomUUID().toString()
                        )
                    )
                )
            }

            every { mockIntegrasjonClient.finnOppgaveMedId(any()) } returns
                lagTestOppgaveDTO(1L)

            every { mockIntegrasjonClient.hentOppgaver(any()) } returns
                FinnOppgaveResponseDto(
                    2,
                    listOf(lagTestOppgaveDTO(1L), lagTestOppgaveDTO(2L, Oppgavetype.BehandleSak, "Z999999"))
                )

            every { mockIntegrasjonClient.opprettOppgave(any()) } returns
                "12345678"

            every { mockIntegrasjonClient.patchOppgave(any()) } returns
                OppgaveResponse(12345678L)

            every { mockIntegrasjonClient.fordelOppgave(any(), any()) } returns
                "12345678"

            every { mockIntegrasjonClient.oppdaterJournalpost(any(), any()) } returns
                OppdaterJournalpostResponse("1234567")

            every { mockIntegrasjonClient.journalførVedtaksbrev(any(), any(), any(), any()) } returns "journalpostId"

            every {
                mockIntegrasjonClient.journalførManueltBrev(any(), any(), any(), any(), any(), any())
            } returns "journalpostId"

            every {
                mockIntegrasjonClient.lagJournalpostForBrev(any(), any(), any(), any(), any(), any(), null)
            } returns "journalpostId"

            every {
                mockIntegrasjonClient.leggTilLogiskVedlegg(any(), any())
            } returns LogiskVedleggResponse(12345678)

            every {
                mockIntegrasjonClient.slettLogiskVedlegg(any(), any())
            } returns LogiskVedleggResponse(12345678)

            every { mockIntegrasjonClient.distribuerBrev(any()) } returns success("bestillingsId")

            every { mockIntegrasjonClient.ferdigstillJournalpost(any(), any()) } just runs

            every { mockIntegrasjonClient.ferdigstillOppgave(any()) } just runs

            every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns
                listOf(Arbeidsfordelingsenhet("4833", "NAV Familie- og pensjonsytelser Oslo 1"))

            every { mockIntegrasjonClient.hentDokument(any(), any()) } returns
                success(TEST_PDF)

            val idSlotPersonMedRelasjoner = slot<String>()
            every {
                mockIntegrasjonClient.sjekkTilgangTilPersonMedRelasjoner(capture(idSlotPersonMedRelasjoner))
            } answers {
                if (idSlotPersonMedRelasjoner.captured.isNotEmpty() && idSlotPersonMedRelasjoner.captured == BARN_DET_IKKE_GIS_TILGANG_TIL_FNR)
                    Tilgang(false, null)
                else
                    Tilgang(true, null)
            }

            val idSlot = slot<List<String>>()
            every {
                mockIntegrasjonClient.sjekkTilgangTilPersoner(capture(idSlot))
            } answers {
                if (idSlot.captured.isNotEmpty() && idSlot.captured.contains(BARN_DET_IKKE_GIS_TILGANG_TIL_FNR))
                    Tilgang(false, null)
                else
                    Tilgang(true, null)
            }

            every { mockIntegrasjonClient.hentArbeidsforhold(any(), any()) } returns emptyList()

            every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns listOf(
                Arbeidsfordelingsenhet(
                    "100",
                    "NAV Familie- og pensjonsytelser Oslo 1"
                )
            )

            every { mockIntegrasjonClient.opprettSkyggesak(any(), any()) } just runs

            every { mockIntegrasjonClient.hentLand(any()) } returns "Testland"

            initEuKodeverk(mockIntegrasjonClient)
        }

        fun initEuKodeverk(integrasjonClient: IntegrasjonClient) {
            val beskrivelsePolen = BeskrivelseDto("POL", "")
            val betydningPolen = BetydningDto(FOM_2004, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen))
            val beskrivelseTyskland = BeskrivelseDto("DEU", "")
            val betydningTyskland =
                BetydningDto(FOM_1900, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland))
            val beskrivelseDanmark = BeskrivelseDto("DNK", "")
            val betydningDanmark =
                BetydningDto(FOM_1990, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark))
            val beskrivelseUK = BeskrivelseDto("GBR", "")
            val betydningUK = BetydningDto(FOM_1900, TOM_2010, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseUK))

            val kodeverkLand = KodeverkDto(
                betydninger = mapOf(
                    "POL" to listOf(betydningPolen),
                    "DEU" to listOf(betydningTyskland),
                    "DNK" to listOf(betydningDanmark),
                    "GBR" to listOf(betydningUK)
                )
            )

            every { integrasjonClient.hentAlleEØSLand() }
                .returns(kodeverkLand)
        }

        val FOM_1900 = LocalDate.of(1900, Month.JANUARY, 1)
        val FOM_1990 = LocalDate.of(1990, Month.JANUARY, 1)
        val FOM_2004 = LocalDate.of(2004, Month.JANUARY, 1)
        val FOM_2008 = LocalDate.of(2008, Month.JANUARY, 1)
        val TOM_2010 = LocalDate.of(2009, Month.DECEMBER, 31)
        val TOM_9999 = LocalDate.of(9999, Month.DECEMBER, 31)
    }
}
