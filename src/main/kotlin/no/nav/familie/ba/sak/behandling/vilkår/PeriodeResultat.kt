package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "PeriodeResultat")
@Table(name = "PERIODE_RESULTAT")
class PeriodeResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "periode_resultat_seq_generator")
        @SequenceGenerator(name = "periode_resultat_seq_generator",
                           sequenceName = "periode_resultat_seq",
                           allocationSize = 50)
        private val id: Long = 0,

        @ManyToOne @JoinColumn(name = "fk_behandling_resultat_id", nullable = false, updatable = false)
        var behandlingResultat: BehandlingResultat,

        @Column(name = "person_ident", nullable = false, updatable = false)
        val personIdent: String,

        @Column(name = "periode_fom")
        val periodeFom: LocalDate?,

        @Column(name = "periode_tom")
        val periodeTom: LocalDate?,

        @OneToMany(mappedBy = "periodeResultat", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        var vilkårResultater: Set<VilkårResultat> = setOf()

) : BaseEntitet() {

        fun hentSamletResultat(): BehandlingResultatType {
                return when {
                    vilkårResultater.all { it.resultat == Resultat.JA } -> {
                            BehandlingResultatType.INNVILGET
                    }
                    else -> {
                            BehandlingResultatType.AVSLÅTT
                    }
                }
        }
}