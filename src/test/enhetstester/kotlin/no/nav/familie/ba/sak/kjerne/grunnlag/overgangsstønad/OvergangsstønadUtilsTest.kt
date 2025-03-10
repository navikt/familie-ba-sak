package no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggGenerator
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class OvergangsstønadUtilsTest {
    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal svare true om at nye perioder med full OS påvirker behandling`() {
        val personIdent = randomFnr()
        val barnAktør = tilAktør(randomFnr())

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggGenerator =
                    SmåbarnstilleggGenerator(
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent,
                            fomDato = LocalDate.now().minusMonths(6),
                            tomDato = LocalDate.now().plusMonths(6),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnAktør),
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = tilAktør(personIdent)),
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = tilAktør(personIdent)),
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnAktør, LocalDate.now().minusYears(2))),
            )

        assertTrue(påvirkerFagsak)
    }

    @Test
    fun `Skal svare false om at nye perioder med full OS påvirker behandling`() {
        val personIdent = randomAktør()
        val barnIdent = randomAktør()

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggGenerator =
                    SmåbarnstilleggGenerator(
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(10),
                            tomDato = LocalDate.now().plusMonths(6),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnIdent),
                            aktør = barnIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnIdent, LocalDate.now().minusYears(2))),
            )

        assertFalse(påvirkerFagsak)
    }

    @Test
    fun `Skal svare false om at nye perioder med full OS påvirker behandling ved flere perioder`() {
        val personIdent = randomAktør()
        val barnIdent = randomAktør()

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggGenerator =
                    SmåbarnstilleggGenerator(
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(10),
                            tomDato = LocalDate.now().minusMonths(6),
                        ),
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(4),
                            tomDato = LocalDate.now().plusMonths(2),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnIdent),
                            aktør = barnIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().minusMonths(6),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().minusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(4),
                            tom = YearMonth.now().plusMonths(2),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(4),
                            tom = YearMonth.now().plusMonths(2),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnIdent, LocalDate.now().minusYears(2))),
            )

        assertFalse(påvirkerFagsak)
    }

    @Test
    fun `skal ikke behandle vedtak om overgangsstønad når vedtaket ikke fører til endring i utbetaling`() {
        val personIdent = randomAktør()
        val barnIdent = randomAktør()

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggGenerator =
                    SmåbarnstilleggGenerator(
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(10),
                            tomDato = LocalDate.now().minusMonths(1),
                        ),
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now(),
                            tomDato = LocalDate.now().plusMonths(6),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(10),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnIdent),
                            aktør = barnIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(10),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnIdent, LocalDate.now().minusYears(2))),
            )

        assertFalse(påvirkerFagsak)
    }

    @Test
    fun `skal behandle vedtak om overgangsstønad når vedtaket fører til endring i utbetaling`() {
        val personIdent = randomAktør()
        val barnIdent = randomAktør()

        val påvirkerFagsak =
            vedtakOmOvergangsstønadPåvirkerFagsak(
                småbarnstilleggGenerator =
                    SmåbarnstilleggGenerator(
                        tilkjentYtelse = lagInitiellTilkjentYtelse(),
                    ),
                nyePerioderMedFullOvergangsstønad =
                    listOf(
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now().minusMonths(10),
                            tomDato = LocalDate.now().minusMonths(1),
                        ),
                        InternPeriodeOvergangsstønad(
                            personIdent = personIdent.aktørId,
                            fomDato = LocalDate.now(),
                            tomDato = LocalDate.now().plusMonths(8),
                        ),
                    ),
                forrigeAndelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(10),
                            ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                            person = tilfeldigPerson(aktør = barnIdent),
                            aktør = barnIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(10),
                            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                        lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                            fom = YearMonth.now().minusMonths(10),
                            tom = YearMonth.now().plusMonths(6),
                            ytelseType = YtelseType.SMÅBARNSTILLEGG,
                            person = tilfeldigPerson(aktør = personIdent),
                            aktør = personIdent,
                        ),
                    ),
                barnasAktørerOgFødselsdatoer = listOf(Pair(barnIdent, LocalDate.now().minusYears(2))),
            )

        assertTrue(påvirkerFagsak)
    }
}
