package no.nav.familie.ba.sak.integrasjoner.sanity

const val hentDokumenter =
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
