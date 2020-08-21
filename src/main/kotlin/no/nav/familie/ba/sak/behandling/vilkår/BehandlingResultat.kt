package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.beregning.domene.personResultaterTilPeriodeResultater
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
                   cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH]
        )
        var personResultater: Set<PersonResultat> = setOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingResultat(id=$id, behandling=${behandling.id})"
    }

    fun hentSamletResultat(): BehandlingResultatType {
        if (personResultater.isEmpty() ||
            personResultater.any { it.hentSamletResultat() == BehandlingResultatType.IKKE_VURDERT }) {
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

    fun periodeResultater(brukMåned: Boolean): Set<PeriodeResultat> = this.personResultaterTilPeriodeResultater(brukMåned)

    fun kopier(): BehandlingResultat {
        val nyttBehandlingResultat = BehandlingResultat(
                behandling = behandling,
                aktiv = aktiv
        )

        nyttBehandlingResultat.personResultater = personResultater.map { it.kopierMedParent(nyttBehandlingResultat) }.toSet()
        return nyttBehandlingResultat
    }
}

enum class BehandlingResultatType(val brevMal: String, val displayName: String) {
    INNVILGET(brevMal = "innvilget", displayName = "Innvilget"),
    DELVIS_INNVILGET(brevMal = "ukjent", displayName = "Delvis innvilget"),
    AVSLÅTT(brevMal = "avslag", displayName = "Avslått"),
    OPPHØRT(brevMal = "opphor", displayName = "Opphørt"),
    HENLAGT(brevMal = "ukjent", displayName = "Henlagt"),
    IKKE_VURDERT(brevMal = "ukjent", displayName = "Ikke vurdert")
}