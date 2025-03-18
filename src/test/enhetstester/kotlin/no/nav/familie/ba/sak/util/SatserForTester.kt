package no.nav.familie.ba.sak.util

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.Sats
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import java.time.LocalDate

fun sisteUtvidetSatsTilTester(): Int = SatsService.finnSisteSatsFor(SatsType.UTVIDET_BARNETRYGD).beløp

fun sisteSmåbarnstilleggSatsTilTester(): Int = SatsService.finnSisteSatsFor(SatsType.SMA).beløp

fun ordinærSatsNesteMånedTilTester(): Sats =
    SatsService.finnAlleSatserFor(SatsType.ORBA).findLast {
        it.gyldigFom.toYearMonth() <= LocalDate.now().nesteMåned()
    }!!
