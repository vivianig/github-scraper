package scraper.util.patterns;

import java.util.regex.Pattern;

public abstract class PatternReplacer {

	protected Pattern pattern;
	protected String replacement;

	public String replace(String content) {
		return pattern.matcher(content).replaceAll(replacement);
	}

}