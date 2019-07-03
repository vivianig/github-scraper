package scraper.util.patterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PatternFinder {

	protected Pattern pattern;

	public String[] find(String content) {
		final Matcher matcher = pattern.matcher(content);
		if (matcher.find()) {
			final String[] groups = new String[matcher.groupCount()];
			for (int i = 0; i < matcher.groupCount(); i++) {
				groups[i] = matcher.group(i + 1);
			}
			return groups;
		} else {
			return new String[] {};
		}
	}

}