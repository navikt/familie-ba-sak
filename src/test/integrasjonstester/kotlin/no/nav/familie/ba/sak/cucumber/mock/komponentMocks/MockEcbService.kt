package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.BegrunnelseTeksterStepDefinition
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import java.math.BigDecimal
import java.time.LocalDate

fun mockEcbService(dataFraCucumber: BegrunnelseTeksterStepDefinition): ECBService {
    val ecbService = mockk<ECBService>()
    every { ecbService.hentValutakurs(any(), any()) } answers {
        val valuta = firstArg<String>()
        val dato = secondArg<LocalDate>()

        val valutakurs =
            dataFraCucumber.valutakurs.values.flatten()
                .firstOrNull { valutakurs -> valutakurs.valutakursdato == dato && valutakurs.valutakode == valuta }

        valutakurs?.kurs ?: BigDecimal.valueOf(10)
    }
    return ecbService
}
