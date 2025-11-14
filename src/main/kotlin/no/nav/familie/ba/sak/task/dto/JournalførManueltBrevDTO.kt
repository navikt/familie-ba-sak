package no.nav.familie.ba.sak.task.dto

import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.mottaker.ManuellAdresseInfo
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerInfo
import no.nav.familie.ba.sak.kjerne.brev.mottaker.tilAvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker

data class JournalførManueltBrevDTO(
    val fagsakId: Long,
    val behandlingId: Long?,
    val manuellBrevRequest: ManueltBrevRequest,
    val mottaker: Mottaker,
    val eksternReferanseId: String,
) {
    data class Mottaker(
        val avsenderMottaker: AvsenderMottaker?,
        val manuellAdresseInfo: ManuellAdresseInfo?,
    ) {
        companion object {
            fun opprettFra(mottakerInfo: MottakerInfo): Mottaker =
                Mottaker(
                    avsenderMottaker = mottakerInfo.tilAvsenderMottaker(),
                    manuellAdresseInfo = mottakerInfo.manuellAdresseInfo,
                )
        }
    }
}
