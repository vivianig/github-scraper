package scraper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import db.HibernateUtil;
import model.Comment;
import model.Comment.CommentBuilder;
import model.Discussion;
import model.Discussion.DiscussionBuilder;
import model.Paragraph;
import model.Paragraph.ParagraphBuilder;
import model.Thread;
import model.Thread.ThreadBuilder;
import model.User;
import model.User.UserBuilder;
import scraper.data.gson.GSONComment;
import scraper.data.gson.GSONUser;
import scraper.util.patterns.Patterns;

abstract class DatabaseHandler {

	private final static Logger logger = LogManager.getLogger(DatabaseHandler.class);
	// TODO move to pattern
	private static final String PARAGRAPH_SPLIT_REGEX = "\r\n\r\n";

	private DatabaseHandler() {
	};

	/**
	 * Save everything to the database
	 *
	 * @param repositoryName The name of the repository the pull request belongs to
	 * @param prName         The number of the pull request
	 * @param comments       The list of comments in the pull request
	 * @param threads        The list of threads comments in the pull request
	 * @param diff
	 */
	static void saveDiscussion(final String repositoryName, final String prName, final List<GSONComment> comments,
			final List<GSONComment> threads, final String diff, final String files) {
		// TODO will break if two repositories have the same name but different user
		final String repositoryShortName = repositoryName.split("/")[1];

		logger.trace(String.format("Attempting to write file prs/%s.%s.txt", repositoryShortName, prName));

		final Map<String, List<GSONComment>> clusteredThreads = clusterThreads(threads);

		// TODO sort comments properly
		// TODO remove class
		final Map<String, User> savedUsersIds = db.DatabaseHandler.getUsers();
		final Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaction = session.beginTransaction();

		final Discussion discussion = DiscussionBuilder.build(String.format("%s_%s", repositoryShortName, prName),
				repositoryName);

		discussion.setDiff(diff);
		discussion.setFiles(files);
		session.save(discussion);
		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		for (final GSONComment g_comment : comments) {
			saveComment(session, discussion, savedUsersIds, g_comment);
			session.update(discussion);
			transaction.commit();
			session.clear();
			transaction = session.beginTransaction();
		}

		for (final List<GSONComment> thread : clusteredThreads.values()) {
			saveThread(session, discussion, savedUsersIds, thread);
			session.update(discussion);
			transaction.commit();
			session.clear();
			transaction = session.beginTransaction();
		}

		logger.trace(String.format("Attempting to save %d threads and %d commments", clusteredThreads.size(),
				comments.size()));

		transaction.commit();
		session.close();

	}

	// TODO refactor to globals
	// TODO fix duplication with savecomments
	private static void saveThread(final Session session, final Discussion discussion,
			final Map<String, User> savedUsersIds, final List<GSONComment> g_thread) {
		final Thread thread = ThreadBuilder.build(g_thread.get(0).getCreated_at());

		for (final GSONComment g_comment : g_thread) {
			final Comment comment = buildComment(g_comment);

			final GSONUser g_user = g_comment.getUser();
			User user = null;
			try {
				if (!savedUsersIds.containsKey(g_user.getId())) {
					user = buildUser(g_user);
					savedUsersIds.put(g_user.getId(), user);
					session.save(user);
					discussion.addUser(user);
				} else {
					user = savedUsersIds.get(g_user.getId());
				}

			} catch (final NullPointerException e) {
				System.err.println(g_user);
				System.err.println(g_comment.getHtml_url());
				System.err.println(savedUsersIds);

			}
			comment.setUser(user);
			for (final Paragraph paragraph : comment.getParagraphs()) {
				session.save(paragraph);
			}
			session.save(comment);
			thread.addComent(comment);
		}
		session.save(thread);
		discussion.addThread(thread);
		session.update(discussion);

	}

	// TODO refactor to globals
	private static void saveComment(final Session session, final Discussion discussion,
			final Map<String, User> savedUsersIds, final GSONComment g_comment) {
		final Comment comment = buildComment(g_comment);
		final GSONUser g_user = g_comment.getUser();
		User user;
		if (!savedUsersIds.containsKey(g_user.getId())) {
			user = buildUser(g_user);
			savedUsersIds.put(g_user.getId(), user);
			session.save(user);
			discussion.addUser(user);
		} else {
			user = savedUsersIds.get(g_user.getId());
		}
		comment.setUser(user);
		for (final Paragraph paragraph : comment.getParagraphs()) {
			session.save(paragraph);
		}
		session.save(comment);
		discussion.addComment(comment);
	}

	/**
	 * The clustering of threads consists in rebuilding the threads from the ordered
	 * list, based on the Original_position metadata
	 *
	 * @param threads Unclustered list of threads
	 * @return A maIdp where threads have been clustered
	 */
	private static Map<String, List<GSONComment>> clusterThreads(final List<GSONComment> threads) {
		logger.trace(String.format("Clustering %d threads", threads.size()));
		final LinkedHashMap<String, List<GSONComment>> clusters = new LinkedHashMap<>();
		for (final GSONComment thread : threads) {
			final String identifier = thread.getOriginal_position() + thread.getOriginal_commit_id()
					+ thread.getDiff_hunk();
			if (!clusters.containsKey(identifier)) {
				clusters.put(identifier, new ArrayList<>());
			}
			clusters.get(identifier).add(thread);
		}
		logger.trace(String.format("Clustering completed, found %d clusters", clusters.size()));
		return clusters;
	}

	private static Comment buildComment(final GSONComment g_comment) {
		final Comment comment = CommentBuilder.build(g_comment.getId(), g_comment.getCreated_at(),
				g_comment.getUpdated_at(), g_comment.getAuthor_association(), g_comment.getOriginal_position(),
				g_comment.getOriginal_commit_id(), g_comment.getDiff_hunk(), g_comment.getBody());
		final List<String> paragraphs = parseParagraphs(g_comment.getBody());
		for (int i = 0; i < paragraphs.size(); i++) {
			final String paragraph = paragraphs.get(i);
			comment.getParagraphs().add(ParagraphBuilder.build(paragraph, i));
		}
		return comment;
	}

	private static List<String> parseParagraphs(final String body) {
		final List<String> paragraphs = new ArrayList<>();
		if (body == null || body.isEmpty()) {
			return paragraphs;
		}
		final String cleanedBody = Patterns.commentBlock.replace(Patterns.codeBlock.replace(body));
		for (String paragraph : cleanedBody.split(PARAGRAPH_SPLIT_REGEX)) {
			paragraph = paragraph.trim();
			if (!paragraph.equals("")) {
				paragraphs.add(paragraph);
			}
		}
		return paragraphs;
	}

	private static User buildUser(final GSONUser g_user) {
		return UserBuilder.build(g_user.getLogin(), g_user.getId());
	}
}
