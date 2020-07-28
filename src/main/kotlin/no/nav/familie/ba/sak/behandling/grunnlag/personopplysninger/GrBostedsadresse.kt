package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import javax.persistence.*

@Entity(name = "GrBostedsadresse")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "PO_BOSTEDSADRESSE")
abstract class GrBostedsadresse(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresse_seq_generator")
        @SequenceGenerator(name = "po_bostedsadresse_seq_generator", sequenceName = "po_bostedsadresse_seq", allocationSize = 50)
        val id: Long = 0
) : BaseEntitet() {

    abstract fun toSecureString(): String

    companion object {
        fun fraBostedsadresse(bostedsadresse: Bostedsadresse?): GrBostedsadresse? {
            if(bostedsadresse == null){
                return null
            }else if (bostedsadresse.vegadresse != null) {
                return GrVegadresse.fraVegadresse(bostedsadresse.vegadresse!!)
            } else if (bostedsadresse.matrikkeladresse != null) {
                return GrMatrikkeladresse.fraMatrikkeladresse(bostedsadresse.matrikkeladresse!!)
            } else if (bostedsadresse.ukjentBosted != null) {
                return GrUkjentBosted.fraUkjentBosted(bostedsadresse.ukjentBosted!!)
            } else {
                return null
            }
        }
    }
}