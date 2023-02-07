package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

class BehandlingsresultatValideringUtils {

    private fun validerAtBarePersonerFremstiltKravForHarFåttEksplisittAvslag(
        personerFremstiltKravFor: List<Aktør>,
        vilkårsvurdering: Vilkårsvurdering
    ) {
        val personerSomHarEksplisittAvslag = vilkårsvurdering.personResultater.filter { it.harEksplisittAvslag() }.map { it.aktør }

        if (!personerFremstiltKravFor.containsAll(personerSomHarEksplisittAvslag)) {
            throw Feil("Det eksisterer personer som har fått eksplisitt avslag, men som det ikke har blitt fremstilt krav for.")
        }
    }
}
