package no.nav.familie.ba.sak.kjerne.småbarnstillegg

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SmåbarnstilleggUtilsTest {
    @Test
    fun `Skal kunne automatisk iverksette småbarnstillegg når endringer i OS kun er frem i tid`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(10),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val nyeAndeler =
            forrigeAndeler +
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = YearMonth.now(),
                        tom = YearMonth.now().plusMonths(2),
                        ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    ),
                )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeAndeler,
                nyeSmåbarnstilleggAndeler = nyeAndeler,
            )

        assertTrue(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder,
            ),
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når endringer i OS er tilbake og frem i tid`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(10),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val nyeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(5),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now(),
                    tom = YearMonth.now().plusMonths(2),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeAndeler,
                nyeSmåbarnstilleggAndeler = nyeAndeler,
            )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder,
            ),
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når endringer i OS er 2 måneder frem i tid`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(10),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val nyeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(5),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().plusMonths(2),
                    tom = YearMonth.now().plusMonths(4),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeAndeler,
                nyeSmåbarnstilleggAndeler = nyeAndeler,
            )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder,
            ),
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når reduksjon i OS kun tilbake i tid`() {
        val forrigeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2),
                    tom = YearMonth.now().minusMonths(10),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            )

        val nyeAndeler = emptyList<AndelTilkjentYtelse>()

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                forrigeSmåbarnstilleggAndeler = forrigeAndeler,
                nyeSmåbarnstilleggAndeler = nyeAndeler,
            )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder,
            ),
        )
    }
}
