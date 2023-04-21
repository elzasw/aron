package cz.aron.domain;

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

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public boolean isValueFromEstimated() {
		return valueFromEstimated;
	}

	public void setValueFromEstimated(boolean valueFromEstimated) {
		this.valueFromEstimated = valueFromEstimated;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public boolean isValueToEstimated() {
		return valueToEstimated;
	}

	public void setValueToEstimated(boolean valueToEstimated) {
		this.valueToEstimated = valueToEstimated;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}    
    
}
