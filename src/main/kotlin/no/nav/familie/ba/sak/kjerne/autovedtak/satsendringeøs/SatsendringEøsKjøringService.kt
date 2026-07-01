package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøringRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class SatsendringEøsKjøringService(
    private val satsendringEøsKjøringRepository: SatsendringEøsKjøringRepository,
) {
    fun hentSatsendringEøsKjøring(behandlingId: Long): SatsendringEøsKjøring =
        satsendringEøsKjøringRepository.findByBehandlingId(behandlingId)
            ?: throw Feil("Ingen SatsendringEøsKjøring funnet for behandling $behandlingId.")

    fun hentSatsendringEøsKjøring(
        fagsakId: Long,
        utbetalingsland: String,
        satsTidspunkt: YearMonth,
    ): SatsendringEøsKjøring =
        satsendringEøsKjøringRepository.findByFagsakIdAndUtbetalingslandAndSatsTidspunkt(fagsakId, utbetalingsland, satsTidspunkt)
            ?: throw Feil("Ingen SatsendringEøsKjøring funnet for fagsak $fagsakId, utbetalingsland $utbetalingsland og tidspunkt $satsTidspunkt.")

    fun settBehandlingId(
        fagsakId: Long,
        utbetalingsland: String,
        satsTidspunkt: YearMonth,
        behandlingId: Long,
    ) {
        val kjøring = hentSatsendringEøsKjøring(fagsakId, utbetalingsland, satsTidspunkt)
        kjøring.behandlingId = behandlingId
        satsendringEøsKjøringRepository.save(kjøring)
    }

    fun settFerdigTidspunkt(
        fagsakId: Long,
        utbetalingsland: String,
        satsTidspunkt: YearMonth,
    ) {
        val kjøring = hentSatsendringEøsKjøring(fagsakId, utbetalingsland, satsTidspunkt)
        kjøring.ferdigTidspunkt = LocalDateTime.now()
        satsendringEøsKjøringRepository.save(kjøring)
    }

    fun settFeiltype(
        fagsakId: Long,
        utbetalingsland: String,
        satsTidspunkt: YearMonth,
        feiltype: String,
    ) {
        val kjøring = hentSatsendringEøsKjøring(fagsakId, utbetalingsland, satsTidspunkt)
        kjøring.feiltype = feiltype
        satsendringEøsKjøringRepository.save(kjøring)
    }
}
