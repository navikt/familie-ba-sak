package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.lagRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetaljEnkel
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.AvslagBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.FortsattInnvilgetBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.InnvilgelseBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder.OpphørBrevPeriode
import no.nav.familie.ba.sak.kjerne.dokument.tilBrevPeriode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseComparator
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBegrunnelsePerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.v2byggBegrunnelserOgFritekster
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class VedtaksperiodeMedBegrunnelseTest {

    private val søker = tilfeldigSøker()
    private val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    private val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    private val personerIPersongrunnlag = listOf(barn1, barn2, søker).map { it.tilBegrunnelsePerson() }

    @Test
    fun `Skal gi riktig antall brevbegrunnelser med riktig tekst`() {
        val begrunnelserOgFritekster = v2byggBegrunnelserOgFritekster(
            fom = LocalDate.now().minusMonths(2),
            tom = LocalDate.now(),
            utbetalingsperiodeDetaljerEnkel = listOf(
                UtbetalingsperiodeDetaljEnkel(
                    personIdent = barn1.personIdent.ident,
                    utbetaltPerMnd = 1054,
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                ),
                UtbetalingsperiodeDetaljEnkel(
                    personIdent = barn2.personIdent.ident,
                    utbetaltPerMnd = 1054,
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                )
            ),
            standardbegrunnelser = listOf(
                RestVedtaksbegrunnelse(
                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER,
                    vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
                    personIdenter = listOf(barn1.personIdent.ident)
                ),
                RestVedtaksbegrunnelse(
                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                    vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
                    personIdenter = listOf(barn2.personIdent.ident)
                )
            ),
            fritekster = emptyList(),
            begrunnelsepersonerIBehandling = emptyList(),
            målform = Målform.NB,
            uregistrerteBarn = emptyList()
        )

        Assertions.assertEquals(2, begrunnelserOgFritekster.size)
    }

    @Test
    fun `Skal med riktig rekkefølge og tekst på fritekstene`() {
        val begrunnelserOgFritekster = v2byggBegrunnelserOgFritekster(
            fom = LocalDate.now().minusMonths(2),
            tom = LocalDate.now(),
            utbetalingsperiodeDetaljerEnkel = listOf(
                UtbetalingsperiodeDetaljEnkel(
                    personIdent = barn1.personIdent.ident,
                    utbetaltPerMnd = 1054,
                    prosent = BigDecimal(100),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD
                ),
            ),
            standardbegrunnelser = emptyList(),
            fritekster = listOf("Fritekst1", "Fritekst2"),
            begrunnelsepersonerIBehandling = emptyList(),
            målform = Målform.NB,
            uregistrerteBarn = emptyList()
        )

        Assertions.assertEquals("Fritekst1", (begrunnelserOgFritekster[0] as FritekstBegrunnelse).fritekst)
        Assertions.assertEquals("Fritekst2", (begrunnelserOgFritekster[1] as FritekstBegrunnelse).fritekst)
    }

    @Test
    fun `Skal gi fortsatt innvilget-brevperiode`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
                ),
            ),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                begrunnelsepersonerIBehandling = personerIPersongrunnlag,
                målform = Målform.NB,
            ) is FortsattInnvilgetBrevPeriode
        )
    }

    @Test
    fun `Skal gi innvilget-brevperiode`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                ),
            ),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                begrunnelsepersonerIBehandling = personerIPersongrunnlag,
                målform = Målform.NB,
            ) is InnvilgelseBrevPeriode
        )
    }

    @Test
    fun `Skal gi avslagbrevperiode`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.AVSLAG,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER,
                ),
            ),
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                begrunnelsepersonerIBehandling = personerIPersongrunnlag,
                målform = Målform.NB,
            ) is AvslagBrevPeriode
        )
    }

    @Test
    fun `Skal gi opphørbrevperiode`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.OPPHØR,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.personIdent.ident),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                ),
            ),
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                begrunnelsepersonerIBehandling = personerIPersongrunnlag,
                målform = Målform.NB,
            ) is OpphørBrevPeriode
        )
    }

    @Test
    fun `Skal gi null ved ingen begrunnelser eller fritekster`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.UTBETALING,
            begrunnelser = emptyList(),
            fritekster = mutableListOf(),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                begrunnelsepersonerIBehandling = personerIPersongrunnlag,
                målform = Målform.NB,
            ) == null
        )
    }

    @Test
    fun `Skal sortere begrunnelser - type innvilgelse kommer før reduksjon`() {

        val begrunnelseInnvilget =
            lagVedtaksbegrunnelse(vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_HELE_FAMILIEN_PLIKTIG_MEDLEM)
        val begrunnelseReduksjon =
            lagVedtaksbegrunnelse(vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.REDUKSJON_FLYTTET_BARN)
        val comparator = BegrunnelseComparator()

        val begrunnelser =
            setOf(begrunnelseReduksjon, begrunnelseInnvilget).toSortedSet(comparator)

        Assertions.assertEquals(begrunnelseInnvilget, begrunnelser.first())
        Assertions.assertEquals(begrunnelseReduksjon, begrunnelser.last())
    }

    @Test
    fun `Skal kun summere beløp tilhørende personer i begrunnelsen`() {
        val søker = randomFnr()
        val barn1 = randomFnr()
        val barn2 = randomFnr()

        val utbetalingsperiodeDetaljer = listOf(
            lagUtbetalingsperiodeDetaljEnkel(
                utbetaltPerMnd = 8,
                personIdent = søker
            ),
            lagUtbetalingsperiodeDetaljEnkel(
                utbetaltPerMnd = 9,
                personIdent = barn1
            ),
            lagUtbetalingsperiodeDetaljEnkel(
                utbetaltPerMnd = 10,
                personIdent = barn2
            ),
        )

        val restVedtaksbegrunnelse =
            lagRestVedtaksbegrunnelse(personIdenter = listOf(søker, barn1))

        Assertions.assertEquals(
            17,
            utbetalingsperiodeDetaljer.utbetaltForPersonerIBegrunnelse(restVedtaksbegrunnelse)
        )
    }
}
