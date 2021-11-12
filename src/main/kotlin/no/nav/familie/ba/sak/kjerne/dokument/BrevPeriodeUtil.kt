package no.nav.familie.ba.sak.kjerne.dokument

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj

fun List<RestPerson>.tilBarnasFødselsdatoer(): String =
    Utils.slåSammen(
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { person ->
                person.fødselsdato
            }
            .map { person ->
                person.fødselsdato?.tilKortString() ?: ""
            }
    )

fun List<UtbetalingsperiodeDetalj>.antallBarn(): Int =
    this.filter { it.person.type == PersonType.BARN }.size

fun List<UtbetalingsperiodeDetalj>.totaltUtbetalt(): Int =
    this.sumOf { it.utbetaltPerMnd }
