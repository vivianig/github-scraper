package scraper.util.patterns;

import java.util.regex.Pattern;

class LineTerminatorReplacer extends PatternReplacer {

	LineTerminatorReplacer() {
		super.pattern = Pattern.compile(String.format("%s{3,}", System.lineSeparator()), Pattern.MULTILINE);
		super.replacement = String.format("%s%s", System.lineSeparator(), System.lineSeparator());
	}

}
