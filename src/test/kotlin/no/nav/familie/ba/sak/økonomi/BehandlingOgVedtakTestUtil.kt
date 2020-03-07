package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakPerson
import no.nav.familie.ba.sak.behandling.vedtak.Ytelsetype
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.fpsak.tidsserie.LocalDateSegment
import java.time.LocalDate
import java.util.*

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
fun lagBehandling() = Behandling(id = nesteBehandlingId(),
                                 fagsak = defaultFagsak,
                                 type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                 kategori = BehandlingKategori.NASJONAL,
                                 underkategori = BehandlingUnderkategori.ORDINÆR)

fun tilfeldigPerson(fødselsdato: LocalDate = LocalDate.now(), personType: PersonType = PersonType.BARN) = Person(
        personIdent = PersonIdent(UUID.randomUUID().toString().substring(0, 18)),
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0)
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

fun sats(ytelsetype: Ytelsetype) =
        when (ytelsetype) {
            Ytelsetype.ORDINÆR_BARNETRYGD -> 1054
            Ytelsetype.UTVIDET_BARNETRYGD -> 1054
            Ytelsetype.SMÅBARNSTILLEGG -> 660
            Ytelsetype.EØS->0
            Ytelsetype.MANUELL_VURDERING->0
        }

fun lagSegmentBeløp(fom: String, tom: String, beløp: Int): LocalDateSegment<Int> =
        LocalDateSegment(dato(fom), dato(tom), beløp)

fun dato(s: String) = LocalDate.parse(s)


