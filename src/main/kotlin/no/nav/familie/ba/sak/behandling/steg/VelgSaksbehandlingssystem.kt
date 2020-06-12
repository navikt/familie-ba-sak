package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import org.springframework.stereotype.Service

@Service
class VelgSaksbehandlingssystem
    : BehandlingSteg<String> {

    override fun stegType(): StegType {
        return StegType.VELG_SAKSBEHANDLINGSSYSTEM
    }

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        // TODO: Gjør kall til Infotrygd replika-tjeneste. Har søker eller barn en sak i infotrygd fra før? Ja/nei.

        // TODO: Avgjøre om vi skal saksbehandle hos oss eller Infotrygd

        // TODO: Neste steg er å sende videre til regelkjøring (vilkårsvurdering) - blir noe refaktorering i Simuleringstask/regelkjørBehandling

        return hentNesteStegForNormalFlyt(behandling)
    }
}