package no.nav.familie.ba.sak.kjerne.dokument.domene.maler.brevperioder

import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Flettefelt
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.flettefelt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse

data class AvslagBrevPeriode(
    override val fom: Flettefelt,
    override val tom: Flettefelt,
    override val belop: Flettefelt,
    override val antallBarn: Flettefelt,
    override val barnasFodselsdager: Flettefelt,
    override val begrunnelser: List<Any>,
    override val type: Flettefelt,
) : BrevPeriode {

    constructor(
        fom: String,
        tom: String? = null,
        begrunnelser: List<Begrunnelse>,
    ) : this(
        fom = flettefelt(fom),
        tom = flettefelt(if (tom.isNullOrBlank()) "" else "til $tom "),
        belop = flettefelt(null),
        antallBarn = flettefelt(null),
        barnasFodselsdager = flettefelt(null),
        begrunnelser = begrunnelser.map {
            when (it) {
                is FritekstBegrunnelse -> it.fritekst
                is BegrunnelseData -> it
                else -> error(BrevPeriode.BEGRUNNELSE_ERROR_MSG)
            }
        },
        type = flettefelt(BrevPeriodeType.AVSLAG.apiNavn),
    )
}
