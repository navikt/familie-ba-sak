package no.nav.familie.ba.sak.behandling.beregning

import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype.*
import no.nav.familie.ba.sak.økonomi.dato
import no.nav.familie.ba.sak.økonomi.årMnd
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.util.ArrayList

internal class YtelseKalkulatorControllerTest {


    @Test
    fun `test at alle perioder opprettes`() {

        val personligeYtelser = listOf(
                PersonligYtelseForPeriode("1", SMÅBARNSTILLEGG, true, årMnd("2020-06"), årMnd("2026-05")),
                PersonligYtelseForPeriode("1", UTVIDET_BARNETRYGD, true, årMnd("2020-10"), årMnd("2022-03")),
                PersonligYtelseForPeriode("2", ORDINÆR_BARNETRYGD, true, årMnd("2020-04"), årMnd("2031-01")),
                PersonligYtelseForPeriode("3", ORDINÆR_BARNETRYGD, false, årMnd("2020-04"), årMnd("2038-03"))
        )

        val controller = YtelseKalkulatorController()

        val ytelseKalkulatorResponse = controller.kalkulerYtelserJson(personligeYtelser).body!!

        assertEquals(215, ytelseKalkulatorResponse.perioder.size)
     }

}