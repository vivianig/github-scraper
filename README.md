# Github Scraper

Simple tool to scrape pull request comments from Github. Requires a SQL server to store the scraped data

# How to use
* Create a file with the list of pull requests to be scraped in the same format as `res/default_list`
* Copy `res/default_config` to `res/config` and set the Github authentication token and the path to thhe file containing the pull requests to be scraped
* Copy `res/default_hibernate.cfg.xml` to `res/hibernate.cfg.xml` and set the information to connect  to the SQL server
* Compile and run
