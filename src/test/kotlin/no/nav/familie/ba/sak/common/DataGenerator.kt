package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.BarnMedOpplysninger
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøkerMedOpplysninger
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadDTO
import no.nav.familie.ba.sak.behandling.vedtak.AndelTilkjentYtelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.økonomi.sats
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.random.Random

fun randomFnr(): String = UUID.randomUUID().toString()
fun randomAktørId(): AktørId = AktørId(UUID.randomUUID().toString())

private var gjeldendeVedtakId: Long = Random.nextLong()
private var gjeldendeBehandlingId: Long = Random.nextLong()
private var gjeldendePersonId: Long = Random.nextLong()
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
                           AktørId("1"),
                           PersonIdent("12345"),
                           FagsakStatus.OPPRETTET)

fun lagBehandling(fagsak: Fagsak = defaultFagsak) = Behandling(id = nesteBehandlingId(),
                                                               fagsak = fagsak,
                                                               type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                               kategori = BehandlingKategori.NASJONAL,
                                                               underkategori = BehandlingUnderkategori.ORDINÆR)

fun tilfeldigPerson(fødselsdato: LocalDate = LocalDate.now(), personType: PersonType = PersonType.BARN) = Person(
        id = nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = PersonIdent(randomFnr()),
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = Kjønn.MANN
)

fun lagVedtak(behandling: Behandling = lagBehandling(),
              forrigeVedtak: Vedtak? = null,
              opphørsdato: LocalDate? = null) =
        Vedtak(id = nesteVedtakId(),
               behandling = behandling,
               ansvarligSaksbehandler = "ansvarligSaksbehandler",
               vedtaksdato = LocalDate.now(),
               stønadBrevMarkdown = "",
               forrigeVedtakId = forrigeVedtak?.id,
               opphørsdato = opphørsdato
        )

fun lagAndelTilkjentYtelse(fom: String,
                           tom: String,
                           ytelsetype: Ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD,
                           beløp: Int = sats(ytelsetype),
                           behandling: Behandling = lagBehandling()): AndelTilkjentYtelse {
    return AndelTilkjentYtelse(
            personId = tilfeldigPerson().id,
            behandlingId = behandling.id,
            tilkjentYtelse = lagInitiellTilkjentYtelse(behandling),
            beløp = beløp,
            stønadFom = dato(fom),
            stønadTom = dato(tom),
            type = ytelsetype
    )
}

fun lagInitiellTilkjentYtelse(behandling: Behandling): TilkjentYtelse {
    return TilkjentYtelse(behandling = behandling, opprettetDato = LocalDate.now(), endretDato = LocalDate.now())
}

fun lagTestPersonopplysningGrunnlag(behandlingId: Long,
                                    søkerPersonIdent: String,
                                    barnasIdenter: List<String>): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
    val søker = Person(aktørId = randomAktørId(),
                       personIdent = PersonIdent(søkerPersonIdent),
                       type = PersonType.SØKER,
                       personopplysningGrunnlag = personopplysningGrunnlag,
                       fødselsdato = LocalDate.of(2019, 1, 1),
                       navn = "",
                       kjønn = Kjønn.KVINNE)
    personopplysningGrunnlag.personer.add(søker)

    barnasIdenter.map {
        personopplysningGrunnlag.personer.add(Person(aktørId = randomAktørId(),
                                                     personIdent = PersonIdent(it),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = LocalDate.of(2019, 1, 1),
                                                     navn = "",
                                                     kjønn = Kjønn.MANN))
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

fun lagPeriodeResultaterForSøkerOgToBarn(behandlingResultat: BehandlingResultat, søkerFnr: String, barn1Fnr: String, barn2Fnr: String, stønadFom: LocalDate, stønadTom: LocalDate): Set<PeriodeResultat> {
    return setOf(
            lagPeriodeResultat(
                    søkerFnr,
                    behandlingResultat = behandlingResultat,
                    resultat = Resultat.JA,
                    periodeFom = stønadFom,
                    periodeTom = stønadTom
            ),
            lagPeriodeResultat(
                    barn1Fnr,
                    behandlingResultat = behandlingResultat,
                    resultat = Resultat.JA,
                    periodeFom = stønadFom,
                    periodeTom = stønadTom
            ),
            lagPeriodeResultat(
                    barn2Fnr,
                    behandlingResultat = behandlingResultat,
                    resultat = Resultat.JA,
                    periodeFom = stønadFom,
                    periodeTom = stønadTom
            )
    )
}

fun lagPeriodeResultat(fnr: String, resultat: Resultat, periodeFom: LocalDate?, periodeTom: LocalDate?, behandlingResultat: BehandlingResultat): PeriodeResultat {
    val periodeResultat = PeriodeResultat(
            behandlingResultat = behandlingResultat,
            personIdent = fnr,
            periodeFom = periodeFom,
            periodeTom = periodeTom)
    periodeResultat.vilkårResultater =
            setOf(VilkårResultat(periodeResultat = periodeResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = resultat,
                    begrunnelse = ""))
    return periodeResultat
}

fun lagBehandlingResultat(fnr: String, behandling: Behandling, resultat: Resultat): BehandlingResultat {
    val behandlingResultat = BehandlingResultat(
            behandling = behandling
    )
    val personResultat = PersonResultat(
            behandlingResultat = behandlingResultat,
            personIdent = fnr)
    personResultat.vilkårResultater =
            setOf(VilkårResultat(personResultat = personResultat,
                                 vilkårType = Vilkår.BOSATT_I_RIKET,
                                 resultat = resultat,
                                 periodeFom = LocalDate.now(),
                                 periodeTom = LocalDate.now(),
                                 begrunnelse = ""))
    behandlingResultat.personResultater = setOf(personResultat)
    return behandlingResultat
}