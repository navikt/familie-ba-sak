package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPerson
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.behandling.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.økonomi.sats
import no.nav.familie.kontrakter.felles.personinfo.SIVILSTAND
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

fun randomFnr(): String = UUID.randomUUID().toString()
fun randomAktørId(): AktørId = AktørId(UUID.randomUUID().toString())

private var gjeldendeVedtakId: Long = abs(Random.nextLong(10000000))
private var gjeldendeBehandlingId: Long = abs(Random.nextLong(10000000))
private var gjeldendePersonId: Long = abs(Random.nextLong(10000000))
private val id_inkrement = 50

fun nesteVedtakId(): Long {
    gjeldendeVedtakId += id_inkrement
    return gjeldendeVedtakId
}

fun nesteBehandlingId(): Long {
    gjeldendeBehandlingId += id_inkrement
    return gjeldendeBehandlingId
}

fun nestePersonId(): Long {
    gjeldendePersonId += id_inkrement
    return gjeldendePersonId
}

val defaultFagsak = Fagsak(1,
                           FagsakStatus.OPPRETTET).also {
    it.søkerIdenter = setOf(FagsakPerson(fagsak = it, personIdent = PersonIdent(randomFnr())))
}

fun lagBehandling(fagsak: Fagsak = defaultFagsak,
                  behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
                  behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
) = Behandling(id = nesteBehandlingId(),
               fagsak = fagsak,
               type = behandlingType,
               kategori = behandlingKategori,
               underkategori = BehandlingUnderkategori.ORDINÆR,
               opprinnelse = BehandlingOpprinnelse.MANUELL)

fun tilfeldigPerson(fødselsdato: LocalDate = LocalDate.now(), personType: PersonType = PersonType.BARN) = Person(
        id = nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = PersonIdent(randomFnr()),
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = Kjønn.MANN,
        sivilstand = SIVILSTAND.UGIFT
)

fun lagVedtak(behandling: Behandling = lagBehandling(),
              forrigeVedtak: Vedtak? = null,
              opphørsdato: LocalDate? = null) =
        Vedtak(id = nesteVedtakId(),
               behandling = behandling,
               vedtaksdato = LocalDate.now(),
               forrigeVedtakId = forrigeVedtak?.id,
               opphørsdato = opphørsdato
        )

fun lagAndelTilkjentYtelse(fom: String,
                           tom: String,
                           ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
                           beløp: Int = sats(ytelseType),
                           behandling: Behandling = lagBehandling(),
                           person: Person = tilfeldigPerson(),
                           periodeIdOffset: Long? = null): AndelTilkjentYtelse {
    return AndelTilkjentYtelse(
            personIdent = person.personIdent.ident,
            behandlingId = behandling.id,
            tilkjentYtelse = lagInitiellTilkjentYtelse(behandling),
            beløp = beløp,
            stønadFom = dato(fom),
            stønadTom = dato(tom),
            type = ytelseType,
            periodeOffset = periodeIdOffset
    )
}

fun lagInitiellTilkjentYtelse(behandling: Behandling): TilkjentYtelse {
    return TilkjentYtelse(behandling = behandling, opprettetDato = LocalDate.now(), endretDato = LocalDate.now())
}

fun lagTestPersonopplysningGrunnlag(behandlingId: Long,
                                    vararg personer: Person): PersonopplysningGrunnlag {

    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    personopplysningGrunnlag.personer.addAll(
            personer.map { it.copy(personopplysningGrunnlag = personopplysningGrunnlag) }
    )
    return personopplysningGrunnlag
}

fun lagTestPersonopplysningGrunnlag(behandlingId: Long,
                                    søkerPersonIdent: String,
                                    barnasIdenter: List<String>): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
    val bostedsadresse = GrMatrikkeladresse(matrikkelId = null, bruksenhetsnummer = "H301", tilleggsnavn = "navn",
                                            postnummer = "0202", kommunenummer = "2231")

    val søker = Person(aktørId = randomAktørId(),
                       personIdent = PersonIdent(søkerPersonIdent),
                       type = PersonType.SØKER,
                       personopplysningGrunnlag = personopplysningGrunnlag,
                       fødselsdato = LocalDate.of(2019, 1, 1),
                       navn = "",
                       kjønn = Kjønn.KVINNE,
                       bostedsadresse = bostedsadresse,
                       sivilstand = SIVILSTAND.GIFT)
    personopplysningGrunnlag.personer.add(søker)

    barnasIdenter.map {
        personopplysningGrunnlag.personer.add(Person(aktørId = randomAktørId(),
                                                     personIdent = PersonIdent(it),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = LocalDate.of(2019, 1, 1),
                                                     navn = "",
                                                     kjønn = Kjønn.MANN,
                                                     bostedsadresse = bostedsadresse,
                                                     sivilstand = SIVILSTAND.UGIFT))
    }
    return personopplysningGrunnlag
}

