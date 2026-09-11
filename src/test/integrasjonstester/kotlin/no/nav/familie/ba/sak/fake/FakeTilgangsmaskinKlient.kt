package no.nav.familie.ba.sak.fake

import io.mockk.mockk
import no.nav.familie.tilgangsmaskin.Regeltype
import no.nav.familie.tilgangsmaskin.TilgangsmaskinKlient
import no.nav.familie.tilgangsmaskin.TilgangsmaskinResultat
import java.net.URI

class FakeTilgangsmaskinKlient : TilgangsmaskinKlient(URI("http://tilgangsmaskin-url"), mockk(relaxed = true)) {
    override fun sjekkTilgangTilPersoner(
        personIdenter: Set<String>,
        regeltype: Regeltype,
    ): List<TilgangsmaskinResultat> =
        personIdenter
            .filter { it.isNotBlank() }
            .map { personIdent ->
                TilgangsmaskinResultat(
                    personIdent = personIdent,
                    harTilgang = true,
                    httpStatus = 204,
                )
            }
}
