package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform

// TODO: Definer alle felter vi trenger. Dette er bare et eksempel.
data class Søknad(
    val søker: Søker,
    val barn: List<Barn>,
    val behandlingKategori: BehandlingKategori,
    val behandlingUnderkategori: BehandlingUnderkategori,
    val harKryssetPåEøsSpørsmål: Boolean,
    val inneholderVedlegg: Boolean,
    val målform: Målform = Målform.NB,
) {
    fun harKryssetForDeltBostedForMinstEttBarn(): Boolean = barn.any { it.harKryssetForDeltBosted }

    fun harKryssetForFosterhjemEllerBeredskapshjemForMinstEttBarn(): Boolean = barn.any { it.erFosterbarn || it.erIBeredskapshjem }

    fun harKryssetForInstitusjonForMinstEttBarn(): Boolean = barn.any { it.oppholderSegIInstitusjon }

    fun harKryssetForAsylsøkerForSøkerEllerMinstEttBarn(): Boolean = søker.erAsylsøker || barn.any { it.erAsylsøker }

    fun harKryssetForAdoptertFraUtlandetForMinstEttBarn(): Boolean = barn.any { it.erAdoptertFraUtland }
}

data class Barn(
    val fnr: String,
    val planleggerÅBoINorge12Mnd: Boolean,
    val erFosterbarn: Boolean,
    val erIBeredskapshjem: Boolean,
    val harKryssetForDeltBosted: Boolean,
    val oppholderSegIInstitusjon: Boolean,
    val erAsylsøker: Boolean,
    val erAdoptertFraUtland: Boolean,
    val borFastMedSøker: Boolean,
    val harOppholdtSegUtenforNorgeSiste12Mnd: Boolean,
)

data class Søker(
    val fnr: String,
    val planleggerÅBoINorge12Mnd: Boolean,
    val erAsylsøker: Boolean,
    val harOppholdtSegSammenhengendeINorgeSiste12Mnd: Boolean,
)
