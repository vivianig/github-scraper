package scraper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import db.DatabaseHandler;
import scraper.util.PropertiesHandler;

public class DataExtractor {
	private final static Logger logger = LogManager.getLogger(DataExtractor.class);

	public static void main(final String[] args) throws IOException {
		try {
			PropertiesHandler.loadProperties();
		} catch (final IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		final Options options = new Options();
		options.addOption(Option.builder("r").required(false).hasArg(true).longOpt("repo")
				.desc("Repository to be scraped").build());
		CommandLine commandLine = null;
		try {
			final CommandLineParser cmdLineParser = new DefaultParser();
			commandLine = cmdLineParser.parse(options, args);
		} catch (final ParseException parseException) {
			System.err.println("ERROR: Invalid args ");
			System.exit(1);
		}

		final PullRequestScraper scraper = new PullRequestScraper();
		if (commandLine.hasOption("repo")) {
			final String repo = commandLine.getOptionValue("repo");
			final List<String> pullRequests = scrapePullRequestList(repo);

			scraper.scrapePullRequests(repo + "/", pullRequests);
		} else {
			final Map<String, List<String>> pullrequests = parsePullRequestList();
			for (final String repository : pullrequests.keySet()) {
				logger.trace(String.format("Scraping repository: %s", repository));
				scraper.scrapePullRequests(repository, pullrequests.get(repository));
			}
		}

	}

	private static List<String> scrapePullRequestList(final String repo) throws IOException {
		final File f = new File(String.format("res/pullrequests_%s", repo.replace("/", "-")));

		if (f.exists()) {

			final List<String> prs = new ArrayList<>();
			final BufferedReader bf = new BufferedReader(new FileReader(f));
			String output;
			final List<String> discussions = DatabaseHandler.getDiscussionsNames();
			while ((output = bf.readLine()) != null) {
				if (!discussions.contains(repo.split("/")[1] + "_" + output.trim())) {
					prs.add(output.trim());
				}
			}
			bf.close();
			final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			for (final String s : prs) {
				bw.write(s + "\n");
			}
			bw.close();
			System.out.println(prs.size() + " PRs to parse remaining.");
			return prs;
		} else {
			try {
				final List<String> prs = GithubHandler.getPullRequestsFromRepo(repo);
				final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
				final List<String> discussions = DatabaseHandler.getDiscussionsNames();
				for (final String s : prs) {
					if (!discussions.contains(repo.split("/")[1] + "_" + s.trim())) {
						bw.write(s + "\n");
					}
				}
				bw.close();
				return prs;
			} catch (final IOException | UnsupportedOperationException | InterruptedException e) {
				System.err.println("Something went wrong with recovering the list of pull requests");
				e.printStackTrace();
			}
		}
		return new ArrayList<>();
	}

	private static Map<String, List<String>> parsePullRequestList() {
		logger.trace("Loading pullrequests file");
		final Map<String, List<String>> pullrequests = new HashMap<>();
		try {
			String line;
			Integer prCounter = 0;
			System.out.println(PropertiesHandler.getPullrequestsList());
			final BufferedReader reader = new BufferedReader(new FileReader(PropertiesHandler.getPullrequestsList()));
			while ((line = reader.readLine()) != null) {
				final String[] pr = line.split(" ", -1);
				if (pr.length != 2) {
					logger.warn(String.format("Badly formatted pullrequest: %s. Skipping line", line));
				} else {
					if (!pullrequests.containsKey(pr[0])) {
						pullrequests.put(pr[0], new ArrayList<>());
						logger.trace(String.format("Found new repository: %s", pr[0]));
					}
					prCounter += 1;
					pullrequests.get(pr[0]).add(pr[1]);
				}
				logger.trace("Loading completed");
				logger.trace(String.format("Found %d pullrequests in %d repositories", prCounter, pullrequests.size()));
			}
			reader.close();
		} catch (final FileNotFoundException e) {
			logger.error("Pullrequests file could not be found");
			System.exit(1);// TODO define those

		} catch (final IOException e) {
			logger.error("IO Error");
			logger.error(e);
			System.exit(1);
		}
		return pullrequests;
	}

}