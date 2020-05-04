package no.nav.familie.ba.sak.journalføring.metadata

import no.nav.familie.ba.sak.journalføring.domene.DokumentType
import org.springframework.stereotype.Component

@Component
class DokarkivMetadata(vararg dokumentMetadata: DokumentMetadata) {

    val metadata: Map<String, DokumentMetadata> = dokumentMetadata.associateBy { it.dokumentTypeId }

    fun getMetadata(dokumentType: String): DokumentMetadata {
        return metadata[dokumentType] ?: error("Ukjent dokumenttype $dokumentType")
    }
}