package scraper.util.patterns;

public abstract class Patterns {

	public static PatternReplacer codeSnippet = new CodeSnippetReplacer();
	public static PatternReplacer codeBlock = new CodeBlockReplacer();
	public static PatternReplacer commentBlock = new CommentBlockReplacer();
	public static PatternReplacer lineTerminator = new LineTerminatorReplacer();

	public static PatternFinder referenceUrl = new ReferenceUrlFinder();
	public static PatternFinder urlRelation = new UrlRelationMatcher();
}
