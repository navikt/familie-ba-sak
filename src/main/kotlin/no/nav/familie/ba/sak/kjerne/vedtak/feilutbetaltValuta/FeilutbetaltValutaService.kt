package no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.FeilutbetaltValutaDto
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeilutbetaltValutaService(
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository,
    private val loggService: LoggService,
) {
    private fun finnFeilutbetaltValutaThrows(id: Long): FeilutbetaltValuta = feilutbetaltValutaRepository.finnFeilutbetaltValuta(id) ?: throw Feil("Finner ikke feilutbetalt valuta med id=$id")

    @Transactional
    fun leggTilFeilutbetaltValutaPeriode(
        feilutbetaltValuta: FeilutbetaltValutaDto,
        behandlingId: Long,
    ): Long {
        val lagret =
            feilutbetaltValutaRepository.save(
                FeilutbetaltValuta(
                    behandlingId = behandlingId,
                    fom = feilutbetaltValuta.fom,
                    tom = feilutbetaltValuta.tom,
                    feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp,
                    // TODO: Sjekk om hele erPerMåned logikken kan fjernes nå som vi ikke ønsker å se på det per måned basis (NAV-21272)
                    erPerMåned = false,
                ),
            )
        loggService.loggFeilutbetaltValutaPeriodeLagtTil(behandlingId = behandlingId, feilutbetaltValuta = lagret)
        return lagret.id
    }

    @Transactional
    fun fjernFeilutbetaltValutaPeriode(
        id: Long,
        behandlingId: Long,
    ) {
        loggService.loggFeilutbetaltValutaPeriodeFjernet(
            behandlingId = behandlingId,
            feilutbetaltValuta = finnFeilutbetaltValutaThrows(id),
        )
        feilutbetaltValutaRepository.deleteById(id)
    }

    fun hentFeilutbetaltValutaPerioder(behandlingId: Long) = feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(behandlingId = behandlingId).map { tilDto(it) }

    private fun tilDto(it: FeilutbetaltValuta) =
        FeilutbetaltValutaDto(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            feilutbetaltBeløp = it.feilutbetaltBeløp,
        )

    @Transactional
    fun oppdatertFeilutbetaltValutaPeriode(
        feilutbetaltValutaDto: FeilutbetaltValutaDto,
        id: Long,
    ) {
        val feilutbetaltValuta = feilutbetaltValutaRepository.findById(id).orElseThrow { Feil("Finner ikke feilutbetalt valuta med id=${feilutbetaltValutaDto.id}") }

        feilutbetaltValuta.fom = feilutbetaltValutaDto.fom
        feilutbetaltValuta.tom = feilutbetaltValutaDto.tom
        feilutbetaltValuta.feilutbetaltBeløp = feilutbetaltValutaDto.feilutbetaltBeløp
        feilutbetaltValuta.erPerMåned = false
    }
}
