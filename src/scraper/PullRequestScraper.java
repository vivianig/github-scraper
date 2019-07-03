package scraper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonSyntaxException;

import scraper.data.gson.GSONComment;

//TODO consider static
public class PullRequestScraper {

	private final Logger logger = LogManager.getLogger(PullRequestScraper.class);

	private String prBaseUrl;
	private String commentsBaseUrl;
	private String threadsBaseUrl;
	private String diffUrl;

	/**
	 * Pulls and saves the data from Github
	 *
	 * @param repositoryName The full name of the repository to scrape
	 * @param pullRequests   The list of pull requests numbers to scrape
	 */
	void scrapePullRequests(final String repositoryName, final List<String> pullRequests) {
		System.setOut(new PrintStream(new OutputStream() {

			@Override
			public void write(final int b) throws IOException {
			}

		}, true));
		this.prBaseUrl = "https://api.github.com/repos/" + repositoryName + "pulls/";
		this.threadsBaseUrl = "https://api.github.com/repos/" + repositoryName + "pulls/";
		this.commentsBaseUrl = "https://api.github.com/repos/" + repositoryName + "issues/";
		this.commentsBaseUrl = "https://api.github.com/repos/" + repositoryName + "issues/";
		this.diffUrl = "https://github.com/" + repositoryName + "/pull/";

		this.logger.trace(String.format("Generating urls for repository %s", repositoryName));
		final List<String[]> prUrls = generateUrls(pullRequests);
		this.logger.trace(String.format("Generated urls for repository %s", repositoryName));
		for (final String[] urls : prUrls) {// TODO parallelize this
			final String prName = urls[0];
			this.logger.trace(String.format("Extracting comments for pull request %s", prName));

			final String prUrl = urls[1];
			final String commentsUrl = urls[2];
			final String threadsUrl = urls[3];
			final String diffUrl = urls[4];

			final GSONComment openingComment = getComment(prUrl);
			final List<GSONComment> comments = getComments(commentsUrl);
			if (comments == null) {
				continue;
			}
			comments.add(0, openingComment);
			final List<GSONComment> threads = getComments(threadsUrl);
			final String diff = getDiff(diffUrl);

			final String files = getFiles(repositoryName, prName);
			try {
				DatabaseHandler.saveDiscussion(repositoryName, prName, comments, threads, diff, files);
			} catch (final Exception e) {
				e.printStackTrace();
				System.err.println("Failed: " + prName);
			}
			this.logger.trace(String.format("Comments extracted for pull request %s", prName));

		}

	}

	private String getFiles(final String repositoryName, final String prName) {
		try {
			return String.join(",",
					GithubHandler.getFilesChanged(repositoryName.substring(0, repositoryName.length() - 1), prName));
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Generate all the URLs to be collected
	 *
	 * @param pullRequests List of pull requests
	 * @return A string array of length 3, containing the number of the pr and the
	 *         two URLs for comments
	 */
	private List<String[]> generateUrls(final List<String> pullRequests) {
		return pullRequests.stream()
				.map(pr -> new String[] { pr, this.prBaseUrl + pr, this.commentsBaseUrl + pr + "/comments",
						this.threadsBaseUrl + pr + "/comments", this.diffUrl + pr + ".diff" })
				.collect(Collectors.toList());
	}

	/**
	 *
	 * @param url Base URL for getting the comments from the REST API of GitHub
	 * @return An array of comments
	 */
	private List<GSONComment> getComments(final String url) {
		try {
			final List<String> pages = GithubHandler.getComments(url);

			return GSONHandler.parseComments(pages);
		} catch (final IOException e) {
			this.logger.error(String.format("Comments extraction failed for URL %s", url));
			e.printStackTrace();
			return null;
		} catch (final JsonSyntaxException e) {
			System.err.println("Caught correctly");
			return null;
		}
	}

	private GSONComment getComment(final String url) {
		try {
			final String page = GithubHandler.getComment(url);
			return GSONHandler.parseComment(page);
		} catch (final IOException e) {
			this.logger.error(String.format("Comments extraction failed for URL %s", url));
			e.printStackTrace();
			return null;
		} catch (final JsonSyntaxException e) {
			System.err.println("Caught correctly");
			return null;
		}
	}

	private String getDiff(final String url) {
		try {
			final String diff = GithubHandler.getDiff(url);
			return diff;
		} catch (final IOException e) {
			this.logger.error(String.format("Comments extraction failed for URL %s", url));
			e.printStackTrace();
			return null;
		} catch (final JsonSyntaxException e) {
			System.err.println("Caught correctly");
			return null;
		}
	}

}