fun dato(s: String) = LocalDate.parse(s)
fun årMnd(s: String) = YearMonth.parse(s)

fun nyOrdinærBehandling(søkersIdent: String): NyBehandling = NyBehandling(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR
)

fun nyRevurdering(søkersIdent: String): NyBehandling = NyBehandling(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.REVURDERING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR
)

fun lagSøknadDTO(søkerIdent: String, annenPartIdent: String, barnasIdenter: List<String>): SøknadDTO {
    return SøknadDTO(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            annenPartIdent = annenPartIdent,
            søkerMedOpplysninger = SøkerMedOpplysninger(
                    ident = søkerIdent
            ),
            barnaMedOpplysninger = barnasIdenter.map {
                BarnMedOpplysninger(
                        ident = it
                )
            }
    )
}

fun lagPersonResultaterForSøkerOgToBarn(behandlingResultat: BehandlingResultat,
                                        søkerFnr: String,
                                        barn1Fnr: String,
                                        barn2Fnr: String,
                                        stønadFom: LocalDate,
                                        stønadTom: LocalDate): Set<PersonResultat> {
    return setOf(
            lagPersonResultat(behandlingResultat = behandlingResultat,
                              fnr = søkerFnr,
                              resultat = Resultat.JA,
                              periodeFom = stønadFom,
                              periodeTom = stønadTom,
                              lagFullstendigVilkårResultat = true,
                              personType = PersonType.SØKER
            ),
            lagPersonResultat(behandlingResultat = behandlingResultat,
                              fnr = barn1Fnr,
                              resultat = Resultat.JA,
                              periodeFom = stønadFom,
                              periodeTom = stønadTom,
                              lagFullstendigVilkårResultat = true,
                              personType = PersonType.BARN
            ),
            lagPersonResultat(behandlingResultat = behandlingResultat,
                              fnr = barn2Fnr,
                              resultat = Resultat.JA,
                              periodeFom = stønadFom,
                              periodeTom = stønadTom,
                              lagFullstendigVilkårResultat = true,
                              personType = PersonType.BARN
            )
    )
}

fun lagPersonResultat(behandlingResultat: BehandlingResultat,
                      fnr: String,
                      resultat: Resultat,
                      periodeFom: LocalDate?,
                      periodeTom: LocalDate?,
                      lagFullstendigVilkårResultat: Boolean = false,
                      personType: PersonType = PersonType.BARN,
                      vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET): PersonResultat {
    val personResultat = PersonResultat(
            behandlingResultat = behandlingResultat,
            personIdent = fnr)

    if (lagFullstendigVilkårResultat) {
        personResultat.setVilkårResultater(
                Vilkår.hentVilkårFor(personType, SakType.NASJONAL).map {
                    VilkårResultat(personResultat = personResultat,
                                   periodeFom = periodeFom,
                                   periodeTom = periodeTom,
                                   vilkårType = it,
                                   resultat = resultat,
                                   begrunnelse = "",
                                   behandlingId = behandlingResultat.behandling.id,
                                   regelInput = null,
                                   regelOutput = null)
                }.toSet())
    } else {
        personResultat.setVilkårResultater(
                setOf(VilkårResultat(personResultat = personResultat,
                                     periodeFom = periodeFom,
                                     periodeTom = periodeTom,
                                     vilkårType = vilkårType,
                                     resultat = resultat,
                                     begrunnelse = "",
                                     behandlingId = behandlingResultat.behandling.id,
                                     regelInput = null,
                                     regelOutput = null))
        )
    }
    return personResultat
}

fun vurderBehandlingResultatTilInnvilget(behandlingResultat: BehandlingResultat, barn: Person) {
    behandlingResultat.personResultater.forEach { personResultat ->
        personResultat.vilkårResultater.forEach {
            if (it.vilkårType == Vilkår.UNDER_18_ÅR) {
                it.resultat = Resultat.JA
                it.periodeFom = barn.fødselsdato
                it.periodeTom = barn.fødselsdato.plusYears(18)
            } else {
                it.resultat = Resultat.JA
                it.periodeFom = LocalDate.now()
            }
        }
    }
}

fun lagBehandlingResultat(fnr: String, behandling: Behandling, resultat: Resultat): BehandlingResultat {
    val behandlingResultat = BehandlingResultat(
            behandling = behandling
    )
    val personResultat = PersonResultat(
            behandlingResultat = behandlingResultat,
            personIdent = fnr)
    personResultat.setVilkårResultater(
            setOf(VilkårResultat(personResultat = personResultat,
                                 vilkårType = Vilkår.BOSATT_I_RIKET,
                                 resultat = resultat,
                                 periodeFom = LocalDate.now(),
                                 periodeTom = LocalDate.now(),
                                 begrunnelse = "",
                                 behandlingId = behandling.id,
                                 regelInput = null,
                                 regelOutput = null))
    )
    behandlingResultat.personResultater = setOf(personResultat)
    return behandlingResultat
}