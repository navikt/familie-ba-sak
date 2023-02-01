package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

class BehandlingsresultatValideringUtils {

    private fun validerAtBarePersonerFramstiltKravForHarFåttAvslag(
        personerDetErFramstiltKravFor: List<Aktør>,
        vilkårsvurdering: Vilkårsvurdering
    ) {
        val personerSomHarFåttAvslag = vilkårsvurdering.personResultater.filter { it.harEksplisittAvslag() }.map { it.aktør }

        if (!personerDetErFramstiltKravFor.containsAll(personerSomHarFåttAvslag)) {
            throw Feil("Det eksisterer personer som har fått avslag men som ikke har blitt søkt for i søknaden!")
        }
    }
}
