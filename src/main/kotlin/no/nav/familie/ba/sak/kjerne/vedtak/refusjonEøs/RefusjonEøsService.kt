package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RefusjonEøsDto
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefusjonEøsService(
    @Autowired
    private val refusjonEøsRepository: RefusjonEøsRepository,
    @Autowired
    private val loggService: LoggService,
) {
    private fun hentRefusjonEøs(id: Long): RefusjonEøs =
        refusjonEøsRepository.finnRefusjonEøs(id)
            ?: throw Feil("Finner ikke refusjon eøs med id=$id")

    @Transactional
    fun leggTilRefusjonEøsPeriode(
        refusjonEøs: RefusjonEøsDto,
        behandlingId: Long,
    ): Long {
        val lagretPeriode =
            refusjonEøsRepository.save(
                RefusjonEøs(
                    behandlingId = behandlingId,
                    fom = refusjonEøs.fom,
                    tom = refusjonEøs.tom,
                    refusjonsbeløp = refusjonEøs.refusjonsbeløp,
                    land = refusjonEøs.land,
                    refusjonAvklart = refusjonEøs.refusjonAvklart,
                ),
            )
        loggService.loggRefusjonEøsPeriodeLagtTil(refusjonEøs = lagretPeriode)
        return lagretPeriode.id
    }

    @Transactional
    fun fjernRefusjonEøsPeriode(
        id: Long,
    ) {
        loggService.loggRefusjonEøsPeriodeFjernet(
            refusjonEøs = hentRefusjonEøs(id),
        )
        refusjonEøsRepository.deleteById(id)
    }

    fun hentRefusjonEøsPerioder(behandlingId: Long) =
        refusjonEøsRepository
            .finnRefusjonEøsForBehandling(behandlingId = behandlingId)
            .map { tilDto(it) }

    private fun tilDto(it: RefusjonEøs) =
        RefusjonEøsDto(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            refusjonsbeløp = it.refusjonsbeløp,
            land = it.land,
            refusjonAvklart = it.refusjonAvklart,
        )

    @Transactional
    fun oppdaterRefusjonEøsPeriode(
        refusjonEøsDto: RefusjonEøsDto,
        id: Long,
    ) {
        val refusjonEøs = hentRefusjonEøs(id)

        refusjonEøs.fom = refusjonEøsDto.fom
        refusjonEøs.tom = refusjonEøsDto.tom
        refusjonEøs.refusjonsbeløp = refusjonEøsDto.refusjonsbeløp
        refusjonEøs.land = refusjonEøsDto.land
        refusjonEøs.refusjonAvklart = refusjonEøsDto.refusjonAvklart
    }

    fun harRefusjonEøsPåBehandling(behandlingId: Long): Boolean = refusjonEøsRepository.finnRefusjonEøsForBehandling(behandlingId).isNotEmpty()
}
