package ca.queensu.cs.sail.mailboxmina2.tools;

import com.sampullara.cli.Argument;

public class CLICreateDatabase {
	
	@Argument(value = "connection", description = "JDBC Database Connection URL.", required = true)
    public String connection;
	
	@Argument(value="username", description="username for database connection.", required=true)
	public String username;
	
	@Argument(value="password", description="password for database connection.", required=true)
	public String password;
	
	@Argument(value="dbname", description="Name of the database to create.", required=true)
	public String dbname;
	
	@Argument(value="drop", description="Drop any existing database (default=true)", required=false)
	public boolean drop;
	
	@Argument(value="module", description="Which module of MailboxMiner2 to use.\n" +
			"\t\tAvailable Modules:\n" +
			"\t\t\tinsert		insert messages inside the mbox file into the database.\n" +
			"\t\t\tthreads		re-create messages threads using the information in the database.\n" +
			"\t\t\tclean		clean up message bodies (remove quotes and signatures).\n" +
			"\t\t\tcreate		create database from the template database.\n", required=true)
	public String module;
	
}
