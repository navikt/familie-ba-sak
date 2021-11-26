package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksbegrunnelseTest {
    val søker = lagPerson(type = PersonType.SØKER)
    val barn1 = lagPerson(type = PersonType.BARN)
    val barn2 = lagPerson(type = PersonType.BARN)
    val barn3 = lagPerson(type = PersonType.BARN)

    val restVedtaksbegrunnelse = lagRestVedtaksbegrunnelse(
        vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
        personIdenter = listOf(søker, barn1, barn2).map { it.personIdent.ident },
        vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
    )

    val vedtaksperiode = NullablePeriode(LocalDate.now().minusMonths(1), LocalDate.now())

    val personerIPersongrunnlag = listOf(søker, barn1, barn2, barn3)

    val målform = Målform.NB

    @Test
    fun `skal gå bra`() {

        val brevbegrunnelse = restVedtaksbegrunnelse.tilBrevBegrunnelse(
            vedtaksperiode = vedtaksperiode,
            personerIPersongrunnlag = personerIPersongrunnlag,
            målform = målform,
            uregistrerteBarn = emptyList(),
            beløp = "1234"
        ) as BegrunnelseData

        Assertions.assertEquals(true, brevbegrunnelse.gjelderSoker)
    }
}
