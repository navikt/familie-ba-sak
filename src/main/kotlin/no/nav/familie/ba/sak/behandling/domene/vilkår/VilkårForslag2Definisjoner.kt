package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.nare.core.evaluations.Evaluering

fun hentVilkårFor(personType: PersonType, sakstype: Any): Set<VilkårForslag2> {
    return alleVilkår.filter { vilkår ->
        personType in vilkår.vilkårType.parterDetteGjelderFor
        && sakstype in vilkår.vilkårType.sakstyperDetteGjelderFor
    }.toSet()
}

internal val under18årOgBorMedSøker = VilkårForslag2(
        vilkårType = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER,
        implementasjon = {
            when {
                this.barn.isNotEmpty() -> Evaluering.ja(
                        "Barn er under 18 år og bor med søker"
                )
                else -> Evaluering.nei("Barn er ikke under 18 år eller bor ikke med søker")
            }
        })

internal val bosattIRiket = VilkårForslag2(
        vilkårType = VilkårType.BOSATT_I_RIKET,
        implementasjon = {
            when {
                this.barn.isNotEmpty() -> Evaluering.ja(
                        "Barn er under 18 år og bor med søker"
                )
                else -> Evaluering.nei("Barn er ikke under 18 år eller bor ikke med søker")
            }
        })

internal val stønadsperiode = VilkårForslag2(
        vilkårType = VilkårType.STØNADSPERIODE,
        implementasjon = {
            when {
                this.barn.isNotEmpty() -> Evaluering.ja(
                        "Retten til barnetrygd"
                )
                else -> Evaluering.nei("Barnetrygd gis fra og med kalendermåneden etter at retten til barnetrygd inntrer")
            }
        })

internal val alleVilkår = setOf(under18årOgBorMedSøker,
                                stønadsperiode,
                                bosattIRiket)