package no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Flettefelt

interface BrevPeriode {

    val fom: Flettefelt
    val tom: Flettefelt
    val belop: Flettefelt
    val antallBarn: Flettefelt
    val barnasFodselsdager: Flettefelt
    val begrunnelser: List<Any>
    val type: Flettefelt

    companion object {

        const val BEGRUNNELSE_ERROR_MSG = "Begrunnelse er ikke string eller begrunnelseData"
    }
}

