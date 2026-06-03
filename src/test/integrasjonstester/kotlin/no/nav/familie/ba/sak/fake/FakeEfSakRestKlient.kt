package no.nav.familie.ba.sak.fake

import io.mockk.mockk
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestKlient
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate

class FakeEfSakRestKlient :
    EfSakRestKlient(
        URI("dummy_uri"),
        mockk<RestClient>(relaxed = true),
    ) {
    private val eksternePerioderResponeMap = mutableMapOf<String, EksternePerioderResponse>()

    override fun hentPerioderMedFullOvergangsstønad(personIdent: String): EksternePerioderResponse =
        if (eksternePerioderResponeMap.containsKey(personIdent)) {
            eksternePerioderResponeMap[personIdent]!!
        } else {
            EksternePerioderResponse(
                perioder =
                    listOf(
                        EksternPeriode(
                            personIdent = personIdent,
                            fomDato = LocalDate.now().minusYears(2),
                            datakilde = Datakilde.EF,
                            tomDato = LocalDate.now().minusMonths(3),
                        ),
                    ),
            )
        }

    fun leggTilEksternPeriode(
        personIdent: String,
        eksternePerioderResponse: EksternePerioderResponse,
    ) {
        eksternePerioderResponeMap[personIdent] = eksternePerioderResponse
    }

    fun reset() {
        eksternePerioderResponeMap.clear()
    }
}
