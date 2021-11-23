package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.mockk
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
                InternPeriodeOvergangsstønad(
                    personIdent = personIdent,
                    fomDato = LocalDate.now().minusMonths(6),
                    tomDato = LocalDate.now().plusMonths(6),
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
            barnasFødselsdatoer = listOf(LocalDate.now().minusYears(2)),
            søkerAktør = randomAktørId()
        )

        assertTrue(påvirkerFagsak)
    }

    @Test
    fun `Skal svare false om at nye perioder med full OS påvirker behandling`() {
        val personIdent = randomFnr()
        val søkerAktørId = randomAktørId()

        val påvirkerFagsak = vedtakOmOvergangsstønadPåvirkerFagsak(
            småbarnstilleggBarnetrygdGenerator = SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = 1L,
                tilkjentYtelse = mockk()
            ),
            nyePerioderMedFullOvergangsstønad = listOf(
                InternPeriodeOvergangsstønad(
                    personIdent = personIdent,
                    fomDato = LocalDate.now().minusMonths(10),
                    tomDato = LocalDate.now().plusMonths(6),
                )
            ),
            forrigeSøkersAndeler = listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(6),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = tilfeldigPerson(personIdent = PersonIdent(personIdent)),
                    aktør = søkerAktørId,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(6),
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    person = tilfeldigPerson(personIdent = PersonIdent(personIdent)),
                    aktør = søkerAktørId,
                )
            ),
            barnasFødselsdatoer = listOf(LocalDate.now().minusYears(2)),
            søkerAktør = søkerAktørId
        )

        assertFalse(påvirkerFagsak)
    }

    @Test
    fun `Skal legge til innvilgelsesbegrunnelse for småbarnstillegg`() {
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.now().førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING
        )

        val oppdatertVedtaksperiodeMedBegrunnelser = finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
            vedtaksperioderMedBegrunnelser = listOf(
                vedtaksperiodeMedBegrunnelser
            ),
            innvilgetMånedPeriode = MånedPeriode(
                fom = YearMonth.now(),
                tom = vedtaksperiodeMedBegrunnelser.tom!!.toYearMonth()
            ),
            redusertMånedPeriode = null
        )

        assertNotNull(oppdatertVedtaksperiodeMedBegrunnelser)
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.any { it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.INNVILGET_SMÅBARNSTILLEGG })
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.none { it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD })
    }

    @Test
    fun `Skal legge til reduksjonsbegrunnelse for småbarnstillegg`() {
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.now().nesteMåned().førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING
        )

        val oppdatertVedtaksperiodeMedBegrunnelser = finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
            vedtaksperioderMedBegrunnelser = listOf(
                vedtaksperiodeMedBegrunnelser
            ),
            innvilgetMånedPeriode = null,
            redusertMånedPeriode = MånedPeriode(
                fom = YearMonth.now().nesteMåned(),
                tom = vedtaksperiodeMedBegrunnelser.tom!!.toYearMonth()
            )
        )

        assertNotNull(oppdatertVedtaksperiodeMedBegrunnelser)
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.none { it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.INNVILGET_SMÅBARNSTILLEGG })
        assertTrue(oppdatertVedtaksperiodeMedBegrunnelser.begrunnelser.any { it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD })
    }

    @Test
    fun `Skal kaste feil om det ikke finnes innvilget eller redusert periode å begrunne`() {
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.now().nesteMåned().førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusMonths(3).sisteDagIMåned(),
            type = Vedtaksperiodetype.UTBETALING
        )

        assertThrows<VedtaksperiodefinnerSmåbarnstilleggFeil> {
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                vedtaksperioderMedBegrunnelser = listOf(
                    vedtaksperiodeMedBegrunnelser
                ),
                innvilgetMånedPeriode = null,
                redusertMånedPeriode = null
            )
        }
    }

    @Test
    fun `Skal kunne automatisk iverksette småbarnstillegg når endringer i OS kun er frem i tid`() {
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusYears(2),
                tom = YearMonth.now().minusMonths(10)
            ),
        )

        val nyeAndeler = forrigeAndeler + listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now(),
                tom = YearMonth.now().plusMonths(2)
            ),
        )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeAndeler,
            nyeSmåbarnstilleggAndeler = nyeAndeler
        )

        assertTrue(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder
            )
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når endringer i OS er tilbake og frem i tid`() {
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusYears(2),
                tom = YearMonth.now().minusMonths(10)
            ),
        )

        val nyeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusYears(2),
                tom = YearMonth.now().minusMonths(5)
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.now(),
                tom = YearMonth.now().plusMonths(2)
            ),
        )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeAndeler,
            nyeSmåbarnstilleggAndeler = nyeAndeler
        )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder
            )
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når endringer i OS er 2 måneder frem i tid`() {
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusYears(2),
                tom = YearMonth.now().minusMonths(10)
            ),
        )

        val nyeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusYears(2),
                tom = YearMonth.now().minusMonths(5)
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().plusMonths(2),
                tom = YearMonth.now().plusMonths(4)
            ),
        )

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeAndeler,
            nyeSmåbarnstilleggAndeler = nyeAndeler
        )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder
            )
        )
    }

    @Test
    fun `Skal ikke kunne automatisk iverksette småbarnstillegg når reduksjon i OS kun tilbake i tid`() {
        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusYears(2),
                tom = YearMonth.now().minusMonths(10)
            ),
        )

        val nyeAndeler = emptyList<AndelTilkjentYtelse>()

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeAndeler,
            nyeSmåbarnstilleggAndeler = nyeAndeler
        )

        assertFalse(
            kanAutomatiskIverksetteSmåbarnstillegg(
                innvilgedeMånedPerioder = innvilgedeMånedPerioder,
                reduserteMånedPerioder = reduserteMånedPerioder
            )
        )
    }
}
