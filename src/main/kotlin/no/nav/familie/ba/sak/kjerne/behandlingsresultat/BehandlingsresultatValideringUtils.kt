package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat

object BehandlingsresultatValideringUtils {

    internal fun validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
        personerFremstiltKravFor: List<Aktør>,
        personResultater: Set<PersonResultat>
    ) {
        val personerSomHarEksplisittAvslag = personResultater.filter { it.harEksplisittAvslag() }

        if (personerSomHarEksplisittAvslag.any { !personerFremstiltKravFor.contains(it.aktør) && !it.erSøkersResultater() }) {
            throw FunksjonellFeil(
                frontendFeilmelding = "Det eksisterer personer som har fått eksplisitt avslag, men som det ikke er blitt fremstilt krav for.",
                melding = "Det eksisterer personer som har fått eksplisitt avslag, men som det ikke har blitt fremstilt krav for."
            )
        }
    }
}
