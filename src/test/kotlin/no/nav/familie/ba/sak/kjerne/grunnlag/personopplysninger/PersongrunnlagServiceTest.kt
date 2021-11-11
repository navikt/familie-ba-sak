package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersongrunnlagServiceTest {

    private val persongrunnlagService = mockk<PersongrunnlagService>()

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Returnerer nytt barn fra personopplysningsgrunnlag`() {
        val søker = randomFnr()
        val barn = randomFnr()
        val nyttbarn = randomFnr()

        val forrigeBehandling = lagBehandling()
        val forrigeGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = forrigeBehandling.id,
            søkerPersonIdent = søker,
            barnasIdenter = listOf(barn)
        )

        val behandling = lagBehandling()
        val grunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker,
            barnasIdenter = listOf(barn, nyttbarn)
        )

        every { persongrunnlagService.hentAktiv(forrigeBehandling.id) } returns forrigeGrunnlag
        every { persongrunnlagService.hentAktiv(behandling.id) } returns grunnlag
        every { persongrunnlagService.finnNyeBarn(any(), any()) } answers { callOriginal() }

        val nye = persongrunnlagService.finnNyeBarn(forrigeBehandling = forrigeBehandling, behandling = behandling)
        assertEquals(nyttbarn, nye.singleOrNull()!!.personIdent.ident)
    }
}
