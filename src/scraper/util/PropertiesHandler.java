package scraper.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public abstract class PropertiesHandler {

	private static String pullrequestsList;
	private static String githubAuthToken;

	// TODO currently it assumes that all the fields are defined
	public static void loadProperties() throws IOException {
		final Properties prop = new Properties();
		final FileInputStream in = new FileInputStream("res/config");
		prop.load(in);
		in.close();

		pullrequestsList = prop.getProperty("pullrequests_list");
		githubAuthToken = prop.getProperty("github_auth_token");
	}

	public static String getPullrequestsList() {
		return pullrequestsList;
	}

	public static String getGithubAuthToken() {
		return githubAuthToken;
	}

}
