package no.nav.familie.ba.sak.kjerne.personident

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

// Verner aktorv2-konsumenten mot endringer som knekker Avro-deserialisering: avhengighetsbump
// (avro 1.12.2 feilet med SecurityException i prod) og simulert skjemaevolusjon fra PDL.
class AktorV2AvroSerdeTest {
    private val topic = "pdl.aktor-v2"

    // Samme verdi-serde-oppsett som kafkaAivenHendelseListenerAvroLatestContainerFactory i KafkaAivenConfig
    private val config =
        mapOf(
            "schema.registry.url" to "mock://$REGISTRY_SCOPE",
            "specific.avro.reader" to true,
        )
    private val serializer = KafkaAvroSerializer().apply { configure(config, false) }
    private val deserializer = KafkaAvroDeserializer().apply { configure(config, false) }

    @Test
    fun `skal serialisere og deserialisere Aktor med samme oppsett som aktorv2-konsumenten`() {
        // Arrange
        val aktor = Aktor(listOf(Identifikator("1234567890123", Type.AKTORID, true)))

        // Act
        val deserialisert = deserializer.deserialize(topic, serializer.serialize(topic, aktor))

        // Assert
        assertEquals(aktor, deserialisert)
    }

    @Test
    fun `skal deserialisere tombstone til null`() {
        // Act
        val deserialisert = deserializer.deserialize(topic, null)

        // Assert
        assertNull(deserialisert)
    }

    @Test
    fun `skal tåle at PDL legger til nytt felt i skjemaet`() {
        // Arrange
        val writerSkjema = writerSkjema(typeSymboler = listOf("FOLKEREGISTERIDENT", "AKTORID", "NPID"), medNyttFelt = true)
        val melding = genericAktor(writerSkjema, enumVerdi = "AKTORID")

        // Act
        val deserialisert = deserializer.deserialize(topic, serializer.serialize(topic, melding))

        // Assert
        assertEquals(Aktor(listOf(Identifikator("1234567890123", Type.AKTORID, true))), deserialisert)
    }

    @Test
    fun `skal deserialisere ukjent identtype fra PDL til UKJENT`() {
        // Arrange
        val writerSkjema = writerSkjema(typeSymboler = listOf("FOLKEREGISTERIDENT", "AKTORID", "NPID", "MIDLERTIDIG_ID"), medNyttFelt = false)
        val melding = genericAktor(writerSkjema, enumVerdi = "MIDLERTIDIG_ID")

        // Act
        val deserialisert = deserializer.deserialize(topic, serializer.serialize(topic, melding))

        // Assert
        assertEquals(Aktor(listOf(Identifikator("1234567890123", Type.UKJENT, true))), deserialisert)
    }

    // Skjemaet slik PDL kan komme til å skrive det, bygget programmatisk slik at testene
    // ikke er avhengige av eksakt JSON-emisjon fra Schema.toString()
    private fun writerSkjema(
        typeSymboler: List<String>,
        medNyttFelt: Boolean,
    ): Schema {
        val namespace = "no.nav.person.pdl.aktor.v2"
        val typeSkjema = Schema.createEnum("Type", null, namespace, typeSymboler)
        val identifikatorFelter =
            mutableListOf(
                Schema.Field("idnummer", Schema.create(Schema.Type.STRING)),
                Schema.Field("type", typeSkjema),
                Schema.Field("gjeldende", Schema.create(Schema.Type.BOOLEAN)),
            )
        if (medNyttFelt) {
            identifikatorFelter.add(Schema.Field("nyttFelt", Schema.create(Schema.Type.STRING)))
        }
        val identifikatorSkjema = Schema.createRecord("Identifikator", null, namespace, false, identifikatorFelter)
        return Schema.createRecord(
            "Aktor",
            null,
            namespace,
            false,
            listOf(Schema.Field("identifikatorer", Schema.createArray(identifikatorSkjema))),
        )
    }

    private fun genericAktor(
        writerSkjema: Schema,
        enumVerdi: String,
    ): GenericRecord {
        val identifikatorSkjema = writerSkjema.getField("identifikatorer").schema().elementType
        val identifikator =
            GenericData.Record(identifikatorSkjema).apply {
                put("idnummer", "1234567890123")
                put("type", GenericData.EnumSymbol(identifikatorSkjema.getField("type").schema(), enumVerdi))
                put("gjeldende", true)
                if (identifikatorSkjema.getField("nyttFelt") != null) {
                    put("nyttFelt", "ny verdi")
                }
            }
        return GenericData.Record(writerSkjema).apply { put("identifikatorer", listOf(identifikator)) }
    }

    companion object {
        private const val REGISTRY_SCOPE = "aktor-v2-serde-test"

        @JvmStatic
        @AfterAll
        fun ryddOppMockSchemaRegistry() {
            MockSchemaRegistry.dropScope(REGISTRY_SCOPE)
        }
    }
}
