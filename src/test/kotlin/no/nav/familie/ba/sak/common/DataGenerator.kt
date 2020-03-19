package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
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
private val id_inkrement = 50

fun nesteVedtakId(): Long {
    gjeldendeVedtakId += id_inkrement
    return gjeldendeVedtakId
}

fun nesteBehandlingId(): Long {
    gjeldendeBehandlingId += id_inkrement
    return gjeldendeBehandlingId
}

val defaultFagsak = Fagsak(1, AktørId("1"), PersonIdent("12345"), FagsakStatus.OPPRETTET)
fun lagBehandling(fagsak: Fagsak = defaultFagsak) = Behandling(id = nesteBehandlingId(),
                                                               fagsak = fagsak,
                                                               type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                               kategori = BehandlingKategori.NASJONAL,
                                                               underkategori = BehandlingUnderkategori.ORDINÆR)

fun tilfeldigPerson(fødselsdato: LocalDate = LocalDate.now(), personType: PersonType = PersonType.BARN) = Person(
        aktørId = randomAktørId(),
        personIdent = PersonIdent(randomFnr()),
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = Kjønn.MANN
)

fun lagVedtak(behandling: Behandling = lagBehandling(),
              resultat: BehandlingResultat = BehandlingResultat.INNVILGET,
              forrigeVedtak: Vedtak? = null,
              opphørsdato: LocalDate? = null) =
        Vedtak(id = nesteVedtakId(),
               behandling = behandling.copy(
                       resultat = resultat
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
                    vedtak: Vedtak = lagVedtak()): VedtakPerson {
    return VedtakPerson(
            person = tilfeldigPerson(),
            stønadFom = dato(fom),
            stønadTom = dato(tom),
            beløp = beløp,
            vedtak = vedtak,
            type = ytelsetype
    )
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

fun nyOrdinærBehandling(søkersIdent: String, barnasIdenter: List<String>): NyBehandling = NyBehandling(
        søkersIdent = søkersIdent,
        barnasIdenter = barnasIdenter,
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR
)

fun nyRevurdering(søkersIdent: String, barnasIdenter: List<String>): NyBehandling = NyBehandling(
        søkersIdent = søkersIdent,
        barnasIdenter = barnasIdenter,
        behandlingType = BehandlingType.REVURDERING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR
)