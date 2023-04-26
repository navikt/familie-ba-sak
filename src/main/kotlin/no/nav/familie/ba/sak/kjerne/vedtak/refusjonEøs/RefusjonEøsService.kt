package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefusjonEøsService(
    @Autowired
    private val refusjonEøsRepository: RefusjonEøsRepository,

    @Autowired
    private val loggService: LoggService
) {

    private fun finnRefusjonEøsThrows(id: Long): RefusjonEøs {
        return refusjonEøsRepository.finnRefusjonEøs(id)
            ?: throw Feil("Finner ikke refusjon eøs med id=$id")
    }

    @Transactional
    fun leggTilRefusjonEøsPeriode(refusjonEøs: RestRefusjonEøs, behandlingId: Long): Long {
        val lagret = refusjonEøsRepository.save(
            RefusjonEøs(
                behandlingId = behandlingId,
                fom = refusjonEøs.fom,
                tom = refusjonEøs.tom,
                refusjonsbeløp = refusjonEøs.refusjonsbeløp,
                land = refusjonEøs.land,
                refusjonAvklart = refusjonEøs.refusjonAvklart
            )
        )
        loggService.loggRefusjonEøsPeriodeLagtTil(behandlingId = behandlingId, refusjonEøs = lagret)
        return lagret.id
    }

    @Transactional
    fun fjernRefusjonEøsPeriode(id: Long, behandlingId: Long) {
        loggService.loggRefusjonEøsPeriodeFjernet(
            behandlingId = behandlingId,
            refusjonEøs = finnRefusjonEøsThrows(id)
        )
        refusjonEøsRepository.deleteById(id)
    }

    fun hentRefusjonEøsPerioder(behandlingId: Long) =
        refusjonEøsRepository.finnRefusjonEøsForBehandling(behandlingId = behandlingId)
            .map { tilRest(it) }

    private fun tilRest(it: RefusjonEøs) =
        RestRefusjonEøs(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            refusjonsbeløp = it.refusjonsbeløp,
            land = it.land,
            refusjonAvklart = it.refusjonAvklart
        )

    @Transactional
    fun oppdaterRefusjonEøsPeriode(refusjonEøs: RestRefusjonEøs, id: Long) {
        val periode = refusjonEøsRepository.findById(id)
            .orElseThrow { Feil("Finner ikke refusjon eøs med id=${refusjonEøs.id}") }

        periode.fom = refusjonEøs.fom
        periode.tom = refusjonEøs.tom
        periode.refusjonsbeløp = refusjonEøs.refusjonsbeløp
        periode.land = refusjonEøs.land
        periode.refusjonAvklart = refusjonEøs.refusjonAvklart
    }
}
