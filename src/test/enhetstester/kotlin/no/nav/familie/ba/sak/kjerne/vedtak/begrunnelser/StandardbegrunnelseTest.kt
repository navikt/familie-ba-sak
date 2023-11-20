package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.brev.lagMinimertPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StandardbegrunnelseTest {
    private val behandling = lagBehandling()
    private val søker = tilfeldigPerson(personType = PersonType.SØKER)
    private val barn = tilfeldigPerson(personType = PersonType.BARN)
    val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

    @Test
    fun `dødeBarnForrigePeriode() skal returnere barn som døde i forrige periode og som er tilknyttet ytelsen`() {
        val barn1Fnr = "12345678910"
        val barn2Fnr = "12345678911"

        // Barn1 dør før Barn2.
        var dødsfallDatoBarn1 = LocalDate.of(2022, 5, 12)
        var dødsfallDatoBarn2 = LocalDate.of(2022, 7, 2)
        var barnIBehandling =
            listOf(
                lagMinimertPerson(dødsfallsdato = dødsfallDatoBarn1, aktivPersonIdent = barn1Fnr),
                lagMinimertPerson(dødsfallsdato = dødsfallDatoBarn2, aktivPersonIdent = barn2Fnr),
            )
        var ytelserForrigePeriode =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom =
                        YearMonth.of(
                            dødsfallDatoBarn1.minusMonths(1).year,
                            dødsfallDatoBarn1.minusMonths(1).month,
                        ),
                    tom = YearMonth.of(dødsfallDatoBarn1.year, dødsfallDatoBarn1.month),
                    aktør = Aktør(barn1Fnr + "00").also { it.personidenter.add(Personident(barn1Fnr, it)) },
                ),
            )

        var dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(
            1,
            dødeBarnForrigePeriode.size,
        )
        assertEquals(
            barn1Fnr,
            dødeBarnForrigePeriode[0],
        )

        ytelserForrigePeriode =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom =
                        YearMonth.of(
                            dødsfallDatoBarn1.minusMonths(1).year,
                            dødsfallDatoBarn1.minusMonths(1).month,
                        ),
                    tom = YearMonth.of(dødsfallDatoBarn2.year, dødsfallDatoBarn2.month),
                    aktør = Aktør(barn2Fnr + "00").also { it.personidenter.add(Personident(barn2Fnr, it)) },
                ),
            )

        dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(
            1,
            dødeBarnForrigePeriode.size,
        )
        assertEquals(
            barn2Fnr,
            dødeBarnForrigePeriode[0],
        )

        // Barn1 og Barn2 dør i samme måned
        dødsfallDatoBarn1 = LocalDate.of(2022, 5, 12)
        dødsfallDatoBarn2 = LocalDate.of(2022, 5, 2)

        barnIBehandling =
            listOf(
                lagMinimertPerson(dødsfallsdato = dødsfallDatoBarn1, aktivPersonIdent = barn1Fnr),
                lagMinimertPerson(dødsfallsdato = dødsfallDatoBarn2, aktivPersonIdent = barn2Fnr),
            )

        ytelserForrigePeriode =
            listOf(
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom =
                        YearMonth.of(
                            dødsfallDatoBarn1.minusMonths(1).year,
                            dødsfallDatoBarn1.minusMonths(1).month,
                        ),
                    tom = YearMonth.of(dødsfallDatoBarn1.year, dødsfallDatoBarn1.month),
                    aktør = Aktør(barn1Fnr + "00").also { it.personidenter.add(Personident(barn1Fnr, it)) },
                ),
                lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                    fom = YearMonth.of(dødsfallDatoBarn2.minusMonths(1).year, dødsfallDatoBarn2.minusMonths(1).month),
                    tom = YearMonth.of(dødsfallDatoBarn2.year, dødsfallDatoBarn2.month),
                    aktør = Aktør(barn2Fnr + "00").also { it.personidenter.add(Personident(barn2Fnr, it)) },
                ),
            )

        dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(
            2,
            dødeBarnForrigePeriode.size,
        )
        assertTrue(
            dødeBarnForrigePeriode.containsAll(barnIBehandling.map { it.aktivPersonIdent }),
        )
    }
}
