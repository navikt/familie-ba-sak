package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.AutomatiskOppdaterValutakursService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class OppdaterValutakursTaskTest {
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val automatiskOppdaterValutakursService: AutomatiskOppdaterValutakursService = mockk()
    private val oppdaterValutakursTask =
        OppdaterValutakursTask(
            automatiskOppdaterValutakursService = automatiskOppdaterValutakursService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    @Test
    fun `skal kaste feil hvis behandling ikke er i behandlingsresultatsteg`() {
        val behandling = lagBehandling()
        val endringstidspunkt = YearMonth.of(2024, 1)

        every { behandlingHentOgPersisterService.hent(any()) } returns behandling

        assertThrows<IllegalArgumentException> {
            oppdaterValutakursTask.doTask(OppdaterValutakursTask.opprettTask(behandling.id, endringstidspunkt))
        }
    }

    @Test
    fun `skal oppdatere valutakurs fra gitt dato`() {
        val behandling =
            lagBehandling(
                førsteSteg = StegType.BEHANDLINGSRESULTAT,
            )
        val endringstidspunkt = YearMonth.of(2024, 1)

        every { behandlingHentOgPersisterService.hent(any()) } returns behandling
        every { automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(any<BehandlingId>(), any(), any()) } just runs

        oppdaterValutakursTask.doTask(OppdaterValutakursTask.opprettTask(behandling.id, endringstidspunkt))

        verify(exactly = 1) {
            automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(
                behandlingId = BehandlingId(behandling.id),
                endringstidspunkt = endringstidspunkt,
            )
        }
    }
}
