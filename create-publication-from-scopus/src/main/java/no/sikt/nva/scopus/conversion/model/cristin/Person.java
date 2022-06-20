package no.sikt.nva.scopus.conversion.model.cristin;

import java.util.List;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.FormalParameterNamingConventions", "PMD.MethodNamingConventions"})
public class Person {

    private final String cristin_person_id;

    private final String first_name;

    private final String surname;

    private final boolean identified_cristin_person;

    private final List<Affiliation> affiliations;

    @JacocoGenerated
    public Person(String cristin_person_id, String first_name, String surname, boolean identified_cristin_person,
                  List<Affiliation> affiliations) {
        this.cristin_person_id = cristin_person_id;
        this.first_name = first_name;
        this.surname = surname;
        this.identified_cristin_person = identified_cristin_person;
        this.affiliations = affiliations;
    }

    @JacocoGenerated
    public String getCristin_person_id() {
        return cristin_person_id;
    }

    @JacocoGenerated
    public String getFirst_name() {
        return first_name;
    }

    @JacocoGenerated
    public String getSurname() {
        return surname;
    }

    @JacocoGenerated
    public boolean isIdentified_cristin_person() {
        return identified_cristin_person;
    }

    @JacocoGenerated
    public List<Affiliation> getAffiliations() {
        return affiliations;
    }
}
