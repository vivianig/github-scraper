package scraper.util.patterns;

import java.util.regex.Pattern;

class UrlRelationMatcher extends PatternFinder {

	UrlRelationMatcher() {
		super.pattern = Pattern.compile("rel=\"(.*?)\"");
	}

}
