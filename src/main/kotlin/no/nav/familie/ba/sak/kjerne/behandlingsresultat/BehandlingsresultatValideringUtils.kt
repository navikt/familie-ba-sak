package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

object BehandlingsresultatValideringUtils {

    internal fun validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
        personerFremstiltKravFor: List<Aktør>,
        vilkårsvurdering: Vilkårsvurdering
    ) {
        val personerSomHarEksplisittAvslag = vilkårsvurdering.personResultater.filter { it.harEksplisittAvslag() }

        if (personerSomHarEksplisittAvslag.all { personerFremstiltKravFor.contains(it.aktør) || it.erSøkersResultater() }) {
            throw Feil("Det eksisterer personer som har fått eksplisitt avslag, men som det ikke har blitt fremstilt krav for.")
        }
    }
}
