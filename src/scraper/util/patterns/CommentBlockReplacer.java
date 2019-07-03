package scraper.util.patterns;

import java.util.regex.Pattern;

class CommentBlockReplacer extends PatternReplacer {
	CommentBlockReplacer() {
		super.pattern = Pattern.compile("(```){1,}?(.+?)(```){1,}", Pattern.DOTALL | Pattern.MULTILINE);
		super.replacement = "";
	}
}
