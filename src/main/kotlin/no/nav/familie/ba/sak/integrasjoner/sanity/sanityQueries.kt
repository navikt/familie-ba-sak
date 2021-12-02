package no.nav.familie.ba.sak.kjerne.dokument

val hentDokumenter =
    "*[_type == \"begrunnelse\"]{" +
        "apiNavn," +
        "navnISystem," +
        "hjemler," +
        "hjemlerFolketrygdloven," +
        "vilkaar," +
        "rolle," +
        "lovligOppholdTriggere," +
        "bosattIRiketTriggere," +
        "giftPartnerskapTriggere," +
        "borMedSokerTriggere," +
        "ovrigeTriggere," +
        "endretUtbetalingsperiodeTriggere," +
        "endretUtbetalingsperiodeDeltBostedTriggere," +
        "endringsaarsaker," +
        "utvidetBarnetrygdTriggere" +
        "}"
