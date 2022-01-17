package no.sikt.nva.scopus.xmlconverter.factory;

import no.scopus.generated.*;
import no.sikt.nva.scopus.xmlconverter.ScopusUtil;
import no.sikt.nva.scopus.xmlconverter.model.Language;
import nva.commons.core.JacocoGenerated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScopusLanguageFactory {

    @JacocoGenerated
    public ScopusLanguageFactory() {}

    public static List<Language> buildLanguages(DocTp docTp) {
        Map<String, Language> map = new LinkedHashMap<>();

        fetchTitles(docTp.getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext(),map);
        fetchAbstracts(docTp.getItem().getItem().getBibrecord().getHead().getAbstracts(),map);
        setCorrectStatusOriginal(map);

        return map.values().stream().collect(Collectors.toList());
    }

    private static void fetchTitles(List<TitletextTp> titletextTps, Map<String,Language> map) {
        if (titletextTps == null) return;

        for (TitletextTp titletextTp : titletextTps) {
            String lang = titletextTp.getLang();
            boolean original = "y".equalsIgnoreCase(titletextTp.getOriginal().value());
            String title = ScopusUtil.extractTextFromDataWithSupInfTags(titletextTp.getContent());

            map.put(lang,new Language(lang,title, null, original));
        }
    }

    private static void fetchAbstracts(AbstractsTp abstractsTps, Map<String,Language> map) {
        if (abstractsTps == null) return;

        boolean noneHasStatusOriginal = map.entrySet().stream().noneMatch(entry -> entry.getValue().isOriginal());

        for (AbstractTp abstractTp : abstractsTps.getAbstract()) {
            String lang = abstractTp.getLang();
            String original = abstractTp.getOriginal().value();

            StringBuilder sb = new StringBuilder();
            for (Para paragraph : abstractTp.getPara()) {
                sb.append(ScopusUtil.extractTextFromDataWithSupInfTags(paragraph.getContent()));
            }

            if (map.get(lang) != null) {
                map.get(lang).setSummary(sb.toString());
                if ("y".equalsIgnoreCase(original) && noneHasStatusOriginal) {
                    map.get(lang).setOriginal(true);
                }
            } else {
                Language text = new Language(lang, null, sb.toString(),"y".equalsIgnoreCase(original) && noneHasStatusOriginal);
                map.put(lang,text);
            }
        }
    }

    // If no one or more than one status original is set to yes, if has english, set only that to yes, or if not, set the first one only to yes
    protected static void setCorrectStatusOriginal(Map<String, Language> map) {
        int numberOfYesInStatusOriginal = 0;
        String firstStatusOriginal = null;

        for (Map.Entry<String,Language> entry : map.entrySet()) {
            if (entry.getValue().isOriginal()) {
                numberOfYesInStatusOriginal ++;
                if (firstStatusOriginal == null) {
                    firstStatusOriginal = entry.getKey();
                }
            }
        }

        if (numberOfYesInStatusOriginal != 1) {
            if (numberOfYesInStatusOriginal > 1) {
                map.entrySet().iterator().forEachRemaining(entry -> entry.getValue().setOriginal(false));
            }
            if (map.containsKey("eng")) {
                map.get("eng").setOriginal(true);
            } else {
                if (firstStatusOriginal != null) {
                    map.get(firstStatusOriginal).setOriginal(true);
                } else {
                    map.entrySet().iterator().next().getValue().setOriginal(true);
                }
            }
        }
    }
}
