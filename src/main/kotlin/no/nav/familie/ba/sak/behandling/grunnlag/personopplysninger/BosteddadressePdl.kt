package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.kontrakter.felles.personinfo.Bostedsadresse
import java.util.*
import javax.persistence.*

@Entity(name = "BostedsadressePdl")
@Inheritance
@DiscriminatorColumn(name = "type")
@Table(name = "PO_BOSTEDSADRESSE")
abstract class BostedsadressePdl(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresse_seq_generator")
        @SequenceGenerator(name = "po_bostedsadresse_seq_generator", sequenceName = "po_bostedsadresse_seq", allocationSize = 50)
        val id: Long = 0
) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        return this === other || toString() == other.toString()
    }

    override fun hashCode(): Int {
        return Objects.hash(toString())
    }

    companion object {
        fun fraBostedadress(bostedsadresse: Bostedsadresse?): BostedsadressePdl? {
            if(bostedsadresse == null){
                return null
            }else if (bostedsadresse.vegadresse != null) {
                return VegadressePdl.fraVegadresse(bostedsadresse.vegadresse!!)
            } else if (bostedsadresse.matrikkeladresse != null) {
                return MatrikkeladressePdl.fraMatrikkeladresse(bostedsadresse.matrikkeladresse!!)
            } else if (bostedsadresse.ukjentBosted != null) {
                return UkjentBostedPdl.fraUkjentBosted(bostedsadresse.ukjentBosted!!)
            } else {
                return null
            }
        }
    }
}