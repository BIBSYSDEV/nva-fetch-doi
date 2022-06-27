package no.sikt.nva.scopus.test.utils;

import com.google.gson.Gson;
import java.net.URI;
import java.util.Set;
import no.sikt.nva.scopus.conversion.model.cristin.Person;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;

public class CristinPersonGenerator {

    public static Person generateCristinPerson(URI cristinId, String firstname, String surname) {
        var names = Set.of(new TypedValue("FirstName", firstname), new TypedValue("LastName", surname));
        return new Person.Builder().withId(cristinId).withNames(names).build();
    }

    public static String convertToJson(Person person) {
        var gson = new Gson();
        return gson.toJson(person);
    }
}
