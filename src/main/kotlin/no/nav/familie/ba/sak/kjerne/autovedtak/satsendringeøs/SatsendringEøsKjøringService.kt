package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.domene.SatsendringEøsKjøringRepository
import org.springframework.stereotype.Service

@Service
class SatsendringEøsKjøringService(
    private val satsendringEøsKjøringRepository: SatsendringEøsKjøringRepository,
) {
    fun hentSatsendringEøsKjøring(behandlingId: Long): SatsendringEøsKjøring =
        satsendringEøsKjøringRepository.findByBehandlingId(behandlingId)
            ?: throw Feil("Ingen SatsendringEøsKjøring funnet for behandling $behandlingId.")
}
