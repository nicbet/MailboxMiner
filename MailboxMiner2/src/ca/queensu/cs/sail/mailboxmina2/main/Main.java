package ca.queensu.cs.sail.mailboxmina2.main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import ca.queensu.cs.sail.mailboxmina2.common.Logger;
import ca.queensu.cs.sail.mailboxmina2.main.modules.IModule;
import ca.queensu.cs.sail.mailboxmina2.main.modules.InsertModule;
import ca.queensu.cs.sail.mailboxmina2.main.modules.PersonalitiesModule;
import ca.queensu.cs.sail.mailboxmina2.main.modules.ThreadsModule;
import ca.queensu.cs.sail.mailboxmina2.tools.CreateDatabase;

import com.sampullara.cli.Args;

public class Main {
	private static Logger logger;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Read command line arguments as specified in CLICreateDatabase Class
		CLIMain cli = new CLIMain();
        try {
        	Args.parse(cli, args);
        } catch (Exception e) {
        	System.err.println("Error while parsing command line arguments: " + e.getMessage());
        	Args.usage(cli);
        	System.exit(1);
        }
        
        if (cli.debug)
        	logger = new Logger(true);
        else
        	logger = new Logger(false);
        
        
        logger.setMaxDebugLevel(Integer.parseInt(cli.loglevel));
        
        FileOutputStream logfileStream=null;
        if (cli.logfile != null) {
        	try {
        		logfileStream = new FileOutputStream(cli.logfile, false);	// Default: append to logfile ;-)
        		PrintStream logPrintStream = new PrintStream(logfileStream);
        		
        		logger.setDebugStream(logPrintStream);
        		logger.setErrorStream(logPrintStream);
        		logger.setLogStream(logPrintStream);
        		
        	} catch (IOException e) {
        		logger.error("Could not use logfile " + cli.logfile + " for logging!", e);
        	}
        }
        
        if (cli.module.equalsIgnoreCase("insert")) {
        	System.out.println("Running insertion module...");
        	// run the insertion module
        	
        	
        	Properties insertProps = new Properties();
        	insertProps.setProperty("db_url", cli.connection);
        	insertProps.setProperty("username", cli.username);
        	insertProps.setProperty("password", cli.password);
        	insertProps.setProperty("path", cli.path);
        	
        	IModule insertModule = new InsertModule(cli.testonly);
        	boolean success = insertModule.run(insertProps, logger);
        	
        	if (success)
        		System.out.println("InsertModule finished successfully!");
        	else
        		System.out.println("There were errors while running the insert module!");
        	
        } else if (cli.module.equalsIgnoreCase("threads")) {
        	// run the threads module
        	System.out.println("Running threads module...");      	
        	
        	Properties threadsProps = new Properties();
        	threadsProps.setProperty("db_url", cli.connection);
        	threadsProps.setProperty("username", cli.username);
        	threadsProps.setProperty("password", cli.password);
        	threadsProps.setProperty("path", cli.path);
        	
        	IModule threadsModule = new ThreadsModule();
        	boolean success = threadsModule.run(threadsProps, logger);
        	
        	if (success)
        		System.out.println("ThreadsModule finished successfully!");
        	else
        		System.out.println("There were errors while running the threads module!");
        	
        } else if (cli.module.equalsIgnoreCase("clean")) {
        	// run the cleanup module
        	
        } else if (cli.module.equalsIgnoreCase("create")) {
        	CreateDatabase.main(args);
        } else if (cli.module.equalsIgnoreCase("persons")) {
        	IModule personsModule = new PersonalitiesModule();
        	
        	Properties personsProps = new Properties();
        	personsProps.setProperty("db_url", cli.connection);
        	personsProps.setProperty("username", cli.username);
        	personsProps.setProperty("password", cli.password);
        	personsProps.setProperty("path", cli.path);
        	
        	boolean success = personsModule.run(personsProps, logger);
        	
        	if (success)
        		System.out.println("PersonalitiesModule finished successfully!");
        	else
        		System.out.println("There were errors while running the personalities module!");
        	
        }else {
        	System.out.println("Invalid module specified!");
        	System.out.println("Available Modules:\n" +
        						"insert		insert messages inside the mbox file into the database.\n" +
        						"threads	re-create messages threads using the information in the database.\n" +
								"clean		clean up message bodies (remove quotes and signatures)." +
								"persons	merge multiple personalitites in the database");
        }
        
        if (cli.logfile != null && logfileStream != null) {
        	try {
				logfileStream.close();
			} catch (IOException e) {
				logger.error("Could not close logfile's stream " + cli.logfile, e);
			}
        }
        
	}
	
	public static Logger getLogger() {
		if (logger == null) 
			logger = new Logger(true);
		
		return logger;
	}

}
