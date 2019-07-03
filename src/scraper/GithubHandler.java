package scraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.PullRequestService;

import scraper.util.PropertiesHandler;
import scraper.util.patterns.Patterns;

abstract class GithubHandler {

	private static final Logger logger = LogManager.getLogger(GithubHandler.class);
	private static final CloseableHttpClient oldClient = HttpClientBuilder.create().build();

	private static final Header authorizationToken = new BasicHeader("Authorization",
			String.format("token %s", PropertiesHandler.getGithubAuthToken()));

	private static Date startingTIme;
	private static final GitHubClient client = new GitHubClient()
			.setOAuth2Token(PropertiesHandler.getGithubAuthToken());

	static List<String> getPullRequestsFromRepo(final String repo)
			throws UnsupportedOperationException, IOException, InterruptedException {

		final List<String> prs = new ArrayList<>();

		final PullRequestService service = new PullRequestService(client);

		final RepositoryId repoID = new RepositoryId(repo.split("/")[0], repo.split("/")[1]);
		final PageIterator<PullRequest> prPageIterator = service.pagePullRequests(repoID, "all");

		while (prPageIterator.hasNext()) {
			for (final PullRequest pr : prPageIterator.next()) {
				prs.add(Integer.toString(pr.getNumber()));
			}
			if (client.getRemainingRequests() <= 10) {
				Thread.sleep(300000);
			}
			System.out.println("Remaining " + client.getRemainingRequests());

		}
		return prs;

	}

	/**
	 *
	 * @param url URL for the comments of a Github pull requests. Must satisfy the
	 *            Github REST API specifications.
	 * @return A list of JSON strings for each JSON objects, one for each page.
	 * @throws ClientProtocolException
	 * @throws IOException
	 */

	static List<String> getComments(final String url) throws ClientProtocolException, IOException {

		logger.trace("Extracting comments");
		final List<String> commentsPages = new ArrayList<>();
		String nextUrl = url;
		final StringBuilder commentsBuilder = new StringBuilder();
		while (nextUrl != null) {
			logger.trace(String.format("Sending HTTP GET request at %s", nextUrl));
			final HttpGet get = new HttpGet(nextUrl);
			get.addHeader(authorizationToken);
			get.addHeader("accept", "application/json");

			final CloseableHttpResponse res = oldClient.execute(get);

			logger.trace(String.format("HTTP responsed received from %s", nextUrl));
			handleRateLimit(Integer.parseInt(res.getHeaders("X-RateLimit-Remaining")[0].getValue().trim()));

			final BufferedReader br = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));

			logger.trace("Parsing response");
			String output;
			while ((output = br.readLine()) != null) {
				commentsBuilder.append(output);
			}

			commentsPages.add(commentsBuilder.toString());
			commentsBuilder.setLength(0);
			logger.trace("Response parsed");

			nextUrl = extractNextLink(res.getHeaders("Link"));

			logger.trace("Closing connection");
			res.close();

		}
		logger.trace("Comments Extracted");
		return commentsPages;
	}

	private static void handleRateLimit(final int remaining) {
		System.out.println("Remaining: " + remaining);
		if (startingTIme == null) {
			startingTIme = new Date();
		}
		if (remaining < 50) {
			try {
				final long timePassed = new Date().getTime() - startingTIme.getTime();
				if (timePassed < TimeUnit.HOURS.toMillis(1)) {
					Thread.sleep(TimeUnit.HOURS.toMillis(1) - (new Date().getTime() - startingTIme.getTime()));
				}
				startingTIme = null;
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public static List<String> getFilesChanged(final String repo, final String pr) throws IOException {
		final Integer prID = Integer.parseInt(pr);

		final PullRequestService service = new PullRequestService(client);
		final RepositoryId repoID = new RepositoryId(repo.split("/")[0], repo.split("/")[1]);
		final List<String> files = service.getFiles(repoID, prID).stream().map(f -> f.getFilename())
				.collect(Collectors.toList());
		return files;
	}

	// TODO either use link or url, not both

	/**
	 *
	 * @param linkHeaders 'Link' header of the response.
	 * @return The link for the next page of comments, or null if no link has been
	 *         found.
	 */
	private static String extractNextLink(final Header[] linkHeaders) {

		logger.trace("Checking for additional pages");
		if (linkHeaders.length == 0) {
			logger.trace("No additional page found");
			return null;
		}

		for (final String link : linkHeaders[0].getValue().split(",")) {
			String[] matchedStrings = Patterns.urlRelation.find(link);
			if (matchedStrings.length != 0 && !matchedStrings[0].equals("next")) {
				continue;
			}
			matchedStrings = Patterns.referenceUrl.find(link);
			if (matchedStrings.length != 0) {
				logger.trace("Additional page found");
				return matchedStrings[0];
			}

		}
		logger.trace("No additional page found");
		return null;

	}

	public static String getComment(final String url) throws ClientProtocolException, IOException {
		logger.trace("Extracting base comment");
		final StringBuilder commentsBuilder = new StringBuilder();
		logger.trace(String.format("Sending HTTP GET request at %s", url));
		final HttpGet get = new HttpGet(url);
		get.addHeader(authorizationToken);
		get.addHeader("accept", "application/json");

		final CloseableHttpResponse res = oldClient.execute(get);

		logger.trace(String.format("HTTP responsed received from %s", url));

		final BufferedReader br = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));

		logger.trace("Parsing response");
		String output;
		while ((output = br.readLine()) != null) {
			commentsBuilder.append(output);
		}

		logger.trace("Response parsed");
		logger.trace("Closing connection");
		res.close();

		return commentsBuilder.toString();
	}

	public static String getDiff(final String url) throws ClientProtocolException, IOException {
		logger.trace("Extracting diff");
		final StringBuilder stringBuilder = new StringBuilder();
		logger.trace(String.format("Sending HTTP GET request at %s", url));
		final HttpGet get = new HttpGet(url);
		get.addHeader(authorizationToken);

		final CloseableHttpResponse res = oldClient.execute(get);

		logger.trace(String.format("HTTP responsed received from %s", url));

		final BufferedReader br = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));

		logger.trace("Parsing response");
		String output;
		while ((output = br.readLine()) != null) {
			stringBuilder.append(output);
		}

		logger.trace("Response parsed");
		logger.trace("Closing connection");
		res.close();

		return stringBuilder.toString();
	}
}
