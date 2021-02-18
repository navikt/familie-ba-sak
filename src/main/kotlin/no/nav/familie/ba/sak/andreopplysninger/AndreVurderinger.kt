package no.nav.familie.ba.sak.andreopplysninger

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "AndreVurderinger")
@Table(name = "ANDRE_VURDERINGER")
data class AndreVurderinger(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "andre_vurderinger_seq_generator")
        @SequenceGenerator(name = "andre_vurderinger_seq_generator", sequenceName = "andre_vurderinger_seq", allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_behandling_id")
        var behandlingId: Long,

        @Column(name = "fk_person_resultat_id")
        var personResultatId: Long,

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat")
        var resultat: Resultat = Resultat.IKKE_VURDERT,

        @Enumerated(EnumType.STRING)
        @Column(name = "type")
        var type: AndreVurderingerType = AndreVurderingerType.OPPLYSNINGSPLIKT,

        @Column(name = "begrunnelse")
        var begrunnelse: String? = null
) : BaseEntitet() {
}

enum class AndreVurderingerType {
    OPPLYSNINGSPLIKT
}