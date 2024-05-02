package no.nav.familie.ba.sak.cucumber.mock

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.kjerne.logg.Logg
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.logg.LoggType
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import java.time.LocalDateTime

fun mockLoggService(): LoggService {
    val loggService = mockk<LoggService>()
    every { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any(), any()) } just runs
    every { loggService.opprettBehandlingLogg(any()) } just runs
    every { loggService.opprettSettPåMaskinellVent(any(), any()) } just runs
    every { loggService.opprettVilkårsvurderingLogg(any(), any(), any()) } returns null
    every { loggService.opprettFerdigstillBehandling(any()) } just runs
    every { loggService.opprettTattAvMaskinellVent(any()) } just runs
    every { loggService.hentLoggForBehandling(any()) } answers {
        val behandlingId = firstArg<Long>()
        listOf(
            Logg(
                behandlingId = behandlingId,
                type = LoggType.BEHANDLING_OPPRETTET,
                rolle = BehandlerRolle.SYSTEM,
                tekst = "",
            ).copy(opprettetTidspunkt = LocalDateTime.now().minusDays(1)),
        )
    }
    return loggService
}
