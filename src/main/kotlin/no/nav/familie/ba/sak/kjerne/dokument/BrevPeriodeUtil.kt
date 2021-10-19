package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj

fun List<UtbetalingsperiodeDetalj>.tilBarnasFødselsdatoer(): String =
    Utils.slåSammen(
        this
            .filter { it.person.type == PersonType.BARN }
            .sortedBy { utbetalingsperiodeDetalj ->
                utbetalingsperiodeDetalj.person.fødselsdato
            }
            .map { utbetalingsperiodeDetalj ->
                utbetalingsperiodeDetalj.person.fødselsdato?.tilKortString() ?: ""
            }
    )

fun List<UtbetalingsperiodeDetalj>.antallBarn(): Int =
    this.filter { it.person.type == PersonType.BARN }.size

fun List<UtbetalingsperiodeDetalj>.totaltUtbetalt(): Int =
    this.sumOf { it.utbetaltPerMnd }
