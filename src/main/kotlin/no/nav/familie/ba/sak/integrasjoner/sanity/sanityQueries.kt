package no.nav.familie.ba.sak.kjerne.brev

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
