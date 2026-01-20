package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.ekstern.restDomene.RegisteropplysningDto
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrMatrikkeladresseOppholdsadresse.Companion.fraMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrUtenlandskAdresseOppholdsadresse.Companion.fraUtenlandskAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrVegadresseOppholdsadresse.Companion.fraVegadresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrOppholdsadresse")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "PO_OPPHOLDSADRESSE")
abstract class GrOppholdsadresse(
    // Alle attributter må være open ellers kastes feil ved oppstart.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_oppholdsadresse_seq_generator")
    @SequenceGenerator(
        name = "po_oppholdsadresse_seq_generator",
        sequenceName = "po_oppholdsadresse_seq",
        allocationSize = 50,
    )
    open val id: Long = 0,
    @Embedded
    open var periode: DatoIntervallEntitet? = null,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    open var person: Person? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "opphold_annet_sted")
    open var oppholdAnnetSted: OppholdAnnetSted? = null,
) : BaseEntitet() {
    abstract fun toSecureString(): String

    abstract fun tilFrontendString(): String

    abstract fun erPåSvalbard(): Boolean

    abstract fun tilAdresse(): Adresse

    protected abstract fun tilKopiForNyPerson(): GrOppholdsadresse

    fun tilKopiForNyPerson(nyPerson: Person): GrOppholdsadresse =
        tilKopiForNyPerson().also {
            it.periode = periode
            it.person = nyPerson
            it.oppholdAnnetSted = oppholdAnnetSted
        }

    fun tilRegisteropplysningDto() =
        RegisteropplysningDto(
            fom = this.periode?.fom.takeIf { it != fregManglendeFlytteDato },
            tom = this.periode?.tom,
            verdi = this.tilFrontendString(),
        )

    companion object {
        // Når flyttedato er satt til 0001-01-01, så mangler den egentlig.
        // Det er en feil i Freg, som har arvet mangelfulle data fra DSF.
        val fregManglendeFlytteDato = LocalDate.of(1, 1, 1)

        fun fraOppholdsadresse(
            oppholdsadresse: Oppholdsadresse,
            person: Person,
            poststed: String? = null,
        ): GrOppholdsadresse =
            when {
                oppholdsadresse.vegadresse != null -> fraVegadresse(oppholdsadresse.vegadresse!!, poststed)
                oppholdsadresse.matrikkeladresse != null -> fraMatrikkeladresse(oppholdsadresse.matrikkeladresse!!, poststed)
                oppholdsadresse.utenlandskAdresse != null -> fraUtenlandskAdresse(oppholdsadresse.utenlandskAdresse!!)
                else -> GrUkjentAdresseOppholdsadresse()
            }.also {
                val fomFraPdl = oppholdsadresse.gyldigFraOgMed
                val fomPåAdresse = fomFraPdl?.let { fomFraPdl -> maxOf(fomFraPdl, person.fødselsdato) }

                it.person = person
                it.periode = DatoIntervallEntitet(fomPåAdresse, oppholdsadresse.gyldigTilOgMed)
                it.oppholdAnnetSted = OppholdAnnetSted.parse(oppholdsadresse.oppholdAnnetSted)
            }
    }
}
