package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.LovligOppholdFaktaEØS
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.VurderBarnErBosattMedSøker
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.VurderBarnErUgift
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.VurderBarnErUnder18
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.VurderBarnHarLovligOpphold
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.VurderPersonErBosattIRiket
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.VurderPersonHarLovligOpphold
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.ANNENPART
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.BARN
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.SØKER
import java.time.LocalDate

enum class Vilkår(
    val parterDetteGjelderFor: List<PersonType>,
    val ytelseType: YtelseType,
    val beskrivelse: String,
    val harRegelverk: Boolean
) {

    UNDER_18_ÅR(
        parterDetteGjelderFor = listOf(BARN),
        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
        beskrivelse = "Er under 18 år",
        false
    ),
    BOR_MED_SØKER(
        parterDetteGjelderFor = listOf(BARN),
        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
        beskrivelse = "Bor med søker",
        true
    ),
    GIFT_PARTNERSKAP(
        parterDetteGjelderFor = listOf(BARN),
        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
        beskrivelse = "Gift/partnerskap",
        false
    ),
    BOSATT_I_RIKET(
        parterDetteGjelderFor = listOf(SØKER, BARN),
        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
        beskrivelse = "Bosatt i riket",
        true
    ),
    LOVLIG_OPPHOLD(
        parterDetteGjelderFor = listOf(SØKER, BARN),
        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
        beskrivelse = "Lovlig opphold",
        true
    ),
    UTVIDET_BARNETRYGD(
        parterDetteGjelderFor = listOf(SØKER),
        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
        beskrivelse = "Utvidet barnetrygd",
        false
    );

    override fun toString(): String {
        return this.name
    }

    companion object {

        fun hentVilkårFor(personType: PersonType, ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD, fagsakType: FagsakType = FagsakType.NORMAL): Set<Vilkår> {
            return when (fagsakType) {
                FagsakType.NORMAL -> when (personType) {
                    BARN -> setOf(UNDER_18_ÅR, BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD)
                    SØKER -> setOf(BOSATT_I_RIKET, LOVLIG_OPPHOLD) + if (ytelseType == YtelseType.UTVIDET_BARNETRYGD) setOf(UTVIDET_BARNETRYGD) else emptySet()
                    ANNENPART -> emptySet()
                }
                FagsakType.INSTITUSJON -> when (personType) {
                    BARN -> setOf(UNDER_18_ÅR, BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD)
                    SØKER, ANNENPART -> emptySet()
                }
                FagsakType.BARN_ENSLIG_MINDREÅRIG -> when (personType) {
                    BARN -> setOf(UNDER_18_ÅR, BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD) + if (ytelseType == YtelseType.UTVIDET_BARNETRYGD) setOf(UTVIDET_BARNETRYGD) else emptySet()
                    SØKER, ANNENPART -> emptySet()
                }
            }
        }

        fun hentFødselshendelseVilkårsreglerRekkefølge(): List<Vilkår> {
            return listOf(
                UNDER_18_ÅR,
                BOR_MED_SØKER,
                GIFT_PARTNERSKAP,
                BOSATT_I_RIKET,
                LOVLIG_OPPHOLD
            )
        }
    }

    fun defaultRegelverk(behandlingKategori: BehandlingKategori): Regelverk? {
        return when (this) {
            BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD -> {
                if (behandlingKategori == BehandlingKategori.EØS) {
                    Regelverk.EØS_FORORDNINGEN
                } else {
                    Regelverk.NASJONALE_REGLER
                }
            }

            UTVIDET_BARNETRYGD, UNDER_18_ÅR, GIFT_PARTNERSKAP -> null
        }
    }

    fun vurderVilkår(
        person: Person,
        vurderFra: LocalDate = LocalDate.now(),
        annenForelder: Person? = null
    ): AutomatiskVurdering {
        val vilkårsregel = when (this) {
            UNDER_18_ÅR -> VurderBarnErUnder18(
                alder = person.hentAlder()
            )

            BOR_MED_SØKER -> VurderBarnErBosattMedSøker(
                søkerAdresser = person.personopplysningGrunnlag.søker.bostedsadresser,
                barnAdresser = person.bostedsadresser
            )

            GIFT_PARTNERSKAP -> VurderBarnErUgift(
                sivilstander = person.sivilstander
            )

            BOSATT_I_RIKET -> VurderPersonErBosattIRiket(
                adresser = person.bostedsadresser,
                vurderFra = vurderFra
            )

            LOVLIG_OPPHOLD -> if (person.type == BARN) {
                VurderBarnHarLovligOpphold(
                    aktør = person.aktør
                )
            } else {
                VurderPersonHarLovligOpphold(
                    morLovligOppholdFaktaEØS = LovligOppholdFaktaEØS(
                        arbeidsforhold = person.arbeidsforhold,
                        bostedsadresser = person.bostedsadresser,
                        statsborgerskap = person.statsborgerskap
                    ),
                    annenForelderLovligOppholdFaktaEØS = if (annenForelder != null) {
                        LovligOppholdFaktaEØS(
                            arbeidsforhold = annenForelder.arbeidsforhold,
                            bostedsadresser = annenForelder.bostedsadresser,
                            statsborgerskap = annenForelder.statsborgerskap
                        )
                    } else {
                        null
                    },
                    opphold = person.opphold
                )
            }

            UTVIDET_BARNETRYGD -> throw Feil("Ikke støtte for å automatisk vurdere vilkåret ${this.beskrivelse}")
        }

        return AutomatiskVurdering(
            evaluering = vilkårsregel.vurder(),
            regelInput = vilkårsregel.convertDataClassToJson()
        )
    }
}

data class AutomatiskVurdering(
    val regelInput: String,
    val evaluering: Evaluering,
    val resultat: Resultat = evaluering.resultat
)

data class GyldigVilkårsperiode(
    val gyldigFom: LocalDate = LocalDate.MIN,
    val gyldigTom: LocalDate = LocalDate.MAX
) {

    fun gyldigFor(dato: LocalDate): Boolean {
        return !(dato.isBefore(gyldigFom) || dato.isAfter(gyldigTom))
    }
}
