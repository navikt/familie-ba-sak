package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.mockk
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class SmåbarnstilleggUtilsTest {

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

    @Test
    fun `Skal generere og begrunne reduksjon i småbarnstillegg, samt ny innvilget periode`() {
        val forrigeFom = YearMonth.now().minusYears(2)
        val forrigeTom = YearMonth.now().minusMonths(10)
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = forrigeFom,
                tom = forrigeTom
            ),
        )

        val nyFom = YearMonth.now().minusYears(1)
        val nyeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = nyFom,
                tom = forrigeTom
            ),
        )

        val vedtaksperioder = utledVedtaksperioderTilAutovedtakVedOSVedtak(
            vedtaksperioderMedBegrunnelser = emptyList(),
            vedtak = lagVedtak(),
            forrigeSmåbarnstilleggAndeler = forrigeAndeler,
            nyeSmåbarnstilleggAndeler = nyeAndeler
        )

        assertEquals(vedtaksperioder.size, 2)

        validerVedtaksperiode(
            vedtaksperioder = vedtaksperioder,
            fom = forrigeFom.førsteDagIInneværendeMåned(),
            tom = nyFom.forrigeMåned().sisteDagIInneværendeMåned(),
            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )

        validerVedtaksperiode(
            vedtaksperioder = vedtaksperioder,
            fom = nyFom.førsteDagIInneværendeMåned(),
            tom = forrigeTom.sisteDagIInneværendeMåned(),
            vedtaksperiodetype = Vedtaksperiodetype.UTBETALING
        )
    }

    @Test
    fun `Skal generere og begrunne reduksjoner i småbarnstillegg`() {
        val forrigeFom = YearMonth.now().minusYears(2)
        val forrigeTom = YearMonth.now().minusMonths(2)
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = forrigeFom,
                tom = forrigeTom
            ),
        )

        val nyTom1 = YearMonth.now().minusMonths(14)
        val nyFom2 = YearMonth.now().minusMonths(12)
        val nyTom2 = YearMonth.now().minusMonths(10)

        val nyFom3 = YearMonth.now().minusMonths(8)
        val nyTom3 = YearMonth.now().minusMonths(5)
        val nyeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = forrigeFom,
                tom = nyTom1
            ),
            lagAndelTilkjentYtelse(
                fom = nyFom2,
                tom = nyTom2
            ),
            lagAndelTilkjentYtelse(
                fom = nyFom3,
                tom = nyTom3
            ),
        )

        val vedtaksperioder = utledVedtaksperioderTilAutovedtakVedOSVedtak(
            vedtaksperioderMedBegrunnelser = emptyList(),
            vedtak = lagVedtak(),
            forrigeSmåbarnstilleggAndeler = forrigeAndeler,
            nyeSmåbarnstilleggAndeler = nyeAndeler
        )

        val reduksjonsperioder = vedtaksperioder.filter { it.type == Vedtaksperiodetype.OPPHØR }.sortedBy { it.fom }
        assertEquals(reduksjonsperioder.size, 3)

        validerVedtaksperiode(
            vedtaksperioder = reduksjonsperioder,
            fom = nyTom1.nesteMåned().førsteDagIInneværendeMåned(),
            tom = nyFom2.forrigeMåned().sisteDagIInneværendeMåned(),
            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )

        validerVedtaksperiode(
            vedtaksperioder = reduksjonsperioder,
            fom = nyTom2.nesteMåned().førsteDagIInneværendeMåned(),
            tom = nyFom3.forrigeMåned().sisteDagIInneværendeMåned(),
            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )

        validerVedtaksperiode(
            vedtaksperioder = reduksjonsperioder,
            fom = nyTom3.nesteMåned().førsteDagIInneværendeMåned(),
            tom = forrigeTom.sisteDagIInneværendeMåned(),
            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )
    }

    @Test
    fun `Skal gjenbruke opphør og utbetalingsperiode ved begrunnelse av småbarnstillegg`() {
        val forrigeFom = YearMonth.now().minusYears(2)
        val forrigeTom = YearMonth.now().minusMonths(10)
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = forrigeFom,
                tom = forrigeTom
            ),
        )

        val nyFom = YearMonth.now().minusYears(1)
        val nyeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = nyFom,
                tom = forrigeTom
            ),
        )

        val vedtaksperioder = utledVedtaksperioderTilAutovedtakVedOSVedtak(
            vedtaksperioderMedBegrunnelser = listOf(
                lagVedtaksperiodeMedBegrunnelser(
                    fom = forrigeFom.førsteDagIInneværendeMåned(),
                    tom = nyFom.forrigeMåned().sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.OPPHØR
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    fom = forrigeFom.førsteDagIInneværendeMåned(),
                    tom = nyFom.forrigeMåned().sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING
                ),
                lagVedtaksperiodeMedBegrunnelser(
                    fom = nyFom.førsteDagIInneværendeMåned(),
                    tom = forrigeTom.sisteDagIInneværendeMåned(),
                    type = Vedtaksperiodetype.UTBETALING
                )
            ),
            vedtak = lagVedtak(),
            forrigeSmåbarnstilleggAndeler = forrigeAndeler,
            nyeSmåbarnstilleggAndeler = nyeAndeler
        )

        assertEquals(vedtaksperioder.size, 2)

        validerVedtaksperiode(
            vedtaksperioder = vedtaksperioder,
            fom = forrigeFom.førsteDagIInneværendeMåned(),
            tom = nyFom.forrigeMåned().sisteDagIInneværendeMåned(),
            vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
        )

        validerVedtaksperiode(
            vedtaksperioder = vedtaksperioder,
            fom = nyFom.førsteDagIInneværendeMåned(),
            tom = forrigeTom.sisteDagIInneværendeMåned(),
            vedtaksperiodetype = Vedtaksperiodetype.UTBETALING
        )
    }

    private fun validerVedtaksperiode(
        vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
        fom: LocalDate,
        tom: LocalDate,
        vedtaksperiodetype: Vedtaksperiodetype
    ) {
        val reduksjonsperiode = vedtaksperioder.find { it.fom == fom && it.tom == tom && it.type == vedtaksperiodetype }
        assertNotNull(reduksjonsperiode)
        when (vedtaksperiodetype) {
            Vedtaksperiodetype.OPPHØR -> reduksjonsperiode?.begrunnelser?.any { it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_SAMBOER_MER_ENN_12_MÅNEDER }
            Vedtaksperiodetype.UTBETALING -> reduksjonsperiode?.begrunnelser?.any { it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER }
        }
    }
}
