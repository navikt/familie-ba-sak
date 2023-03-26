package no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingId
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

    private fun finnFeilutbetaltValutaThrows(id: Long): FeilutbetaltValuta {
        return feilutbetaltValutaRepository.finnFeilutbetaltValuta(id)
            ?: throw Feil("Finner ikke feilutbetalt valuta med id=$id")
    }

    @Transactional
    fun leggTilFeilutbetaltValutaPeriode(feilutbetaltValuta: RestFeilutbetaltValuta, behandlingId: BehandlingId): Long {
        val lagret = feilutbetaltValutaRepository.save(
            FeilutbetaltValuta(
                behandlingId = behandlingId,
                fom = feilutbetaltValuta.fom,
                tom = feilutbetaltValuta.tom,
                feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp
            )
        )
        loggService.loggFeilutbetaltValutaPeriodeLagtTil(behandlingId = behandlingId, feilutbetaltValuta = lagret)
        return lagret.id
    }

    @Transactional
    fun fjernFeilutbetaltValutaPeriode(id: Long, behandlingId: BehandlingId) {
        loggService.loggFeilutbetaltValutaPeriodeFjernet(
            behandlingId = behandlingId,
            feilutbetaltValuta = finnFeilutbetaltValutaThrows(id)
        )
        feilutbetaltValutaRepository.deleteById(id)
    }

    fun hentFeilutbetaltValutaPerioder(behandlingId: BehandlingId) =
        feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(behandlingId = behandlingId.id)
            .map { tilRest(it) }

    private fun tilRest(it: FeilutbetaltValuta) =
        RestFeilutbetaltValuta(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            feilutbetaltBeløp = it.feilutbetaltBeløp
        )

    @Transactional
    fun oppdatertFeilutbetaltValutaPeriode(feilutbetaltValuta: RestFeilutbetaltValuta, id: Long) {
        val periode = feilutbetaltValutaRepository.findById(id)
            .orElseThrow { Feil("Finner ikke feilutbetalt valuta med id=${feilutbetaltValuta.id}") }

        periode.fom = feilutbetaltValuta.fom
        periode.tom = feilutbetaltValuta.tom
        periode.feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp
    }
}
