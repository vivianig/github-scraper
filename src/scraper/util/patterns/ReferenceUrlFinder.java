package scraper.util.patterns;

import java.util.regex.Pattern;

class ReferenceUrlFinder extends PatternFinder {

	ReferenceUrlFinder() {
		super.pattern = Pattern.compile("<(.*?)>");
	}

}
