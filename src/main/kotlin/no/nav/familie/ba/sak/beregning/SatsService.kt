package no.nav.familie.ba.sak.beregning

import no.nav.familie.ba.sak.beregning.domene.Sats
import no.nav.familie.ba.sak.beregning.domene.SatsRepository
import no.nav.familie.ba.sak.beregning.domene.SatsType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SatsService(private val satsRepository: SatsRepository) {

    fun hentGyldigSatsFor(type: SatsType, dato: LocalDate): Sats {
        return satsRepository.finnAlleSatserFor(type)
                .first { sats ->
                    when {
                        sats.gyldigFom !== null -> sats.gyldigFom <= dato
                        sats.gyldigTom !== null -> sats.gyldigTom >= dato
                        else -> true
                    }
                }
    }
}