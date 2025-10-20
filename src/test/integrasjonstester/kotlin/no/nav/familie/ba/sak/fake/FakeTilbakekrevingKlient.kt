package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingId
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ba.sak.testfiler.Testfil
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import org.springframework.web.client.RestOperations
import java.net.URI

class FakeTilbakekrevingKlient(
    restOperations: RestOperations,
) : TilbakekrevingKlient(URI.create("http://tilbakekreving"), restOperations) {
    override fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray = Testfil.TEST_PDF

    override fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): TilbakekrevingId = "id1"

    override fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean = false

    override fun hentTilbakekrevingsbehandlinger(fagsakId: Long): List<Behandling> = emptyList()
}
