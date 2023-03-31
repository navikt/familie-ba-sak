package no.nav.familie.ba.sak.kjerne.behandling.domene

import javax.persistence.AttributeConverter
import javax.persistence.Convert
import javax.persistence.Converter
import javax.persistence.Embeddable

@Embeddable
data class BehandlingId(
    @Convert(converter = BehandlingIdConverter::class)
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
