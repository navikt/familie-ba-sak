package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBegrunnelsePerson

class VedtaksperiodeMedBegrunnelseTest {

    private val søker = tilfeldigSøker()
    private val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    private val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    private val personerIPersongrunnlag = listOf(barn1, barn2, søker).map { it.tilBegrunnelsePerson() }

    /*
    TODO: Fiks når vi har minimert dataene inn til brevperioder

    @Test
    fun `Skal gi riktig antall brevbegrunnelser med riktig tekst`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagBrevPeriodeGrunnlagMedPersoner(
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            begrunnelser = listOf(
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn1.aktør.aktivFødselsnummer()),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
                ),
                lagRestVedtaksbegrunnelse(
                    personIdenter = listOf(barn2.aktør.aktivFødselsnummer()),
                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_BOR_MED_SØKER,
                ),
            ),
            fritekster = mutableListOf(),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        val begrunnelserOgFritekster = utvidetVedtaksperiodeMedBegrunnelser.byggBegrunnelserOgFritekster(
            personerIPersongrunnlag = emptyList(),
            uregistrerteBarn = emptyList()
        )

        Assertions.assertEquals(2, begrunnelserOgFritekster.size)
    }

    @Test
    fun `Skal med riktig rekkefølge og tekst på fritekstene`() {
        val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            begrunnelser = emptyList(),
            fritekster = mutableListOf(
                VedtaksbegrunnelseFritekst(
                    id = 1,
                    fritekst = "Fritekst1",
                    vedtaksperiodeMedBegrunnelser = mockk(),
                ),
                VedtaksbegrunnelseFritekst(
                    id = 2,
                    fritekst = "Fritekst2",
                    vedtaksperiodeMedBegrunnelser = mockk(),
                )
            ),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        val begrunnelserOgFritekster = utvidetVedtaksperiodeMedBegrunnelser.byggBegrunnelserOgFritekster(
            personerIPersongrunnlag = emptyList(),
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
                    personIdenter = listOf(barn1.aktør.aktivFødselsnummer()),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
                ),
            ),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
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
                    personIdenter = listOf(barn1.aktør.aktivFødselsnummer()),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                ),
            ),
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj())
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
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
                    personIdenter = listOf(barn1.aktør.aktivFødselsnummer()),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER,
                ),
            ),
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
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
                    personIdenter = listOf(barn1.aktør.aktivFødselsnummer()),
                    vedtakBegrunnelseSpesifikasjon =
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                ),
            ),
        )

        Assertions.assertTrue(
            utvidetVedtaksperiodeMedBegrunnelser.tilBrevPeriode(
                personerIPersongrunnlag = personerIPersongrunnlag,
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
                personerIPersongrunnlag = personerIPersongrunnlag,
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
        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val utbetalingsperiodeDetaljer = listOf(
            lagUtbetalingsperiodeDetalj(
                utbetaltPerMnd = 8,
                person = søker.tilRestPerson()
            ),
            lagUtbetalingsperiodeDetalj(
                utbetaltPerMnd = 9,
                person = barn1.tilRestPerson()
            ),
            lagUtbetalingsperiodeDetalj(
                utbetaltPerMnd = 10,
                person = barn2.tilRestPerson()
            ),
        )

        val utvidetVedtaksperiodeMedBegrunnelser =
            lagUtvidetVedtaksperiodeMedBegrunnelser(utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer)
        val restVedtaksbegrunnelse =
            lagRestVedtaksbegrunnelse(
                personIdenter = listOf(
                    søker.aktør.aktivFødselsnummer(),
                    barn1.aktør.aktivFødselsnummer()
                )
            )

        Assertions.assertEquals(
            17,
            utvidetVedtaksperiodeMedBegrunnelser.utbetaltForPersonerIBegrunnelse(restVedtaksbegrunnelse)
        )
    }*/
}
