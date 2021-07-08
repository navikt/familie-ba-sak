package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus

fun harSøkerÅpneBehandlinger(behandlinger: List<Behandling>): Boolean {
   return behandlinger.any { behandling -> behandling.status != BehandlingStatus.AVSLUTTET }
}

