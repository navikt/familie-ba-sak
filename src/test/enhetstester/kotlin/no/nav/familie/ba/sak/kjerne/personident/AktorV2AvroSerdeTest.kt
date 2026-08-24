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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AktorV2AvroSerdeTest {
    private val registryScope = "aktor-v2-serde-test"
    private val config =
        mapOf(
            "schema.registry.url" to "mock://$registryScope",
            "specific.avro.reader" to true,
        )
    private val serializer = KafkaAvroSerializer().apply { configure(config, false) }
    private val deserializer = KafkaAvroDeserializer().apply { configure(config, false) }

    @AfterAll
    fun ryddOppMockSchemaRegistry() {
        MockSchemaRegistry.dropScope(registryScope)
    }

    @Test
    fun `skal serialisere og deserialisere en Aktor med oppsettet til aktorv2-konsumenten`() {
        val aktor = Aktor(listOf(Identifikator("1234567890123", Type.AKTORID, true)))

        val bytes = serializer.serialize(PDL_AKTOR_V2_TOPIC, aktor)
        val deserialisert = deserializer.deserialize(PDL_AKTOR_V2_TOPIC, bytes)

        assertEquals(aktor, deserialisert)
    }

    @Test
    fun `skal deserialisere en tombstone-melding til null`() {
        val deserialisert = deserializer.deserialize(PDL_AKTOR_V2_TOPIC, null)

        assertNull(deserialisert)
    }

    @Test
    fun `skal tåle at PDL legger til et nytt felt i skjemaet`() {
        val melding = lagAktorMelding(medNyttFelt = true)

        val bytes = serializer.serialize(PDL_AKTOR_V2_TOPIC, melding)
        val deserialisert = deserializer.deserialize(PDL_AKTOR_V2_TOPIC, bytes)

        assertEquals(Aktor(listOf(Identifikator("1234567890123", Type.AKTORID, true))), deserialisert)
    }

    @Test
    fun `skal feile når PDL tar i bruk en ny identtype siden Type-enumen ikke har noen default`() {
        val melding =
            lagAktorMelding(
                typeSymboler = listOf("MIDLERTIDIG_ID", "FOLKEREGISTERIDENT", "AKTORID", "NPID"),
                type = "MIDLERTIDIG_ID",
            )
        val bytes = serializer.serialize(PDL_AKTOR_V2_TOPIC, melding)

        val feil = assertThrows<SerializationException> { deserializer.deserialize(PDL_AKTOR_V2_TOPIC, bytes) }

        assertTrue(feil.årsaksmeldinger().any { it.contains("MIDLERTIDIG_ID") }) {
            "Forventet oppløsningsfeil for ukjent enum-symbol, fikk: $feil"
        }
    }

    @Test
    fun `skal feile når PDL fjerner et felt fra skjemaet`() {
        val melding = lagAktorMelding(utenGjeldende = true)
        val bytes = serializer.serialize(PDL_AKTOR_V2_TOPIC, melding)

        val feil = assertThrows<SerializationException> { deserializer.deserialize(PDL_AKTOR_V2_TOPIC, bytes) }

        assertTrue(feil.årsaksmeldinger().any { it.contains("gjeldende") }) {
            "Forventet oppløsningsfeil for manglende felt 'gjeldende', fikk: $feil"
        }
    }

    // Bygger en Aktor-melding slik den ser ut med PDLs (potensielt endrede) writer-skjema.
    // Skjemaet ligger i meldingen, så testene trenger ikke forholde seg til det separat.
    private fun lagAktorMelding(
        typeSymboler: List<String> = listOf("FOLKEREGISTERIDENT", "AKTORID", "NPID"),
        type: String = "AKTORID",
        medNyttFelt: Boolean = false,
        utenGjeldende: Boolean = false,
    ): GenericRecord {
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
        val aktorSkjema =
            Schema.createRecord(
                "Aktor",
                null,
                namespace,
                false,
                listOf(Schema.Field("identifikatorer", Schema.createArray(identifikatorSkjema))),
            )

        val identifikator =
            GenericData.Record(identifikatorSkjema).apply {
                put("idnummer", "1234567890123")
                put("type", GenericData.EnumSymbol(typeSkjema, type))
                if (!utenGjeldende) put("gjeldende", true)
                if (medNyttFelt) put("nyttFelt", "ny verdi")
            }
        return GenericData.Record(aktorSkjema).apply { put("identifikatorer", listOf(identifikator)) }
    }

    private fun Throwable.årsaksmeldinger(): Sequence<String> = generateSequence(this) { it.cause }.map { it.message.orEmpty() }
}
