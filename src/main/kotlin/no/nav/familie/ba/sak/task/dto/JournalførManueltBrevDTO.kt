package no.nav.familie.ba.sak.task.dto

import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerInfo

data class JournalførManueltBrevDTO(
    val fagsakId: Long,
    val behandlingId: Long?,
    val manuellBrevRequest: ManueltBrevRequest,
    val mottakerInfo: MottakerInfo,
    val eksternReferanseId: String,
)
