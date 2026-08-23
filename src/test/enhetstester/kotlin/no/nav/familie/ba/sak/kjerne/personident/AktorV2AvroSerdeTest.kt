package no.nav.familie.ba.sak.kjerne.personident

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.familie.ba.sak.config.KafkaAivenConfig.Companion.PDL_AKTOR_V2_TOPIC
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.errors.SerializationException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// Verner aktorv2-konsumenten mot endringer som knekker Avro-deserialisering: avhengighetsbump
// (avro 1.12.2 feilet med SecurityException i prod) og simulert skjemaevolusjon fra PDL.
class AktorV2AvroSerdeTest {
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
        val bytes = serializer.serialize(PDL_AKTOR_V2_TOPIC, aktor)
        val deserialisert = deserializer.deserialize(PDL_AKTOR_V2_TOPIC, bytes)

        // Assert
        assertEquals(aktor, deserialisert)
    }

    @Test
    fun `skal deserialisere tombstone til null`() {
        // Act
        val deserialisert = deserializer.deserialize(PDL_AKTOR_V2_TOPIC, null)

        // Assert
        assertNull(deserialisert)
    }

    @Test
    fun `skal tåle at PDL legger til nytt felt og ny identtype i skjemaet`() {
        // Arrange — ukjent symbol først i writer-lista, slik at kjente symboler får andre indekser
        // enn hos leseren og navnebasert enum-oppløsning faktisk utøves
        val writerSkjema =
            writerSkjema(
                typeSymboler = listOf("MIDLERTIDIG_ID", "FOLKEREGISTERIDENT", "AKTORID", "NPID"),
                medNyttFelt = true,
            )
        val melding = genericAktor(writerSkjema, "MIDLERTIDIG_ID", "AKTORID")

        // Act
        val bytes = serializer.serialize(PDL_AKTOR_V2_TOPIC, melding)
        val deserialisert = deserializer.deserialize(PDL_AKTOR_V2_TOPIC, bytes)

        // Assert
        assertEquals(
            Aktor(
                listOf(
                    Identifikator("1234567890123", Type.UKJENT, true),
                    Identifikator("1234567890123", Type.AKTORID, true),
                ),
            ),
            deserialisert,
        )
    }

    @Test
    fun `skal feile på melding der PDL har fjernet et felt`() {
        // Arrange
        val writerSkjema =
            writerSkjema(
                typeSymboler = listOf("FOLKEREGISTERIDENT", "AKTORID", "NPID"),
                utenGjeldende = true,
            )
        val bytes = serializer.serialize(PDL_AKTOR_V2_TOPIC, genericAktor(writerSkjema, "AKTORID"))

        // Act
        val feil = assertThrows<SerializationException> { deserializer.deserialize(PDL_AKTOR_V2_TOPIC, bytes) }

        // Assert
        val årsaker = generateSequence(feil as Throwable) { it.cause }.map { it.message.orEmpty() }
        assertTrue(årsaker.any { it.contains("gjeldende") }) {
            "Forventet oppløsningsfeil for manglende felt 'gjeldende', fikk: $feil"
        }
    }

    @Test
    fun `Type-enumen skal ha UKJENT som reader-default`() {
        // Arrange
        val typeSkjema =
            Aktor
                .getClassSchema()
                .getField("identifikatorer")
                .schema()
                .elementType
                .getField("type")
                .schema()

        // Assert
        assertEquals("UKJENT", typeSkjema.enumDefault) {
            "AktorV2.avdl avviker bevisst fra PDL sitt skjema: UKJENT må stå som default ('= UKJENT;') " +
                "for at nye identtyper fra PDL ikke skal knekke konsumenten"
        }
    }

    // Skjemaet slik PDL kan komme til å skrive det, bygget programmatisk slik at testene ikke er
    // avhengige av eksakt JSON-emisjon fra Schema.toString()
    private fun writerSkjema(
        typeSymboler: List<String>,
        medNyttFelt: Boolean = false,
        utenGjeldende: Boolean = false,
    ): Schema {
        val namespace = "no.nav.person.pdl.aktor.v2"
        val typeSkjema = Schema.createEnum("Type", null, namespace, typeSymboler)
        val identifikatorFelter =
            buildList {
                add(Schema.Field("idnummer", Schema.create(Schema.Type.STRING)))
                add(Schema.Field("type", typeSkjema))
                if (!utenGjeldende) add(Schema.Field("gjeldende", Schema.create(Schema.Type.BOOLEAN)))
                if (medNyttFelt) add(Schema.Field("nyttFelt", Schema.create(Schema.Type.STRING)))
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

    // Setter kun feltene som finnes i writer-skjemaet, slik at feltvalg bare uttrykkes i writerSkjema()
    private fun genericAktor(
        writerSkjema: Schema,
        vararg enumVerdier: String,
    ): GenericRecord {
        val identifikatorSkjema = writerSkjema.getField("identifikatorer").schema().elementType
        val identifikatorer =
            enumVerdier.map { enumVerdi ->
                GenericData.Record(identifikatorSkjema).apply {
                    put("type", GenericData.EnumSymbol(identifikatorSkjema.getField("type").schema(), enumVerdi))
                    identifikatorSkjema.getField("idnummer")?.let { put("idnummer", "1234567890123") }
                    identifikatorSkjema.getField("gjeldende")?.let { put("gjeldende", true) }
                    identifikatorSkjema.getField("nyttFelt")?.let { put("nyttFelt", "ny verdi") }
                }
            }
        return GenericData.Record(writerSkjema).apply { put("identifikatorer", identifikatorer) }
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
