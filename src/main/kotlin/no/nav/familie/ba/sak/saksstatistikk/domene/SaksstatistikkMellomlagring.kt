package no.nav.familie.ba.sak.saksstatistikk.domene

import no.nav.familie.ba.sak.saksstatistikk.sakstatistikkObjectMapper
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.objectMapper
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "SaksstatistikkMellomlagring")
@Table(name = "SAKSSTATISTIKK_MELLOMLAGRING")
data class SaksstatistikkMellomlagring(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "saksstatistikk_mellomlagring_seq_generator")
    @SequenceGenerator(
        name = "saksstatistikk_mellomlagring_seq_generator",
        sequenceName = "SAKSSTATISTIKK_MELLOMLAGRING_SEQ",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "offset_verdi")
    var offsetVerdi: Long? = null,

    @Column(name = "funksjonell_id")
    val funksjonellId: String,

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: SaksstatistikkMellomlagringType,

    @Column(name = "kontrakt_versjon")
    val kontraktVersjon: String,

    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "json")
    val json: String,

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "konvertert_tid")
    var konvertertTidspunkt: LocalDateTime? = null,

    @Column(name = "sendt_tid")
    var sendtTidspunkt: LocalDateTime? = null,

    @Column(name = "type_id")
    var typeId: Long? = null,
) {
    fun jsonToSakDVH(): SakDVH {
        return sakstatistikkObjectMapper.readValue(json, SakDVH::class.java)
    }

    fun jsonToBehandlingDVH(): BehandlingDVH {
        return sakstatistikkObjectMapper.readValue(json, BehandlingDVH::class.java)
    }

}


enum class SaksstatistikkMellomlagringType {
    SAK,
    BEHANDLING
}


