package no.sikt.nva.scopus.factory;

import no.scopus.generated.*;
import no.sikt.nva.scopus.model.Channel;
import nva.commons.core.JacocoGenerated;

public class ScopusChannelFactory {

    @JacocoGenerated
    public ScopusChannelFactory() {}

    public static Channel buildChannel(DocTp docTp) {
        Channel channel = new Channel(
                formatIssn(docTp.getMeta().getIssn()),
                docTp.getMeta().getVolume(),
                docTp.getMeta().getIssue());
        fetchSourceData(docTp.getItem().getItem().getBibrecord().getHead().getSource(), channel);

        return channel;
    }


    private static void fetchSourceData(SourceTp sourceTp, Channel channel) {
        if (sourceTp == null) return;

        channel.setExternalId(sourceTp.getSrcid());
        channel.setExternalName(fetchSourceName(sourceTp.getSourcetitle()));
        channel.setCountry(sourceTp.getCountry());
        channel.setArticleNr(sourceTp.getArticleNumber());

        StringBuilder publisherName = new StringBuilder();
        sourceTp.getPublisher().stream().forEach(p -> publisherName.append(p.getPublishername()));
        channel.setPublisherName(publisherName.toString());

        sourceTp.getIsbn().forEach(isbnTp -> {
            channel.getIsbns().add(isbnTp.getContent());
        });
        sourceTp.getIssn().forEach(issnTp -> {
            if ("electronic".equals(issnTp.getType())) {
                channel.setEissn(formatIssn(issnTp.getContent()));
            } else if ("print".equals(issnTp.getType())) {
                channel.setIssn(formatIssn(issnTp.getContent()));
            }
        });

        if (sourceTp.getVolisspag() != null) {
            sourceTp.getVolisspag().getContent().forEach(elm -> {
                if (elm.getName().toString().equals("supplement")) {
                    channel.setSupplement(elm.getValue().toString());
                }
                if (elm.getName().toString().equals("pagerange")) {
                    PagerangeTp pagerangeTp = (PagerangeTp)elm.getValue();
                    channel.setPageFrom(pagerangeTp.getFirst());
                    channel.setPageTo(pagerangeTp.getLast());
                }
                if (elm.getName().toString().equals("pagecount")) {
                    PagecountTp pagecountTp = (PagecountTp)elm.getValue();
                    channel.setNumberOfPages(Integer.valueOf(pagecountTp.getContent()));
                }
                if (elm.getName().toString().equals("voliss")) {
                    VolissTp volissTp = (VolissTp)elm.getValue();
                    if (channel.getVolume() == null) channel.setVolume(volissTp.getVolume());
                    if (channel.getIssue() == null) channel.setIssue(volissTp.getIssue());
                }
            });
        }
    }


    private static String fetchSourceName(SourcetitleTp sourcetitleTp) {

        if (sourcetitleTp == null || sourcetitleTp.getContent() == null) return null;

        StringBuilder name = new StringBuilder();
        sourcetitleTp.getContent().stream().forEach(name::append);
        return name.toString();
    }


    protected static String formatIssn(String issn) {

        if (issn != null) {
            issn = issn.replaceAll(" ","");
            if (issn.length() == 8){
                StringBuilder sb = new StringBuilder(issn);
                sb.insert(4, "-");
                return sb.toString();
            }
        }

        return issn;
    }
}
