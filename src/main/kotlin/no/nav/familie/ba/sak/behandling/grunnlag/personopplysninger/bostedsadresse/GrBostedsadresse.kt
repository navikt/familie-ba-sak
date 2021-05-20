package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.bostedsadresse

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrBostedsadresse")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "PO_BOSTEDSADRESSE")
abstract class GrBostedsadresse(
        // Alle attributter må være open ellers kastes feil ved oppsrart.
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresse_seq_generator")
        @SequenceGenerator(name = "po_bostedsadresse_seq_generator", sequenceName = "po_bostedsadresse_seq", allocationSize = 50)
        open val id: Long = 0,

        @Embedded
        open val periode: DatoIntervallEntitet? = null,

        @JsonIgnore
        @ManyToOne
        @JoinColumn(name = "fk_po_person_id")
        open var person: Person? = null,
) : BaseEntitet() {

    abstract fun toSecureString(): String

    companion object {

        fun fraBostedsadresse(bostedsadresse: Bostedsadresse, person: Person): GrBostedsadresse {
            return when {
                bostedsadresse.vegadresse != null -> {
                    GrVegadresse.fraVegadresse(bostedsadresse.vegadresse!!).also { it.person = person }
                }
                bostedsadresse.matrikkeladresse != null -> {
                    GrMatrikkeladresse.fraMatrikkeladresse(bostedsadresse.matrikkeladresse!!).also { it.person = person }
                }
                bostedsadresse.ukjentBosted != null -> {
                    GrUkjentBosted.fraUkjentBosted(bostedsadresse.ukjentBosted!!).also { it.person = person }
                }
                else -> throw Feil("Vegadresse, matrikkeladresse og ukjent bosted har verdi null ved mapping fra bostedadresse")
            }
        }

        fun erSammeAdresse(adresse: GrBostedsadresse?, andreAdresse: GrBostedsadresse?): Boolean {
            return adresse != null &&
                   adresse !is GrUkjentBosted &&
                   adresse == andreAdresse
        }
    }
}