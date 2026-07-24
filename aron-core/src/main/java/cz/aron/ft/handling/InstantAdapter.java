package cz.aron.ft.handling;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.Instant;

public class InstantAdapter extends XmlAdapter<String, Instant> {
    public Instant unmarshal(String v) {
        return Instant.parse(v);
    }

    public String marshal(Instant v) {
        return v.toString();
    }
}
