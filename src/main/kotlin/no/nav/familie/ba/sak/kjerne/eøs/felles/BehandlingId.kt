package no.nav.familie.ba.sak.kjerne.eøs.felles

import java.util.UUID

data class BehandlingId(
    val id: Long,
    val korrelasjonsid: UUID = UUID.randomUUID()
)
