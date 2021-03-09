package no.nav.familie.ba.sak.brev

import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.brev.domene.maler.BrevPeriode
import no.nav.familie.ba.sak.brev.domene.maler.PeriodeType
import org.junit.jupiter.api.Test

class BrevPeriodeUtilTest {

    @Test
    fun `sorterBrevPerioderEtterFomDato gir brevperioder i riktig rekkefølge`() {
        val datoer = listOf("1. juli 2019", "1. september 2020", "1. april 2020", "1. mars 2021")
        val brevtyper = listOf(PeriodeType.INNVILGELSE, PeriodeType.OPPHOR, PeriodeType.INNVILGELSE, PeriodeType.OPPHOR)

        val brevperioder =
                datoer.mapIndexed { index, dato -> BrevPeriode(fom = dato, begrunnelser = listOf(""), type = brevtyper[index]) }

        val sorterteBrevPerioder = sorterBrevPerioderEtterFomDato(brevperioder)

        assertEquals(listOf("1. juli 2019", "1. april 2020", "1. september 2020", "1. mars 2021"),
                     sorterteBrevPerioder.map { it.fom[0] })

    }
}