package no.nav.familie.ba.sak.gdpr.domene

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ba.sak.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.behandling.vilkår.FaktaTilVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.toJson
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.nare.Evaluering
import no.nav.familie.kontrakter.felles.objectMapper
import java.util.*
import javax.persistence.*

@Entity(name = "FødselshendelsePreLansering")
@Table(name = "FOEDSELSHENDELSE_PRE_LANSERING")
data class FødselshendelsePreLansering(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "foedselshendelse_pre_lansering_seq_generator")
        @SequenceGenerator(name = "foedselshendelse_pre_lansering_seq_generator",
                           sequenceName = "foedselshendelse_pre_lansering_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandlingId: Long,

        @Column(name = "person_ident", nullable = false, updatable = false)
        val personIdent: String,

        @Column(name = "ny_behandling_hendelse", nullable = false, updatable = false, columnDefinition = "TEXT")
        val nyBehandlingHendelse: String = "",

        @Column(name = "filtreringsregler_input", columnDefinition = "TEXT")
        val filtreringsreglerInput: String = "",

        @Column(name = "filtreringsregler_output", columnDefinition = "TEXT")
        val filtreringsreglerOutput: String = "",

        @Column(name = "vilkaarsvurderinger_for_foedselshendelse", columnDefinition = "TEXT")
        var vilkårsvurderingerForFødselshendelse: String = "",
) : BaseEntitet() {

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    override fun toString(): String {
        return "FødselshendelsePreLansering(id=$id)"
    }

    fun leggTilVurderingForPerson(faktaTilVilkårsvurdering: FaktaTilVilkårsvurdering, evaluering: Evaluering) {
        val midlertidigVilkårsvurderingerForFødselshendelse =
                if (vilkårsvurderingerForFødselshendelse.isBlank()) VilkårsvurderingerForFødselshendelse()
                else
                    objectMapper.readValue(vilkårsvurderingerForFødselshendelse)

        midlertidigVilkårsvurderingerForFødselshendelse.vurderinger.add(
                VilkårsvurderingForFødselshendelse(faktaTilVilkårsvurdering = faktaTilVilkårsvurdering.toJson(),
                                                   evaluering = evaluering.toJson())
        )

        vilkårsvurderingerForFødselshendelse = midlertidigVilkårsvurderingerForFødselshendelse.toJson()
    }

    fun hentVilkårsvurderingerForFødselshendelse(): VilkårsvurderingerForFødselshendelse {
        return objectMapper.readValue(vilkårsvurderingerForFødselshendelse)
    }
}

fun NyBehandlingHendelse.toJson(): String = objectMapper.writeValueAsString(this)

data class VilkårsvurderingerForFødselshendelse(
        val vurderinger: MutableList<VilkårsvurderingForFødselshendelse> = mutableListOf()
) {

    fun toJson(): String =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
}

data class VilkårsvurderingForFødselshendelse(
        val faktaTilVilkårsvurdering: String,
        val evaluering: String
)