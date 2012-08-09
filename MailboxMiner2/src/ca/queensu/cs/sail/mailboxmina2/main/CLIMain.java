package ca.queensu.cs.sail.mailboxmina2.main;

import com.sampullara.cli.Argument;

public class CLIMain {
		
		@Argument(value = "connection", description = "JDBC Database Connection URL. (//hostname:port/dbname)", required = true)
	    public String connection;
		
		@Argument(value="username", description="username for database connection.", required=true)
		public String username;
		
		@Argument(value="password", description="password for database connection.", required=true)
		public String password;
		
		@Argument(value="dbname", description="Name of the database to use.", required=false)
		public String dbname;
		
		@Argument(value="drop", description="Drop any existing database (default=true)", required=false)
		public boolean drop;

		@Argument(value="path", description="Path to a folder containing mbox files, or path to a single file.", required=false)
		public String path = "./";
		
		@Argument(value="module", description="Which module of MailboxMiner2 to use.\n" +
				"\t\tAvailable Modules:\n" +
				"\t\t\tinsert		insert messages inside the mbox file into the database.\n" +
				"\t\t\tthreads		re-create messages threads using the information in the database.\n" +
				"\t\t\tclean		clean up message bodies (remove quotes and signatures).\n" +
				"\t\t\tcreate		create database form the template database.\n" +
				"\t\t\tpersons		merge multiple personalities\n", required=true)
		public String module;
		
		@Argument(value="debug", description="Enabled Debug mode (default=false)", required=false)
		public boolean debug;
		
		@Argument(value="logfile", description="Optional Logfile to write instead of System.out/err streams", required=false)
		public String logfile;
		
		@Argument(value="verbosity", description="How verbose shall the debug/log/error statements be. Default=3", required=false)
		public String loglevel = "3";
		
		@Argument(value="testonly", description ="Whether to execute database I/O or just do a dry run of the algorithms", required=false)
		public boolean testonly = false;
}
