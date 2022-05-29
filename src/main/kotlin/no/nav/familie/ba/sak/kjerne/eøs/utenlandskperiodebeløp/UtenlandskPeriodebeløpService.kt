package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UtenlandskPeriodebeløpService(
    utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val kompetanseService: KompetanseService,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp>>
) : PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse> {
    val skjemaService = PeriodeOgBarnSkjemaService(
        utenlandskPeriodebeløpRepository,
        endringsabonnenter
    )

    fun hentUtenlandskePeriodebeløp(behandlingId: Long) =
        skjemaService.hentMedBehandlingId(behandlingId)

    fun oppdaterUtenlandskPeriodebeløp(behandlingId: Long, utenlandskPeriodebeløp: UtenlandskPeriodebeløp) =
        skjemaService.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)

    fun slettUtenlandskPeriodebeløp(utenlandskPeriodebeløpId: Long) =
        skjemaService.slettSkjema(utenlandskPeriodebeløpId)

    @Transactional
    fun tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId: Long) {
        val gjeldendeKompetanser = kompetanseService.hentKompetanser(behandlingId)

        tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId, gjeldendeKompetanser)
    }

    @Transactional
    override fun skjemaerEndret(behandlingId: Long, endretTil: Collection<Kompetanse>) {
        tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId, endretTil)
    }

    private fun tilpassUtenlandskPeriodebeløpTilKompetanser(
        behandlingId: Long,
        gjeldendeKompetanser: Collection<Kompetanse>
    ) {
        val forrigeUtenlandskePeriodebeløp = hentUtenlandskePeriodebeløp(behandlingId)

        val oppdaterteUtenlandskPeriodebeløp = tilpassUtenlandskePeriodebeløpTilKompetanser(
            forrigeUtenlandskePeriodebeløp,
            gjeldendeKompetanser
        ).medBehandlingId(behandlingId)

        skjemaService.lagreSkjemaDifferanse(forrigeUtenlandskePeriodebeløp, oppdaterteUtenlandskPeriodebeløp)
    }
}
