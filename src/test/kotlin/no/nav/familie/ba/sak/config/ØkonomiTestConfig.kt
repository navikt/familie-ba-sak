package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.økonomi.OppdragProtokollStatus
import no.nav.familie.ba.sak.økonomi.ØkonomiKlient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity


@TestConfiguration
class ØkonomiTestConfig {

    @Bean
    @Profile("mock-iverksett")
    @Primary
    fun mockØkonomiKlient(): ØkonomiKlient {
        val økonomiKlient: ØkonomiKlient = mockk()

        val iverksettRespons =
                ResponseEntity.ok().body(Ressurs("Mocksvar fra Økonomi-klient", Ressurs.Status.SUKSESS, "", ""))
        every { økonomiKlient.iverksettOppdrag(any<Utbetalingsoppdrag>()) } returns iverksettRespons

        val hentStatusRespons =
                ResponseEntity.ok().body(Ressurs(OppdragProtokollStatus.KVITTERT_OK, Ressurs.Status.SUKSESS, "", ""))
        every { økonomiKlient.hentStatus(any<StatusFraOppdragDTO>()) } returns hentStatusRespons

        return økonomiKlient
    }
}