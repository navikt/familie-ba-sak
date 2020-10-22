package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.økonomi.ØkonomiKlient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.RestSimulerResultat
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity


@TestConfiguration
class ØkonomiTestConfig {

    @Bean
    @Profile("mock-økonomi")
    @Primary
    fun mockØkonomiKlient(): ØkonomiKlient {
        val økonomiKlient: ØkonomiKlient = mockk()

        val iverksettRespons =
                ResponseEntity.ok().body(Ressurs("Mocksvar fra Økonomi-klient", Ressurs.Status.SUKSESS, "", "", null))
        every { økonomiKlient.iverksettOppdrag(any<Utbetalingsoppdrag>()) } returns iverksettRespons

        val hentStatusRespons =
                ResponseEntity.ok().body(Ressurs(OppdragStatus.KVITTERT_OK, Ressurs.Status.SUKSESS, "", "", null))
        every { økonomiKlient.hentStatus(any<OppdragId>()) } returns hentStatusRespons

        every { økonomiKlient.hentEtterbetalingsbeløp(any()) } returns ResponseEntity.ok()
                .body(Ressurs.success(RestSimulerResultat(etterbetaling = 1054)))

        return økonomiKlient
    }
}