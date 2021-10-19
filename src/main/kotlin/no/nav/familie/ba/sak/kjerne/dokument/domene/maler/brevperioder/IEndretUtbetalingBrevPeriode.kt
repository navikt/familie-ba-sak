package no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Flettefelt

interface IEndretUtbetalingBrevPeriode : BrevPeriode {

    override val fom: Flettefelt
    override val tom: Flettefelt
    override val belop: Flettefelt
    override val antallBarn: Flettefelt
    override val barnasFodselsdager: Flettefelt
    override val begrunnelser: List<Any>
    override val type: Flettefelt
    val typeBarnetrygd: Flettefelt
}

