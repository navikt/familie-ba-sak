package no.nav.familie.ba.sak.behandling.domene

import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
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
                   cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE]
        )
        var personResultater: Set<PersonResultat> = setOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingResultat(id=$id, behandling=${behandling.id})"
    }

    fun kopier(): BehandlingResultat = BehandlingResultat(
            personResultater = personResultater.map { personResultat -> personResultat.kopier() }.toSet(),
            behandling = behandling,
            aktiv = aktiv
    )

    fun hentSamletResultat(): BehandlingResultatType {
        if (personResultater.isEmpty()) {
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

    fun disjunktePersoner(sammenligning: BehandlingResultat): Pair<Set<PersonResultat>, Set<PersonResultat>> {
        val venstre = mutableSetOf<PersonResultat>()
        val høyre = mutableSetOf<PersonResultat>()

        personResultater.forEach { A ->
            sammenligning.personResultater.forEach { B ->
                if (personResultater.none { it.personIdent == B.personIdent }) høyre.add(B)
                else if (sammenligning.personResultater.none { it.personIdent == A.personIdent }) venstre.add(A)
            }
        }
        return Pair(venstre, høyre)
    }
}

enum class BehandlingResultatType(val brevMal: String, val displayName: String) {
    INNVILGET(brevMal = "Innvilget", displayName = "Innvilget"),
    DELVIS_INNVILGET(brevMal = "Ukjent", displayName = "Delvis innvilget"),
    AVSLÅTT(brevMal = "Avslag", displayName = "Avslått"),
    OPPHØRT(brevMal = "Opphor", displayName = "Opphørt"),
    HENLAGT(brevMal = "Ukjent", displayName = "Henlagt"),
    IKKE_VURDERT(brevMal = "Ukjent", displayName = "Ikke vurdert")
}