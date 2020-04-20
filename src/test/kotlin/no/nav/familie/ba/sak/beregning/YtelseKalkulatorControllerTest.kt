package no.nav.familie.ba.sak.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.vedtak.YtelseType.*
import no.nav.familie.ba.sak.beregning.PersonligYtelseForPeriode
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.beregning.YtelseKalkulatorController
import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import no.nav.familie.ba.sak.common.årMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class YtelseKalkulatorControllerTest {


    val satsRepository = mockk<SatsRepository>()
    val satsService = SatsService(satsRepository)

    @BeforeEach
    fun init() {
        every { satsRepository.finnAlleSatserFor(SatsType.SMA) } returns listOf(Sats(0, SatsType.SMA, 660, LocalDate.now(), null))
        every { satsRepository.finnAlleSatserFor(SatsType.ORBA) } returns listOf(Sats(0,
                                                                                      SatsType.ORBA,
                                                                                      1054,
                                                                                      LocalDate.now(),
                                                                                      null))
    }

    @Test
    fun `test at alle perioder opprettes`() {

        val personligeYtelser = listOf(
                PersonligYtelseForPeriode("1",
                                          SMÅBARNSTILLEGG,
                                          true,
                                          årMnd("2020-06"),
                                          årMnd("2026-05")),
                PersonligYtelseForPeriode("1",
                                          UTVIDET_BARNETRYGD,
                                          true,
                                          årMnd("2020-10"),
                                          årMnd("2022-03")),
                PersonligYtelseForPeriode("2",
                                          ORDINÆR_BARNETRYGD,
                                          true,
                                          årMnd("2020-04"),
                                          årMnd("2031-01")),
                PersonligYtelseForPeriode("3",
                                          ORDINÆR_BARNETRYGD,
                                          false,
                                          årMnd("2020-04"),
                                          årMnd("2038-03"))
        )

        val controller = YtelseKalkulatorController(satsService)

        val ytelseKalkulatorResponse = controller.kalkulerYtelserJson(personligeYtelser).body!!.data!!

        assertEquals(215, ytelseKalkulatorResponse.perioder.size)
    }

}