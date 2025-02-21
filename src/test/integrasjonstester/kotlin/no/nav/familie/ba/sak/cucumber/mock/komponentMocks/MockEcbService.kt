﻿package no.nav.familie.ba.sak.cucumber.mock.komponentMocks

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.VedtaksperioderOgBegrunnelserStepDefinition
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import java.math.BigDecimal
import java.time.LocalDate

fun mockEcbService(dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition): ECBService {
    val ecbService = mockk<ECBService>()
    every { ecbService.hentValutakurs(any(), any()) } answers {
        val valuta = firstArg<String>()
        val dato = secondArg<LocalDate>()

        if (dato.isAfter(dataFraCucumber.dagensDato)) {
            throw IllegalArgumentException("Kan ikke hente valutakurs for dato $dato etter dagens dato ${dataFraCucumber.dagensDato}")
        }

        val valutakurs =
            dataFraCucumber.valutakurs.values
                .flatten()
                .firstOrNull { valutakurs -> valutakurs.valutakursdato == dato && valutakurs.valutakode == valuta }

        valutakurs?.kurs ?: BigDecimal.valueOf(10)
    }
    return ecbService
}
