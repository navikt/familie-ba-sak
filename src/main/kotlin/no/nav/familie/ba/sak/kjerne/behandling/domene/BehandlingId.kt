package no.nav.familie.ba.sak.kjerne.behandling.domene

import javax.persistence.AttributeConverter
import javax.persistence.Converter

data class BehandlingId(
    val id: Long
)

@Converter
class BehandlingIdConverter :
    AttributeConverter<BehandlingId, Long> {

    override fun convertToDatabaseColumn(behandlingId: BehandlingId) =
        behandlingId.id

    override fun convertToEntityAttribute(id: Long): BehandlingId =
        BehandlingId(id)
}
