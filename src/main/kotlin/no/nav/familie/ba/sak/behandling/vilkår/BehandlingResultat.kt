package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.beregning.domene.personResultaterTilPeriodeResultater
import no.nav.familie.ba.sak.common.*
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

        @Enumerated(EnumType.STRING)
        @Column(name = "samlet_resultat")
        var samletResultat: BehandlingResultatType = BehandlingResultatType.IKKE_VURDERT,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "behandlingResultat",
                   cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH]
        )
        var personResultater: Set<PersonResultat> = setOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "BehandlingResultat(id=$id, behandling=${behandling.id})"
    }

    fun oppdaterSamletResultat(nyttBehandlingsresultat: BehandlingResultatType) {
        if (erHenlagt()) {
            throw Feil("Kan ikke endre på behandlingsresultat som er henlagt.")
        }

        samletResultat = nyttBehandlingsresultat
    }

    fun beregnSamletResultat(personopplysningGrunnlag: PersonopplysningGrunnlag?,
                             behandling: Behandling): BehandlingResultatType {
        if (personopplysningGrunnlag == null || personResultater.isEmpty() ||
            personResultater.any { it.hentSamletResultat() == BehandlingResultatType.IKKE_VURDERT }) {
            return BehandlingResultatType.IKKE_VURDERT
        }

        val minimumStørrelseForInnvilgelse =
                if (behandling.skalBehandlesAutomatisk) personopplysningGrunnlag.barna.size else 1

        return when {
            hentOppfyltePerioderPerBarn(personopplysningGrunnlag).size >= minimumStørrelseForInnvilgelse ->
                BehandlingResultatType.INNVILGET
            else ->
                if (this.behandling.type == BehandlingType.REVURDERING
                    || this.behandling.type == BehandlingType.TEKNISK_OPPHØR)
                    BehandlingResultatType.OPPHØRT
                else BehandlingResultatType.AVSLÅTT
        }
    }

    private fun hentOppfyltePerioderPerBarn(personopplysningGrunnlag: PersonopplysningGrunnlag): List<Pair<Periode, OppfyltPeriode>> {
        val (innvilgetPeriodeResultatSøker, innvilgedePeriodeResultatBarna) = hentInnvilgedePerioder(personopplysningGrunnlag)

        return innvilgedePeriodeResultatBarna
                .flatMap { periodeResultatBarn ->
                    innvilgetPeriodeResultatSøker
                            .filter { it.overlapper(periodeResultatBarn) }
                            .map { overlappendePerioderesultatSøker ->
                                val oppfyltFom =
                                        maksimum(overlappendePerioderesultatSøker.periodeFom, periodeResultatBarn.periodeFom)
                                val oppfyltTom =
                                        minimum(overlappendePerioderesultatSøker.periodeTom, periodeResultatBarn.periodeTom)

                                Pair(Periode(oppfyltFom, oppfyltTom),
                                     OppfyltPeriode(barn = periodeResultatBarn, søker = overlappendePerioderesultatSøker))
                            }
                }
    }

    fun periodeResultater(brukMåned: Boolean): Set<PeriodeResultat> = this.personResultaterTilPeriodeResultater(brukMåned)

    fun hentInnvilgedePerioder(personopplysningGrunnlag: PersonopplysningGrunnlag): Pair<List<PeriodeResultat>, List<PeriodeResultat>> {
        val periodeResultater = periodeResultater(false)

        val identBarnMap = personopplysningGrunnlag.barna
                .associateBy { it.personIdent.ident }

        val innvilgetPeriodeResultatSøker = periodeResultater.filter {
            it.personIdent == personopplysningGrunnlag.søker.personIdent.ident && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.SØKER
            )
        }
        val innvilgedePeriodeResultatBarna = periodeResultater.filter {
            identBarnMap.containsKey(it.personIdent) && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.BARN
            )
        }

        return Pair(innvilgetPeriodeResultatSøker, innvilgedePeriodeResultatBarna)
    }

    fun kopier(): BehandlingResultat {
        val nyttBehandlingResultat = BehandlingResultat(
                behandling = behandling,
                aktiv = aktiv,
                samletResultat = samletResultat,
        )

        nyttBehandlingResultat.personResultater = personResultater.map { it.kopierMedParent(nyttBehandlingResultat) }.toSet()
        return nyttBehandlingResultat
    }

    fun erHenlagt() =
            samletResultat == BehandlingResultatType.HENLAGT_FEILAKTIG_OPPRETTET || samletResultat == BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET
}

enum class BehandlingResultatType(val brevMal: String, val displayName: String) {
    INNVILGET(brevMal = "innvilget", displayName = "Innvilget"),
    DELVIS_INNVILGET(brevMal = "ukjent", displayName = "Delvis innvilget"),
    AVSLÅTT(brevMal = "avslag", displayName = "Avslått"),
    OPPHØRT(brevMal = "opphor", displayName = "Opphørt"),
    HENLAGT_FEILAKTIG_OPPRETTET(brevMal = "ukjent", displayName = "Henlagt feilaktig opprettet"),
    HENLAGT_SØKNAD_TRUKKET(brevMal = "ukjent", displayName = "Henlagt søknad trukket"),
    IKKE_VURDERT(brevMal = "ukjent", displayName = "Ikke vurdert"),
    ENDRING(brevMal = "ukjent", displayName = "Endring"),
    INGEN_ENDRING(brevMal = "ukjent", displayName = "Ingen endring")
}

data class OppfyltPeriode(
        val barn: PeriodeResultat,
        val søker: PeriodeResultat
)