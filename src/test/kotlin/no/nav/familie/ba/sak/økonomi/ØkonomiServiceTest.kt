package no.nav.familie.ba.sak.økonomi

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.YtelseType.*
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ØkonomiServiceTest {

    lateinit var beregningService: BeregningService
    lateinit var økonomiService: ØkonomiService

    @BeforeAll
    fun setUp() {
        beregningService = mockk()
        økonomiService = spyk(ØkonomiService(mockk(), mockk(), mockk(), mockk(), beregningService, mockk()))
    }

    @Test
    fun `skal separere i nye og opphørte andeler for økonomi ved endret lengde på periode (ny periode erstatter opphørt)`() {
        val søker = tilfeldigPerson()
        val barn = tilfeldigPerson()

        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()

        val andelerBehandling1 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling1,
                                       person = søker),
                lagAndelTilkjentYtelse("2019-03-01",
                                       "2037-02-28",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling1,
                                       person = barn))
        val andelerBehandling2 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2023-03-31",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling2,
                                       person = søker),
                lagAndelTilkjentYtelse("2019-03-01",
                                       "2037-02-28",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling2,
                                       person = barn))

        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling1.id) } returns andelerBehandling1
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling2.id) } returns andelerBehandling2

        val (nye, opphørte) = økonomiService.separerNyeOgOpphørteAndelerForØkonomi(behandlingId = behandling2.id, forrigeBehandlingId = behandling1.id)
        assertEquals( 1,nye.size)
        assertEquals(1, opphørte.size)
        assertEquals( andelerBehandling1[0], opphørte.first())
        assertEquals( andelerBehandling2[0], nye.first())
    }

    @Test
    fun `skal separere opphørt periode`() {
        val søker = tilfeldigPerson()
        val barn = tilfeldigPerson()

        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()

        val andelerBehandling1 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2023-03-31",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling2,
                                       person = søker),
                lagAndelTilkjentYtelse("2019-03-01",
                                       "2037-02-28",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling1,
                                       person = barn))
        val andelerBehandling2 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2023-03-31",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling2,
                                       person = søker))

        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling1.id) } returns andelerBehandling1
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling2.id) } returns andelerBehandling2

        val (nye, opphørte) = økonomiService.separerNyeOgOpphørteAndelerForØkonomi(behandlingId = behandling2.id, forrigeBehandlingId = behandling1.id)
        assertEquals( 0, nye.size)
        assertEquals(1, opphørte.size)
        assertEquals( andelerBehandling1[1], opphørte.first())
    }

    @Test
    fun `skal separere ny periode`() {
        val søker = tilfeldigPerson()
        val barn = tilfeldigPerson()

        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()

        val andelerBehandling1 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2023-03-31",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling2,
                                       person = søker))
        val andelerBehandling2 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2023-03-31",
                                       SMÅBARNSTILLEGG,
                                       660,
                                       behandling2,
                                       person = søker),
                lagAndelTilkjentYtelse("2019-03-01",
                                       "2037-02-28",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling1,
                                       person = barn)
        )

        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling1.id) } returns andelerBehandling1
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling2.id) } returns andelerBehandling2

        val (nye, opphørte) = økonomiService.separerNyeOgOpphørteAndelerForØkonomi(behandlingId = behandling2.id, forrigeBehandlingId = behandling1.id)
        assertEquals( 1, nye.size)
        assertEquals(0, opphørte.size)
        assertEquals( andelerBehandling2[1], nye.first())
    }

    @Test
    fun `skal lage nye perioder når forrige behandling er et opphør`() {
        val søker = tilfeldigPerson()

        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()

        val andelerBehandling2 = listOf(
                lagAndelTilkjentYtelse("2019-03-01",
                                       "2037-02-28",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       behandling1,
                                       person = søker)
        )

        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling1.id) } returns emptyList()
        every { beregningService.hentAndelerTilkjentYtelseForBehandling(behandling2.id) } returns andelerBehandling2

        val (nye, opphørte) = økonomiService.separerNyeOgOpphørteAndelerForØkonomi(behandlingId = behandling2.id, forrigeBehandlingId = behandling1.id)
        assertEquals( 1, nye.size)
        assertEquals(0, opphørte.size)
        assertEquals( andelerBehandling2[0], nye.first())
    }
}