package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class SmåbarnstilleggUtils {

    @Test
    fun `Skal svare true om at nye perioder med full OS påvirker behandling`() {
        val personIdent = randomFnr()

        val påvirkerFagsak = vedtakOmOvergangsstønadPåvirkerFagsak(
            småbarnstilleggBarnetrygdGenerator = SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = 1L,
                tilkjentYtelse = mockk()
            ),
            nyePerioderMedFullOvergangsstønad = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personIdent,
                    fomDato = LocalDate.now().minusMonths(6),
                    tomDato = LocalDate.now().plusMonths(6),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            ),
            forrigeSøkersAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(6),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = tilfeldigPerson(personIdent = PersonIdent(personIdent))
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(6),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    person = tilfeldigPerson(personIdent = PersonIdent(personIdent))
                )
            ),
            barnasFødselsdatoer = listOf(LocalDate.now().minusYears(2))
        )

        assertTrue(påvirkerFagsak)
    }

    @Test
    fun `Skal svare false om at nye perioder med full OS påvirker behandling`() {
        val personIdent = randomFnr()

        val påvirkerFagsak = vedtakOmOvergangsstønadPåvirkerFagsak(
            småbarnstilleggBarnetrygdGenerator = SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = 1L,
                tilkjentYtelse = mockk()
            ),
            nyePerioderMedFullOvergangsstønad = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personIdent,
                    fomDato = LocalDate.now().minusMonths(10),
                    tomDato = LocalDate.now().plusMonths(6),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            ),
            forrigeSøkersAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(6),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = tilfeldigPerson(personIdent = PersonIdent(personIdent))
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(6),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    person = tilfeldigPerson(personIdent = PersonIdent(personIdent))
                )
            ),
            barnasFødselsdatoer = listOf(LocalDate.now().minusYears(2))
        )

        assertFalse(påvirkerFagsak)
    }
}