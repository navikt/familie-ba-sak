package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling

class VelgSaksbehandlingssystem
    : BehandlingSteg<String> {

    override fun stegType(): StegType {
        return StegType.VELG_SAKSBEHANDLINGSSYSTEM
    }

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String, stegService: StegService?): StegType {
        // TODO: Kjøre filtreringsregler

        // TODO: Avgjøre hva som blir samlet resultat - skal vi saksbehandle hos oss eller Infotrygd

        // TODO: Neste steg er å sende videre til regelkjøring (vilkårsvurdering) - blir noe refaktorering i Simuleringstask/
        // regelkjørBehandling
    }


}