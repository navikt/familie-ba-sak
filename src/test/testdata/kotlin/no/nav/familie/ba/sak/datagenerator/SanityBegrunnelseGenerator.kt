package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelseDto
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Tema
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårRolle
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.BarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun lagRestSanityBegrunnelse(
    apiNavn: String = "",
    navnISystem: String = "",
    vilkaar: List<String>? = emptyList(),
    rolle: List<String>? = emptyList(),
    lovligOppholdTriggere: List<String>? = emptyList(),
    bosattIRiketTriggere: List<String>? = emptyList(),
    giftPartnerskapTriggere: List<String>? = emptyList(),
    borMedSokerTriggere: List<String>? = emptyList(),
    ovrigeTriggere: List<String>? = emptyList(),
    endringsaarsaker: List<String>? = emptyList(),
    hjemler: List<String> = emptyList(),
    hjemlerFolketrygdloven: List<String> = emptyList(),
    endretUtbetalingsperiodeDeltBostedTriggere: String = "",
    endretUtbetalingsperiodeTriggere: List<String>? = emptyList(),
    periodeResultatForPerson: String? = null,
    fagsakType: String? = null,
    regelverk: String? = null,
    brevPeriodeType: String? = null,
    begrunnelseTypeForPerson: String? = null,
    ikkeIBruk: Boolean? = false,
    stotterFritekst: Boolean? = false,
): SanityBegrunnelseDto =
    SanityBegrunnelseDto(
        apiNavn = apiNavn,
        navnISystem = navnISystem,
        vilkaar = vilkaar,
        rolle = rolle,
        lovligOppholdTriggere = lovligOppholdTriggere,
        bosattIRiketTriggere = bosattIRiketTriggere,
        giftPartnerskapTriggere = giftPartnerskapTriggere,
        borMedSokerTriggere = borMedSokerTriggere,
        ovrigeTriggere = ovrigeTriggere,
        endringsaarsaker = endringsaarsaker,
        hjemler = hjemler,
        hjemlerFolketrygdloven = hjemlerFolketrygdloven,
        endretUtbetalingsperiodeDeltBostedUtbetalingTrigger = endretUtbetalingsperiodeDeltBostedTriggere,
        endretUtbetalingsperiodeTriggere = endretUtbetalingsperiodeTriggere,
        periodeResultatForPerson = periodeResultatForPerson,
        fagsakType = fagsakType,
        regelverk = regelverk,
        brevPeriodeType = brevPeriodeType,
        begrunnelseTypeForPerson = begrunnelseTypeForPerson,
        ikkeIBruk = ikkeIBruk,
        stotterFritekst = stotterFritekst,
    )

fun lagSanityBegrunnelse(
    apiNavn: String = "",
    navnISystem: String = "",
    vilkår: Set<Vilkår> = emptySet(),
    rolle: List<VilkårRolle> = emptyList(),
    lovligOppholdTriggere: List<VilkårTrigger> = emptyList(),
    bosattIRiketTriggere: List<VilkårTrigger> = emptyList(),
    giftPartnerskapTriggere: List<VilkårTrigger> = emptyList(),
    borMedSokerTriggere: List<VilkårTrigger> = emptyList(),
    ovrigeTriggere: List<ØvrigTrigger> = emptyList(),
    endringsaarsaker: List<Årsak> = emptyList(),
    hjemler: List<String> = emptyList(),
    hjemlerFolketrygdloven: List<String> = emptyList(),
    endretUtbetalingsperiodeDeltBostedTriggere: EndretUtbetalingsperiodeDeltBostedTriggere? = null,
    endretUtbetalingsperiodeTriggere: List<EndretUtbetalingsperiodeTrigger> = emptyList(),
    resultat: SanityPeriodeResultat? = null,
    fagsakType: FagsakType? = null,
    periodeType: BrevPeriodeType? = null,
    begrunnelseTypeForPerson: VedtakBegrunnelseType? = null,
): SanityBegrunnelse =
    SanityBegrunnelse(
        apiNavn = apiNavn,
        navnISystem = navnISystem,
        vilkår = vilkår,
        rolle = rolle,
        lovligOppholdTriggere = lovligOppholdTriggere,
        bosattIRiketTriggere = bosattIRiketTriggere,
        giftPartnerskapTriggere = giftPartnerskapTriggere,
        borMedSokerTriggere = borMedSokerTriggere,
        øvrigeTriggere = ovrigeTriggere,
        endringsaarsaker = endringsaarsaker,
        hjemler = hjemler,
        hjemlerFolketrygdloven = hjemlerFolketrygdloven,
        endretUtbetalingsperiodeDeltBostedUtbetalingTrigger = endretUtbetalingsperiodeDeltBostedTriggere,
        endretUtbetalingsperiodeTriggere = endretUtbetalingsperiodeTriggere,
        periodeResultat = resultat,
        fagsakType = fagsakType,
        periodeType = periodeType,
        begrunnelseTypeForPerson = begrunnelseTypeForPerson,
    )

fun lagSanityEøsBegrunnelse(
    apiNavn: String = "",
    navnISystem: String = "",
    annenForeldersAktivitet: List<KompetanseAktivitet> = emptyList(),
    barnetsBostedsland: List<BarnetsBostedsland> = emptyList(),
    kompetanseResultat: List<KompetanseResultat> = emptyList(),
    hjemler: List<String> = emptyList(),
    hjemlerFolketrygdloven: List<String> = emptyList(),
    hjemlerEØSForordningen883: List<String> = emptyList(),
    hjemlerEØSForordningen987: List<String> = emptyList(),
    hjemlerSeperasjonsavtalenStorbritannina: List<String> = emptyList(),
    vilkår: List<Vilkår> = emptyList(),
    fagsakType: FagsakType? = null,
    tema: Tema? = null,
    periodeType: BrevPeriodeType? = null,
    valgbarhet: Valgbarhet? = null,
    ovrigeTriggere: List<ØvrigTrigger> = emptyList(),
): SanityEØSBegrunnelse =
    SanityEØSBegrunnelse(
        apiNavn = apiNavn,
        navnISystem = navnISystem,
        annenForeldersAktivitet = annenForeldersAktivitet,
        barnetsBostedsland = barnetsBostedsland,
        kompetanseResultat = kompetanseResultat,
        hjemler = hjemler,
        hjemlerFolketrygdloven = hjemlerFolketrygdloven,
        hjemlerEØSForordningen883 = hjemlerEØSForordningen883,
        hjemlerEØSForordningen987 = hjemlerEØSForordningen987,
        hjemlerSeperasjonsavtalenStorbritannina = hjemlerSeperasjonsavtalenStorbritannina,
        vilkår = vilkår.toSet(),
        fagsakType = fagsakType,
        tema = tema,
        periodeType = periodeType,
        valgbarhet = valgbarhet,
        øvrigeTriggere = ovrigeTriggere,
    )
