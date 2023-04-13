package no.nav.familie.ba.sak.cucumber.domeneparser

import java.util.UUID

object IdTIlUUIDHolder {

    val behandlingIdTilUUID = (1..10).associateWith { UUID.randomUUID() }

    fun behandlingIdFraUUID(id: UUID) = behandlingIdTilUUID.entries.single { it.value == id }.key
}
