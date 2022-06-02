package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UtenlandskPeriodebeløpService(
    utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val kompetanseService: KompetanseService
) {
    val skjemaService = PeriodeOgBarnSkjemaService(utenlandskPeriodebeløpRepository)

    fun hentUtenlandskePeriodebeløp(behandlingId: Long) =
        skjemaService.hentMedBehandlingId(behandlingId)

    fun oppdaterUtenlandskPeriodebeløp(behandlingId: Long, utenlandskPeriodebeløp: UtenlandskPeriodebeløp) =
        skjemaService.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)

    fun slettUtenlandskPeriodebeløp(utenlandskPeriodebeløpId: Long) =
        skjemaService.slettSkjema(utenlandskPeriodebeløpId)

    @Transactional
    fun tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId: Long) {
        val forrigeUtenlandskePeriodebeløp = hentUtenlandskePeriodebeløp(behandlingId)
        val gjeldendeKompetanser = kompetanseService.hentKompetanser(behandlingId)

        val oppdaterteUtenlandskPeriodebeløp = tilpassUtenlandskePeriodebeløpTilKompetanser(
            forrigeUtenlandskePeriodebeløp,
            gjeldendeKompetanser
        ).medBehandlingId(behandlingId)

        skjemaService.lagreSkjemaDifferanse(forrigeUtenlandskePeriodebeløp, oppdaterteUtenlandskPeriodebeløp)
    }

    @Transactional
    fun kopierOgErstattUtenlandskPeriodebeløp(fraBehandlingId: Long, tilBehandlingId: Long) =
        skjemaService.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)
}
