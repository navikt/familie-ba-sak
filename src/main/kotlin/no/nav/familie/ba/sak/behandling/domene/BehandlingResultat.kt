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
        var behandlingResultat: MutableSet<PeriodeResultat>


) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingResultat(id=$id, behandling=${behandling.id})"
    }

    fun hentSamletResultat(): Resultat {
        return if (behandlingResultat.any { it.hentSamletResultat() == Resultat.NEI }) Resultat.NEI else Resultat.JA
    }

}

//TODO: Legg ved rett før merge
/*
CREATE TABLE BEHANDLING_RESULTAT
(
    ID               BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID BIGINT REFERENCES behandling (id)   NOT NULL,
    AKTIV            BOOLEAN      DEFAULT TRUE           NOT NULL
);

alter table behandling rename column resultat to brev;

alter table vilkar_resultat rename constraint vilkar_resultat_samlet_vilkar_resultat_id_fkey to vilkar_resultat_periode_resultat_id_fkey;

alter table samlet_vilkar_resultat drop column fk_behandling_id;
alter table samlet_vilkar_resultat rename to periode_resultat;
alter table periode_resultat add column behandling_resultat_id bigint references BEHANDLING_RESULTAT (id) default null;
alter table periode_resultat add column periode_fom timestamp (3);
alter table periode_resultat add column periode_tom timestamp (3);
alter index samlet_vilkar_resultat_pkey rename to periode_resultat_pkey;


 */