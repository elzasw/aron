package cz.aron.commons;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private HttpUtils() {
    }

    /**
     * Result of a conditional-request evaluation.
     *
     * @param expired      {@code true} when the client's cached copy is stale and the full
     *                     representation should be sent (200 OK); {@code false} when it is still
     *                     valid and a 304 Not Modified may be returned
     * @param eTag         the (quoted) entity tag of the current representation
     * @param lastModified last-modified timestamp of the representation, in epoch milliseconds
     */
    public record ExpireStatus(boolean expired, String eTag, long lastModified) {
    }

    /**
     * Evaluates the {@code If-None-Match} / {@code If-Modified-Since} conditional request headers
     * against a representation last changed at {@code published}.
     *
     * <p>Per RFC 7232, {@code If-None-Match} takes precedence; when it is present,
     * {@code If-Modified-Since} is ignored.
     *
     * @param published       time the representation was last changed
     * @param ifNoneMatch     value of the {@code If-None-Match} header (may be {@code null})
     * @param ifModifiedSince value of the {@code If-Modified-Since} header (may be {@code null})
     */
    public static ExpireStatus computeExpired(LocalDateTime published, String ifNoneMatch, String ifModifiedSince) {
        return computeExpired(published, ifNoneMatch, ifModifiedSince, null);
    }

    /**
     * As {@link #computeExpired(LocalDateTime, String, String)}, for a representation that also
     * depends on something other than time - the presentation language, say. The variant becomes
     * part of the entity tag, so a client switching variants gets the new representation instead
     * of a 304 for the one it already holds.
     *
     * @param variant discriminator of the representation ({@code null} = time alone decides)
     */
    public static ExpireStatus computeExpired(LocalDateTime published, String ifNoneMatch, String ifModifiedSince,
            String variant) {
        long lastModified = published
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        // ETag is quoted so it round-trips with the value the client echoes back in If-None-Match
        String eTag = variant != null ? "\"" + lastModified + "-" + variant + "\"" : "\"" + lastModified + "\"";

        boolean expired = true;
        if (ifNoneMatch != null) {
            if (eTag.equals(ifNoneMatch.trim())) {
                expired = false;
            }
        } else if (ifModifiedSince != null) {
            try {
                var ifModifiedSinceDate = DateTimeFormatter.RFC_1123_DATE_TIME.parse(ifModifiedSince);
                long ifModifiedSinceSeconds = ifModifiedSinceDate.getLong(ChronoField.INSTANT_SECONDS);
                // not modified when the resource's last change is not newer than the client's copy
                // (If-Modified-Since has second precision, so compare in seconds)
                if (Math.floorDiv(lastModified, 1000L) <= ifModifiedSinceSeconds) {
                    expired = false;
                }
            } catch (DateTimeParseException ignore) {
                log.warn("Fail to parse if-modified-since {}", ifModifiedSince, ignore);
            }
        }
        return new ExpireStatus(expired, eTag, lastModified);
    }
}
