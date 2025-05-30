package no.nav.familie.ba.sak.integrasjoner.skyggesak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("postgres", "integrasjonstest")
@Tag("integration")
class SkyggesakServiceTest : AbstractSpringIntegrationTest() {
    @Autowired
    lateinit var skyggesakRepository: SkyggesakRepository

    lateinit var skyggesakService: SkyggesakService

    @BeforeEach
    fun init() {
        skyggesakService =
            SkyggesakService(
                skyggesakRepository = skyggesakRepository,
                fagsakRepository = mockk(),
                integrasjonClient = mockk(relaxed = true),
            )
        skyggesakRepository.deleteAll()
    }

    @Test
    fun `Skal sende skyggesak for fagsak med sendtTidspunkt null`() {
        val sendtTidspunkt = listOf(null, LocalDateTime.now())
        skyggesakRepository.saveAll(
            sendtTidspunkt.mapIndexed { i, tid -> Skyggesak(fagsakId = i.toLong(), sendtTidspunkt = tid) },
        )

        every { skyggesakService.fagsakRepository.finnFagsak(any()) } returns Fagsak(aktør = Aktør("1234567890123"))

        skyggesakService.sendSkyggesaker()

        verify(exactly = 1) {
            skyggesakService.integrasjonClient.opprettSkyggesak(Aktør("1234567890123"), 0)
        }
        Assertions.assertEquals(0, skyggesakRepository.finnSkyggesakerKlareForSending(Pageable.unpaged()).size)
    }

    @Test
    fun `Skal slette skyggesaker eldre enn 14 dager`() {
        val now = LocalDateTime.now()
        val sendtTidspunkt = listOf(now.minusDays(13), now.minusDays(14), null)

        skyggesakRepository.saveAll(
            sendtTidspunkt.mapIndexed { i, tid -> Skyggesak(fagsakId = i.toLong(), sendtTidspunkt = tid) },
        )

        Assertions.assertEquals(2, skyggesakRepository.finnSkyggesakerSomErSendt().size)
        skyggesakService.fjernGamleSkyggesakInnslag()

        // Sjekker at usendt skyggesak ikke er slettet, samt skyggesak sendt for mindre enn 14 dager siden
        Assertions.assertEquals(
            null,
            skyggesakRepository.finnSkyggesakerKlareForSending(Pageable.unpaged()).single().sendtTidspunkt,
        )
        Assertions.assertEquals(
            now.minusDays(13).toLocalDate(),
            skyggesakRepository
                .finnSkyggesakerSomErSendt()
                .single()
                .sendtTidspunkt
                ?.toLocalDate(),
        )
    }
}
