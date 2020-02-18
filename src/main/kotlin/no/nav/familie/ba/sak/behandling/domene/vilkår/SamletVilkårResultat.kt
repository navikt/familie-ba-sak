package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity
@Table(name = "samlet_vilkar_resultat")
class SamletVilkårResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "samlet_vilkar_resultat_seq")
        @SequenceGenerator(name = "samlet_vilkar_resultat_seq")
        private val id: Long? = null,

        @Column(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandlingId: Long,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @OneToMany(mappedBy = "samletVilkårResultat", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        val samletVilkårResultat: Set<VilkårResultat>
) : BaseEntitet()