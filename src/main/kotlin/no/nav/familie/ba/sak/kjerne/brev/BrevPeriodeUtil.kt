package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson

fun List<MinimertRestPerson>.tilBarnasFødselsdatoer(): String =
    Utils.slåSammen(
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { person ->
                person.fødselsdato
            }
            .map { person ->
                person.fødselsdato.tilKortString()
            }
    )

fun List<MinimertUtbetalingsperiodeDetalj>.antallBarn(): Int =
    this.filter { it.person.type == PersonType.BARN }.size

fun List<MinimertUtbetalingsperiodeDetalj>.totaltUtbetalt(): Int =
    this.sumOf { it.utbetaltPerMnd }
