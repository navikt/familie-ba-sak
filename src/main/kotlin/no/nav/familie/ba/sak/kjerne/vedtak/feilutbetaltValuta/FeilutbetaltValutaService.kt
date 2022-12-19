package no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeilutbetaltValutaService(
    @Autowired
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository,

    @Autowired
    private val loggService: LoggService
) {
    @Transactional
    fun leggTilFeilutbetaltValutaPeriode(feilutbetaltValuta: RestFeilutbetaltValuta, behandlingId: Long): Long {
        val lagret = feilutbetaltValutaRepository.save(
            FeilutbetaltValuta(
                behandlingId = behandlingId,
                fom = feilutbetaltValuta.fom,
                tom = feilutbetaltValuta.tom,
                feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp
            )
        )
        loggService.loggFeilutbetaltValutaLagtTil(behandlingId = behandlingId)
        return lagret.id
    }

    @Transactional
    fun fjernFeilutbetaltValutaPeriode(id: Long, behandlingId: Long) {
        feilutbetaltValutaRepository.deleteById(id)
        loggService.loggFeilutbetaltValutaFjernet(behandlingId = behandlingId)
    }

    fun hentFeilutbetaltValutaPerioder(behandlingId: Long) =
        feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(behandlingId = behandlingId).map { tilRest(it) }

    private fun tilRest(it: FeilutbetaltValuta) =
        RestFeilutbetaltValuta(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            feilutbetaltBeløp = it.feilutbetaltBeløp
        )

    @Transactional
    fun oppdaterFeilutbetaltValutaPeriode(feilutbetaltValuta: RestFeilutbetaltValuta, id: Long) {
        val periode = feilutbetaltValutaRepository.findById(id).orElseThrow { Feil("Finner ikke feilutbetalt valuta med id=${feilutbetaltValuta.id}") }

        periode.fom = feilutbetaltValuta.fom
        periode.tom = feilutbetaltValuta.tom
        periode.feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp

        feilutbetaltValutaRepository.save(periode)
    }
}
