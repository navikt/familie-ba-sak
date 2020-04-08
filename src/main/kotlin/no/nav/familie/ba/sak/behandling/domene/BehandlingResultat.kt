package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity(name = "BehandlingResultat")
@Table(name = "BEHANDLING_RESULTAT")
data class BehandlingResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_resultat_seq_generator")
        @SequenceGenerator(name = "behandling_resultat_seq_generator",
                           sequenceName = "behandling_resultat_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "behandlingResultat",
                   cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE]
        )
        var personResultater: Set<PersonResultat> = setOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingResultat(id=$id, behandling=${behandling.id})"
    }

    fun hentSamletResultat(): BehandlingResultatType {
        if (personResultater.isEmpty()) {
            return BehandlingResultatType.IKKE_VURDERT
        }

        return when {
            personResultater.all { it.hentSamletResultat() == BehandlingResultatType.INNVILGET } ->
                BehandlingResultatType.INNVILGET
            else ->
                if (behandling.type == BehandlingType.REVURDERING) BehandlingResultatType.OPPHØRT
                else BehandlingResultatType.AVSLÅTT
        }
    }
}

enum class BehandlingResultatType(val brevMal: String) {
    INNVILGET("Innvilget"), DELVIS_INNVILGET("Ukjent"), AVSLÅTT("Avslag"), OPPHØRT("Opphor"), HENLAGT("Ukjent"), IKKE_VURDERT("Ukjent")
}