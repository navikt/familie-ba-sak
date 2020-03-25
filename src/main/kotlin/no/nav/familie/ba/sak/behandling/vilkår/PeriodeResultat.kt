package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
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

        @ManyToOne @JoinColumn(name = "behandling_resultat_id")
        var behandlingResultat: BehandlingResultat? = null,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @Column(name = "periode_fom", nullable = false, updatable = false)
        val periodeFom: LocalDate?,

        @Column(name = "periode_tom", nullable = false, updatable = false)
        val periodeTom: LocalDate?,

        @OneToMany(mappedBy = "periodeResultat", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        var vilkårResultater: MutableSet<VilkårResultat>

) : BaseEntitet() {

        fun hentSamletResultat () : BehandlingResultatType {
                return if (vilkårResultater.any { it.resultat == Resultat.NEI }) BehandlingResultatType.AVSLÅTT else BehandlingResultatType.INNVILGET
        }
}