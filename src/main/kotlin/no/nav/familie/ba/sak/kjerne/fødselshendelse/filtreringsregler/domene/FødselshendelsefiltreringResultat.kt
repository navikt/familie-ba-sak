package no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.StringListConverter
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "FødselshendelsefiltreringResultat")
@Table(name = "FOEDSELSHENDELSEFILTRERING_RESULTAT")
class FødselshendelsefiltreringResultat(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "foedselshendelsefiltrering_resultat_seq_generator")
    @SequenceGenerator(
        name = "foedselshendelsefiltrering_resultat_seq_generator",
        sequenceName = "foedselshendelsefiltrering_resultat_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", nullable = false, updatable = false, unique = true)
    val behandlingId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "filtreringsregel")
    val filtreringsregel: Filtreringsregel,

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat")
    val resultat: Resultat,

    @Column(name = "begrunnelse", columnDefinition = "TEXT", nullable = false)
    val begrunnelse: String,

    @Column(name = "evalueringsaarsaker")
    @Convert(converter = StringListConverter::class)
    val evalueringsårsaker: List<String> = emptyList(),

    @Column(name = "regel_input", columnDefinition = "TEXT")
    val regelInput: String? = null,
) : BaseEntitet() {

    override fun toString(): String {
        return "FødselshendelsefiltreringResultat(" +
            "id=$id, " +
            "filtreringsregel=$filtreringsregel, " +
            "resultat=$resultat, " +
            "evalueringÅrsaker=$evalueringsårsaker" +
            ")"
    }
}

fun List<FødselshendelsefiltreringResultat>.erOppfylt() = this.all { it.resultat == Resultat.OPPFYLT }
