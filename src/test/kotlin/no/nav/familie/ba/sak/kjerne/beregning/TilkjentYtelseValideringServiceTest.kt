package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelseValideringServiceTest {

    private val beregningServiceMock = mockk<BeregningService>()
    private val totrinnskontrollServiceMock = mockk<TotrinnskontrollService>()
    private val persongrunnlagServiceMock = mockk<PersongrunnlagService>()

    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @BeforeEach
    fun setUp() {
        tilkjentYtelseValideringService = TilkjentYtelseValideringService(
            beregningService = beregningServiceMock,
            totrinnskontrollService = totrinnskontrollServiceMock,
            persongrunnlagService = persongrunnlagServiceMock
        )

        every { beregningServiceMock.hentRelevanteTilkjentYtelserForBarn(barnAktør = barn1.aktør, fagsakId = any()) } answers { emptyList() }
        every { beregningServiceMock.hentRelevanteTilkjentYtelserForBarn(barnAktør = barn2.aktør, fagsakId = any()) } answers { emptyList() }
        every { beregningServiceMock.hentRelevanteTilkjentYtelserForBarn(barnAktør = barn3MedUtbetalinger.aktør, fagsakId = any()) } answers {
            listOf(
                TilkjentYtelse(behandling = lagBehandling(), endretDato = LocalDate.now().minusYears(1), opprettetDato = LocalDate.now().minusYears(1))
            )
        }
    }

    @Test
    fun `Skal returnere false hvis ingen barn allerede mottar barnetrygd`() {
        Assertions.assertFalse(
            tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                behandling = lagBehandling(),
                barna = listOf(barn1, barn2)
            )
        )
    }

    @Test
    fun `Skal returnere true hvis det løper barnetrygd for minst ett barn`() {
        Assertions.assertTrue(
            tilkjentYtelseValideringService.barnetrygdLøperForAnnenForelder(
                behandling = lagBehandling(),
                barna = listOf(barn1, barn3MedUtbetalinger)
            )
        )
    }

    companion object {
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val barn3MedUtbetalinger = lagPerson(type = PersonType.BARN)
    }
}
