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
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPersonYtelsesperiode
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.økonomi.sats
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

fun randomFnr(): String = UUID.randomUUID().toString()
fun randomAktørId(): AktørId = AktørId(UUID.randomUUID().toString())

private var gjeldendeVedtakId: Long = 1
private var gjeldendeBehandlingId: Long = 1
private var gjeldendePersonId: Long = 1
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
        id= nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = PersonIdent(randomFnr()),
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = Kjønn.MANN
)

fun lagVedtak(behandling: Behandling = lagBehandling(),
              resultat: BrevType = BrevType.INNVILGET,
              forrigeVedtak: Vedtak? = null,
              opphørsdato: LocalDate? = null) =
        Vedtak(id = nesteVedtakId(),
               behandling = behandling.copy(
                       brevType = resultat
               ),
               ansvarligSaksbehandler = "ansvarligSaksbehandler",
               vedtaksdato = LocalDate.now(),
               stønadBrevMarkdown = "",
               forrigeVedtakId = forrigeVedtak?.id,
               opphørsdato = opphørsdato
        )

fun lagPersonVedtak(fom: String,
                    tom: String,
                    ytelsetype: Ytelsetype = Ytelsetype.ORDINÆR_BARNETRYGD,
                    beløp: Int = sats(ytelsetype),
                    vedtak: Vedtak = lagVedtak()): VedtakPersonYtelsesperiode {
    return VedtakPersonYtelsesperiode(
            personId = tilfeldigPerson().id,
            vedtakId = vedtak.id, beløp = beløp, stønadFom = dato(fom), stønadTom = dato(tom), type = ytelsetype)
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