package scraper.util.patterns;

import java.util.regex.Pattern;

class CodeSnippetReplacer extends PatternReplacer {

	CodeSnippetReplacer() {
		super.pattern = Pattern.compile("`([^`]*)?`", Pattern.DOTALL | Pattern.MULTILINE);
		super.replacement = "";
	}

}
