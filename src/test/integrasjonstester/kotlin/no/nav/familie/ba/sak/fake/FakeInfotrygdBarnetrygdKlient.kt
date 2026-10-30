package no.nav.familie.ba.sak.fake

import io.mockk.mockk
import no.nav.familie.ba.sak.ekstern.pensjon.BarnetrygdTilPensjonResponse
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdKlient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBrevkode
import no.nav.familie.kontrakter.ba.infotrygd.InfotrygdSøkResponse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate

class FakeInfotrygdBarnetrygdKlient : InfotrygdBarnetrygdKlient(URI.create("http://fake-infotrygd-barnetrygd"), mockk<RestClient>(relaxed = true), 1) {
    val stønaderIInfotrygd = mutableMapOf<Pair<String, List<String>>, InfotrygdSøkResponse<Stønad>>()
    val barnerygdTilPensjon = mutableMapOf<String, BarnetrygdTilPensjonResponse>()

    override fun hentSaker(
        søkersIdenter: List<String>,
        barnasIdenter: List<String>,
    ): InfotrygdSøkResponse<Sak> =
        InfotrygdSøkResponse(
            emptyList(),
            emptyList(),
        )

    override fun hentStønader(
        søkersIdenter: List<String>,
        barnasIdenter: List<String>,
        historikk: Boolean,
    ): InfotrygdSøkResponse<Stønad> =
        stønaderIInfotrygd[Pair(søkersIdenter.first(), barnasIdenter)] ?: InfotrygdSøkResponse(
            emptyList(),
            emptyList(),
        )

    override fun harNyligSendtBrevFor(
        søkersIdenter: List<String>,
        brevkoder: List<InfotrygdBrevkode>,
    ): SendtBrevResponse = SendtBrevResponse(false, emptyList())

    override fun hentBarnetrygdTilPensjon(
        personIdent: String,
        fraDato: LocalDate,
    ): BarnetrygdTilPensjonResponse = barnerygdTilPensjon[personIdent] ?: BarnetrygdTilPensjonResponse(emptyList())

    fun leggTilStønaderIInfotrygd(
        søkersIdent: String,
        barnasIdenter: List<String>,
        stønader: InfotrygdSøkResponse<Stønad>,
    ) {
        stønaderIInfotrygd[Pair(søkersIdent, barnasIdenter)] = stønader
    }

    fun leggTilBarnetrygdTilPensjon(
        søkersIdent: String,
        barnetrygdTilPensjonResponse: BarnetrygdTilPensjonResponse,
    ) {
        barnerygdTilPensjon[søkersIdent] = barnetrygdTilPensjonResponse
    }
}
