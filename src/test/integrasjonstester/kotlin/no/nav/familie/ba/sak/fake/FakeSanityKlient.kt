package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.config.restTemplate
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.sanity.SanityData

class FakeSanityKlient : SanityKlient("ba-brev", restTemplate, 1) {
    private val begrunnelser: List<SanityBegrunnelse> = SanityData.hentSanityBegrunnelser()
    private val eøsBegrunnelser: List<SanityEØSBegrunnelse> = SanityData.hentSanityEØSBegrunnelser()

    override fun hentBegrunnelser(): List<SanityBegrunnelse> = begrunnelser

    override fun hentEØSBegrunnelser(): List<SanityEØSBegrunnelse> = eøsBegrunnelser
}
