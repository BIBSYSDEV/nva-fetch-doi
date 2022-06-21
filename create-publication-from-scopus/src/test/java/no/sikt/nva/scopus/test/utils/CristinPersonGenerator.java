package no.sikt.nva.scopus.test.utils;

import com.google.gson.Gson;
import no.sikt.nva.scopus.conversion.model.cristin.Person;

public class CristinPersonGenerator {

    public static Person generateCristinPerson(String cristinId, String firstname, String surname) {
        return new Person(cristinId, firstname, surname, false, null);
    }

    public static String convertToJson(Person person){
        var gson = new Gson();
        return gson.toJson(person);
    }

}
