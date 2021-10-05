package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode

fun finnAlleBarnsFødselsDatoerIUtbetalingsperiode(utbetalingsperiode: Utbetalingsperiode): String =
    Utils.slåSammen(
        utbetalingsperiode.utbetalingsperiodeDetaljer
            .filter { utbetalingsperiodeDetalj ->
                utbetalingsperiodeDetalj.person.type == PersonType.BARN
            }
            .sortedBy { utbetalingsperiodeDetalj ->
                utbetalingsperiodeDetalj.person.fødselsdato
            }
            .map { utbetalingsperiodeDetalj ->
                utbetalingsperiodeDetalj.person.fødselsdato?.tilKortString() ?: ""
            }
    )
