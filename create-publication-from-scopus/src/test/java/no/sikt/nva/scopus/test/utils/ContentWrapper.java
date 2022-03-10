package no.sikt.nva.scopus.test.utils;

import java.io.Serializable;
import java.util.List;

public class ContentWrapper {

    private final List<Serializable> contentList;

    public ContentWrapper(List<Serializable> contentList) {
        this.contentList = contentList;
    }

    public List<Serializable> getContentList() {
        return contentList;
    }
}
