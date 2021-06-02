package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.restDomene.tilRestPerson
import no.nav.familie.ba.sak.behandling.vedtak.domene.tilBrevPeriode
import no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.brev.domene.maler.flettefelt
import no.nav.familie.ba.sak.common.lagUtbetalingsperiode
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BrevPeriodeUtilsTest {


    val søker = tilfeldigSøker()
    val barn = tilfeldigPerson(personType = PersonType.BARN)
    val personerIPersongrunnlag = listOf(barn, søker)

    val utbetalingsperioder = listOf(
            lagUtbetalingsperiode(
                    utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj(person = søker.tilRestPerson())),
            )
    )


    @Test
    fun `byggFortsattInnvilgetBrevperiode skal lage brevperiode av typen FORTSATT_INNVILGET`() {
        val vedtaksperiode = lagVedtaksperiodeMedBegrunnelser(
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
                begrunnelser = mutableSetOf(lagVedtaksbegrunnelse(personIdenter = listOf(barn.personIdent.ident)))
        )

        val brevperiode = vedtaksperiode.tilBrevPeriode(
                søker = søker,
                personerIPersongrunnlag = personerIPersongrunnlag,
                utbetalingsperioder = utbetalingsperioder,
        )

        Assertions.assertEquals(flettefelt(BrevPeriodeType.FORTSATT_INNVILGET.apiNavn), brevperiode?.type)
    }
}