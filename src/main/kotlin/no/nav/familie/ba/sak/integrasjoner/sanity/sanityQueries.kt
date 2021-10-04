package no.nav.familie.ba.sak.kjerne.dokument

val hentDokumenter =
        "*[_type == \"begrunnelse\"]{\n" +
        "     apiNavn,\n" +
        "     navnISystem,\n" +
        "     hjemler,\n" +
        "     hjemlerFolketrygdloven,\n" +
        "     vilkaar,  \n" +
        "     rolle,   \n" +
        "     lovligOppholdTriggere,\n" +
        "     bosattIRiketTriggere,\n" +
        "     giftPartnerskapTriggere,\n" +
        "     borMedSokerTriggere,\n" +
        "     ovrigeTriggere\n" +
        " }"