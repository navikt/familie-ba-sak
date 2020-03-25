package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
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

        @ManyToOne @JoinColumn(name = "behandling_resultat_id")
        var behandlingResultat: BehandlingResultat? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_person_id", nullable = false, updatable = false)
        val person: Person,

        @Column(name = "periode_fom", nullable = false, updatable = false)
        val periodeFom: LocalDate,

        @Column(name = "periode_tom", nullable = false, updatable = false)
        val periodeTom: LocalDate?,

        @OneToMany(mappedBy = "periodeResultat", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        var vilkårResultater: MutableSet<VilkårResultat> = mutableSetOf()

) : BaseEntitet() {

        fun hentSamletResultat(): BehandlingResultatType {
                if ( vilkårResultater.none { it.resultat == Resultat.NEI }) {
                        return BehandlingResultatType.INNVILGET
                } else if ( vilkårResultater.none { it.resultat == Resultat.JA }) {
                        return BehandlingResultatType.AVSLÅTT
                } else {
                        return BehandlingResultatType.DELVIS_INNVILGET
                }
        }
}