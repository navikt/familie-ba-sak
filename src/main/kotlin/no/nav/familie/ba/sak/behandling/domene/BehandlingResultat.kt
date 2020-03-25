package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.vilkår.PeriodeResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.nare.core.evaluations.Resultat
import javax.persistence.*

@Entity(name = "BehandlingResultat")
@Table(name = "BEHANDLING_RESULTAT")
data class BehandlingResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_resultat_seq_generator")
        @SequenceGenerator(name = "behandling_resultat_seq_generator", sequenceName = "behandling_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @OneToMany(mappedBy = "behandlingResultat", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        var periodeResultater: MutableSet<PeriodeResultat> = mutableSetOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingResultat(id=$id, behandling=${behandling.id})"
    }

    fun hentSamletResultat(): BehandlingResultatType {
        return when {
            periodeResultater.all { it.hentSamletResultat() == BehandlingResultatType.INNVILGET } -> {
                BehandlingResultatType.INNVILGET
            }
            else -> BehandlingResultatType.AVSLÅTT
        }
    }
}

enum class BehandlingResultatType {
    INNVILGET, DELVIS_INNVILGET, AVSLÅTT, OPPHØRT, HENLAGT
}