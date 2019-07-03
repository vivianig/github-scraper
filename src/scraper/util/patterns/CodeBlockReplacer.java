package scraper.util.patterns;

import java.util.regex.Pattern;

class CodeBlockReplacer extends PatternReplacer {
	CodeBlockReplacer() {
		super.pattern = Pattern.compile("<!--.*?-->", Pattern.DOTALL | Pattern.MULTILINE);
		super.replacement = "";
	}
}
