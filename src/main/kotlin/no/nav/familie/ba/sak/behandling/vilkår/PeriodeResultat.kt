package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.nare.core.evaluations.Resultat
import javax.persistence.*

@Entity
@Table(name = "periode_resultat")
class PeriodeResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "periode_resultat_seq_generator")
        @SequenceGenerator(name = "periode_resultat_seq_generator",
                           sequenceName = "periode_resultat_seq",
                           allocationSize = 50)
        private val id: Long = 0,

        @Column(name = "fk_behandling_id", nullable = false, updatable = false) //TODO: FJERNE?
        val behandlingId: Long,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @ManyToOne @JoinColumn(name = "behandling_resultat_id")
        var behandlingResultat: BehandlingResultat? = null,

        @OneToMany(mappedBy = "periodeResultat", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        var periodeResultat: MutableSet<VilkårResultat>

) : BaseEntitet() {

        fun hentSamletResultat () : Resultat {
                return if (periodeResultat.any { it.resultat == Resultat.NEI }) Resultat.NEI else Resultat.JA
        }
}