package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilkjentYtelseValideringServiceTest {

    private val beregningServiceMock = mockk<BeregningService>()
    private val totrinnskontrollServiceMock = mockk<TotrinnskontrollService>()
    private val persongrunnlagServiceMock = mockk<PersongrunnlagService>()
    private val personidentServiceMock = mockk<PersonidentService>()
    private val behandlingServiceMock = mockk<BehandlingService>()

    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @BeforeEach
    fun setUp() {
        tilkjentYtelseValideringService = TilkjentYtelseValideringService(
            beregningService = beregningServiceMock,
            totrinnskontrollService = totrinnskontrollServiceMock,
            persongrunnlagService = persongrunnlagServiceMock,
            personidentService = personidentServiceMock,
            behandlingService = behandlingServiceMock
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

    @Test
    fun `Skal returnere liste med personer som har etterbetaling som er mer enn 3 år tilbake i tid`() {
        val behandling = lagBehandling()
        val person1 = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    fom = inneværendeMåned().minusYears(4),
                    tom = inneværendeMåned(),
                    beløp = 2108,
                    person = person1
                ),
                lagAndelTilkjentYtelse(
                    fom = inneværendeMåned().minusYears(4),
                    tom = inneværendeMåned(),
                    beløp = 2108,
                    person = person2
                ),
            )
        )

        val forrigeBehandling = lagBehandling()

        val forrigeTilkjentYtelse = TilkjentYtelse(
            behandling = forrigeBehandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
            andelerTilkjentYtelse = mutableSetOf(
                lagAndelTilkjentYtelse(
                    fom = inneværendeMåned().minusYears(4),
                    tom = inneværendeMåned(),
                    beløp = 2108,
                    person = person1
                ),
                lagAndelTilkjentYtelse(
                    fom = inneværendeMåned().minusYears(4),
                    tom = inneværendeMåned(),
                    beløp = 1054,
                    person = person2
                ),
            )
        )

        every { beregningServiceMock.hentTilkjentYtelseForBehandling(behandlingId = behandling.id) } answers { tilkjentYtelse }
        every { behandlingServiceMock.hent(behandlingId = behandling.id) } answers { behandling }
        every { behandlingServiceMock.hentForrigeBehandlingSomErIverksatt(behandling = behandling) } answers { forrigeBehandling }
        every { beregningServiceMock.hentOptionalTilkjentYtelseForBehandling(behandlingId = forrigeBehandling.id) } answers { forrigeTilkjentYtelse }
        every { personidentServiceMock.hentAktør(person1.aktør.aktørId) } answers { person1.aktør }
        every { personidentServiceMock.hentAktør(person2.aktør.aktørId) } answers { person2.aktør }

        Assertions.assertTrue(tilkjentYtelseValideringService.finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId = behandling.id).size == 1)
        Assertions.assertEquals(person2.aktør.aktivFødselsnummer(), tilkjentYtelseValideringService.finnAktørerMedUgyldigEtterbetalingsperiode(behandlingId = behandling.id).single())
    }

    companion object {
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val barn3MedUtbetalinger = lagPerson(type = PersonType.BARN)
    }
}
