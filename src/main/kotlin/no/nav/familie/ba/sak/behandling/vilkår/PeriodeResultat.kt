package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.nare.core.evaluations.Resultat
import javax.persistence.*

@Entity
@Table(name = "samlet_vilkar_resultat")
class PeriodeResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "samlet_vilkar_resultat_seq_generator")
        @SequenceGenerator(name = "samlet_vilkar_resultat_seq_generator",
                           sequenceName = "samlet_vilkar_resultat_seq",
                           allocationSize = 50)
        private val id: Long = 0,

        @Column(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandlingId: Long,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @OneToMany(mappedBy = "samletVilkårResultat", cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE])
        var periodeResultat: MutableSet<VilkårResultat>

) : BaseEntitet() {

        fun hentSamletResultat () : Resultat {
                return if (periodeResultat.any { it.resultat == Resultat.NEI }) Resultat.NEI else Resultat.JA
        }
}