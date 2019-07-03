package scraper;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import scraper.data.gson.GSONComment;

public abstract class GSONHandler {

	private static final Gson gson = new Gson();

	public static List<GSONComment> parseComments(List<String> jsonStrings) {
		final List<GSONComment> comments = new LinkedList<>();
		for (final String comment : jsonStrings) {
			comments.addAll(gson.fromJson(comment, new TypeToken<Collection<GSONComment>>() {
			}.getType()));
		}
		return comments;

	}

	public static GSONComment parseComment(String jsonComment) {
		return gson.fromJson(jsonComment, new TypeToken<GSONComment>() {
		}.getType());

	}

}
