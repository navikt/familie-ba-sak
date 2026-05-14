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
        fun `skal låse opp fagsak, sette status til AVSLUTTET, og gjenåpne sak i Joark`() {
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val resultat = fagsakLåsingService.låsOppFagsak(fagsak.id, "Test begrunnelse")

            assertThat(resultat.status).isEqualTo(FagsakStatus.AVSLUTTET)
        }

        @Test
        fun `skal opprette FagsakLåsing med hendelse LÅST_OPP`() {
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val låsingSlot = slot<FagsakLåsing>()
            every { fagsakLåsingRepository.save(capture(låsingSlot)) } answers { firstArg() }

            fagsakLåsingService.låsOppFagsak(fagsak.id, "En god grunn")

            assertThat(låsingSlot.captured.hendelse).isEqualTo(FagsakLåsHendelse.LÅST_OPP)
            assertThat(låsingSlot.captured.begrunnelse).isEqualTo("En god grunn")
            assertThat(låsingSlot.captured.fagsak.id).isEqualTo(fagsak.id)
        }

        @Test
        fun `skal deaktivere gammel aktiv låsing før ny lagres`() {
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            val gammelLåsing =
                FagsakLåsing(
                    id = 99,
                    fagsak = fagsak,
                    tidspunkt = java.time.LocalDateTime.now(),
                    hendelse = FagsakLåsHendelse.LÅST,
                    begrunnelse = "Gammel",
                    aktiv = true,
                )

            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns gammelLåsing
            every { fagsakLåsingRepository.saveAndFlush(any()) } answers { firstArg() }

            fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")

            verify { fagsakLåsingRepository.saveAndFlush(match { !it.aktiv && it.id == 99L }) }
        }

        @Test
        fun `skal kalle gjenaapneSak på integrasjonsklienten med riktige verdier`() {
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val requestSlot = slot<GjenåpneSakRequest>()
            every { integrasjonKlient.gjenåpneSakIDokarkiv(capture(requestSlot)) } just runs

            fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")

            val request = requestSlot.captured
            assertThat(request.tema).isEqualTo(Tema.BAR)
            assertThat(request.fagsakId).isEqualTo(fagsak.id.toString())
            assertThat(request.fagsaksystem).isEqualTo(Fagsystem.BA)
            assertThat(request.bruker.idType).isEqualTo(BrukerIdType.FNR)
            assertThat(request.bruker.id).isEqualTo(fagsak.aktør.aktivFødselsnummer())
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis fagsak ikke har status LÅST`() {
            val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

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
        fun `skal kaste feil hvis begrunnelse er blank`() {
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            assertThrows<FunksjonellFeil> {
                fagsakLåsingService.låsOppFagsak(fagsak.id, "   ")
            }

            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
        }

        @Test
        fun `skal kaste Feil hvis fagsak ikke finnes`() {
            every { fagsakRepository.finnFagsak(any()) } returns null

            assertThrows<no.nav.familie.ba.sak.common.Feil> {
                fagsakLåsingService.låsOppFagsak(999L, "Begrunnelse")
            }
        }
    }
}
