package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.initSteg
import no.nav.familie.ba.sak.behandling.vilkår.SamletVilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.common.BaseEntitet
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

        @Enumerated(EnumType.STRING)
        @Column(name = "brev", nullable = false)
        var brev: BrevType = BrevType.IKKE_VURDERT,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @OneToMany(mappedBy = "samletVilkårResultat", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        var samletVilkårResultat: MutableSet<SamletVilkårResultat>


) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingResultat(id=$id, behandling=${behandling.id}, brev=${brev})"
    }
}

fun BrevType.toDokGenTemplate(): String {
    return when (this) {
        BrevType.INNVILGET -> "Innvilget"
        BrevType.AVSLÅTT -> "Avslag"
        BrevType.OPPHØRT -> "Opphor"
        else -> error("Invalid/Unsupported vedtak result")
    }
}
enum class BrevType {
    IKKE_VURDERT, INNVILGET, AVSLÅTT, OPPHØRT, HENLAGT
}
