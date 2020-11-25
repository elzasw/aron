package cz.aron.core.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Lukas Jane (inQool) 05.11.2020.
 */
@Getter
@Setter
public class UniversalDate {
    private String from;

    /**
     * Signalise that the {@link #from} is an estimate value
     */
    private boolean valueFromEstimated;

    private String to;

    /**
     * Signalise that the {@link #to} is an estimate value
     */
    private boolean valueToEstimated;

    /**
     * Stored date format.
     * <p>
     * Available format characters:
     * <ul>
     *   <li><strong>C</strong> - century</li>
     *   <li><strong>Y</strong> - year</li>
     *   <li><strong>YM</strong> - year/month</li>
     *   <li><strong>D</strong> - date (year/month/day)</li>
     *   <li><strong>DT</strong> - datetime</li>
     *   <li><strong>-</strong> - interval separator</li>
     * </ul>
     * <p>
     * Format syntax options:
     * <ul>
     *   <li>one value (i.e.: {@code Y})</li>
     *   <li>interval (i.e.: {@code Y-Y})</li>
     *   <li>one-sided interval (i.e.: {@code Y-}) NOTE one sided intervals can't actually be used in this class</li>
     * </ul>
     *
     * @see <a href="https://frnk.lightcomp.cz/download/cam/modely/index.html?goto=9:3:1:308">TimeInterval#fmt</a>
     */
    private String format;
}
