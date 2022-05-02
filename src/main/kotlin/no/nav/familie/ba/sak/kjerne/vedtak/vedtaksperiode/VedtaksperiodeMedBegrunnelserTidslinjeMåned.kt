package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.DagTidspunkt.Companion.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

class VedtaksperiodeMedBegrunnelserTidslinjeMåned(
    private val vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
) : Tidslinje<VedtaksperiodeMedBegrunnelser, Måned>() {

    override fun fraOgMed(): Tidspunkt<Måned> = vedtaksperioderMedBegrunnelser.minOf {
        it.fom.tilTidspunktEllerUendeligLengeSiden().tilInneværendeMåned()
    }

    override fun tilOgMed(): Tidspunkt<Måned> = vedtaksperioderMedBegrunnelser.maxOf {
        it.tom.tilTidspunktEllerUendeligLengeTil().tilInneværendeMåned()
    }

    override fun lagPerioder(): List<Periode<VedtaksperiodeMedBegrunnelser, Måned>> =
        vedtaksperioderMedBegrunnelser.map {
            Periode(
                fraOgMed = it.fom.tilTidspunktEllerUendeligLengeSiden().tilInneværendeMåned(),
                tilOgMed = it.tom.tilTidspunktEllerUendeligLengeTil().tilInneværendeMåned(),
                innhold = it
            )
        }
}
