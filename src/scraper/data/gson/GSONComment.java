package scraper.data.gson;

import java.util.Date;

public class GSONComment {
	private String url;
	private String html_url;
	private String id;
	private GSONUser user;
	private String body;
	private Date created_at;
	private Date updated_at;
	private String author_association;
	private String original_position;
	private String original_commit_id;

	// Threads
	private String diff_hunk;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getHtml_url() {
		return html_url;
	}

	public void setHtml_url(String html_url) {
		this.html_url = html_url;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Date getCreated_at() {
		return created_at;
	}

	public void setCreated_at(Date created_at) {
		this.created_at = created_at;
	}

	public Date getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(Date updated_at) {
		this.updated_at = updated_at;
	}

	public GSONUser getUser() {
		return user;
	}

	public void setUser(GSONUser user) {
		this.user = user;
	}

	public String getAuthor_association() {
		return author_association;
	}

	public void setAuthor_association(String author_association) {
		this.author_association = author_association;
	}

	public String getOriginal_position() {
		return original_position;
	}

	public void setOriginal_position(String original_position) {
		this.original_position = original_position;
	}

	public String getOriginal_commit_id() {
		return original_commit_id;
	}

	public void setOriginal_commit_id(String original_commit_id) {
		this.original_commit_id = original_commit_id;
	}

	public String getDiff_hunk() {
		return diff_hunk;
	}

	public void setDiff_hunk(String diff_hunk) {
		this.diff_hunk = diff_hunk;
	}
}
