package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
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
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegisteropplysning
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrMatrikkeladresseDeltBosted.Companion.fraMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrUkjentBostedDeltBosted.Companion.fraUkjentBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.deltbosted.GrVegadresseDeltBosted.Companion.fraVegadresse
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "GrDeltBosted")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "PO_DELT_BOSTED")
abstract class GrDeltBosted(
    // Alle attributter må være open ellers kastes feil ved oppstart.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_delt_bosted_seq_generator")
    @SequenceGenerator(
        name = "po_delt_bosted_seq_generator",
        sequenceName = "po_delt_bosted_seq",
        allocationSize = 50,
    )
    open val id: Long = 0,
    @Embedded
    open var periode: DatoIntervallEntitet? = null,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    open var person: Person? = null,
) : BaseEntitet() {
    abstract fun toSecureString(): String

    abstract fun tilFrontendString(): String

    abstract fun tilAdresse(): Adresse

    protected abstract fun tilKopiForNyPerson(): GrDeltBosted

    fun tilKopiForNyPerson(nyPerson: Person): GrDeltBosted =
        tilKopiForNyPerson().also {
            it.periode = periode
            it.person = nyPerson
        }

    fun tilRestRegisteropplysning() =
        RestRegisteropplysning(
            fom = this.periode?.fom.takeIf { it != fregManglendeFlytteDato },
            tom = this.periode?.tom,
            verdi = this.tilFrontendString(),
        )

    companion object {
        // Når flyttedato er satt til 0001-01-01, så mangler den egentlig.
        // Det er en feil i Freg, som har arvet mangelfulle data fra DSF.
        val fregManglendeFlytteDato = LocalDate.of(1, 1, 1)

        fun fraDeltBosted(
            deltBosted: DeltBosted,
            person: Person,
            poststed: String? = null,
        ): GrDeltBosted =
            when {
                deltBosted.vegadresse != null -> fraVegadresse(deltBosted.vegadresse!!, poststed)
                deltBosted.matrikkeladresse != null -> fraMatrikkeladresse(deltBosted.matrikkeladresse!!, poststed)
                deltBosted.ukjentBosted != null -> fraUkjentBosted(deltBosted.ukjentBosted!!)
                else -> throw Feil("Vegadresse, matrikkeladresse og ukjent bosted har verdi null ved mapping fra delt bosted")
            }.also {
                it.person = person
                it.periode = DatoIntervallEntitet(deltBosted.startdatoForKontrakt, deltBosted.sluttdatoForKontrakt)
            }
    }
}
