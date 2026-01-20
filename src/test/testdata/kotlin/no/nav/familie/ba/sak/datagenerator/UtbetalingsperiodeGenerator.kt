package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.ekstern.restDomene.PersonDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilPersonDto
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import java.math.BigDecimal

fun lagUtbetalingsperiodeDetalj(
    person: PersonDto = tilfeldigSøker().tilPersonDto(),
    ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    utbetaltPerMnd: Int = sats(YtelseType.ORDINÆR_BARNETRYGD),
    prosent: BigDecimal = BigDecimal.valueOf(100),
) = UtbetalingsperiodeDetalj(
    person = person,
    ytelseType = ytelseType,
    utbetaltPerMnd = utbetaltPerMnd,
    erPåvirketAvEndring = false,
    endringsårsak = null,
    prosent = prosent,
)
