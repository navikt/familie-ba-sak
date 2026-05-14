package no.nav.familie.ba.sak.kjerne.fagsaklåsing

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.GjenåpneSakRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FagsakLåsingServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakLåsingRepository = mockk<FagsakLåsingRepository>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()

    private val fagsakLåsingService =
        FagsakLåsingService(
            fagsakRepository = fagsakRepository,
            fagsakLåsingRepository = fagsakLåsingRepository,
            integrasjonKlient = integrasjonKlient,
        )

    @Nested
    inner class LåsOppFagsak {
        @BeforeEach
        fun setUp() {
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(any()) } returns null
            every { fagsakLåsingRepository.save(any()) } answers { firstArg() }
            every { fagsakRepository.save(any()) } answers { firstArg() }
            every { integrasjonKlient.gjenåpneSakIDokarkiv(any()) } just runs
        }

        @Test
        fun `skal opprette FagsakLåsing med hendelse LÅST_OPP`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val låsingSlot = slot<FagsakLåsing>()
            every { fagsakLåsingRepository.save(capture(låsingSlot)) } answers { firstArg() }

            // Act
            fagsakLåsingService.låsOppFagsak(fagsak.id, "En god grunn")

            // Assert
            assertThat(låsingSlot.captured.hendelse).isEqualTo(FagsakLåsHendelse.LÅST_OPP)
            assertThat(låsingSlot.captured.begrunnelse).isEqualTo("En god grunn")
            assertThat(låsingSlot.captured.fagsak.id).isEqualTo(fagsak.id)
        }

        @Test
        fun `skal sette fagsak status til AVSLUTTET`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act
            val oppdatertFagsak = fagsakLåsingService.låsOppFagsak(fagsak.id, "En god grunn")

            // Assert
            assertThat(oppdatertFagsak.status).isEqualTo(FagsakStatus.AVSLUTTET)
            verify { fagsakRepository.save(match { it.status == FagsakStatus.AVSLUTTET }) }
        }

        @Test
        fun `skal kalle gjenåpneSak på integrasjonsklienten med riktige verdier`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val requestSlot = slot<GjenåpneSakRequest>()
            every { integrasjonKlient.gjenåpneSakIDokarkiv(capture(requestSlot)) } just runs

            // Act
            fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")

            // Assert
            val request = requestSlot.captured
            assertThat(request.tema).isEqualTo(Tema.BAR)
            assertThat(request.fagsakId).isEqualTo(fagsak.id.toString())
            assertThat(request.fagsaksystem).isEqualTo(Fagsystem.BA)
            assertThat(request.bruker.idType).isEqualTo(BrukerIdType.FNR)
            assertThat(request.bruker.id).isEqualTo(fagsak.aktør.aktivFødselsnummer())
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis fagsak ikke har status LÅST`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")
                }

            assertThat(feil.message).contains("LÅST")
            assertThat(feil.message).contains("LØPENDE")
            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis begrunnelse er blank`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            assertThrows<FunksjonellFeil> {
                fagsakLåsingService.låsOppFagsak(fagsak.id, "   ")
            }

            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
        }

        @Test
        fun `skal ikke kalle integrasjonsklienten hvis fagsak har feil status`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.OPPRETTET)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            assertThrows<FunksjonellFeil> {
                fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")
            }

            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
            verify(exactly = 0) { fagsakRepository.save(any()) }
        }
    }
}
