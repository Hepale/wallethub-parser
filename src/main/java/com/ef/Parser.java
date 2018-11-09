package com.ef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import com.ef.init.HibernateUtil;

/**
 * Parsing file to do the final test for WalletHub
 * 
 * @author Alejandro
 *
 */
public class Parser {
	// log variable of java.util.logging
	private static final Logger log = Logger.getLogger(Parser.class.getName());

	// Variable to store parameters
	Map<String, String> arguments = new HashMap<String, String>();
	Set<String[]> file = new HashSet<String[]>();

	/**
	 *  Starting point
	 * @param args
	 * @throws ParamsException, IOException, ParseException
	 */
	public static void main(String[] args) throws ParamsException, IOException, ParseException  {
		Parser mine = new Parser();
		mine.parameters(args);
		mine.getLogFile();
		mine.storeInDB();
		mine.ipFilter();
	}

	/**
	 *  To read parameters
	 * @param args
	 * @return 
	 * @throws ParamsException 
	 */
	private void parameters(String[] args) throws ParamsException {
		String[] temp;
		for (String arg : args) {
			if (!arg.startsWith("--"))
				continue;
			temp = arg.split("=");
			arguments.put(temp[0].substring(2), temp[1]);
		}
		
		validateParams();
	}
	
	/**
	 *  To validate params
	 * @return boolean
	 * @throws ParamsException 
	 */
	private void validateParams() throws ParamsException {
		try {
			if (!(arguments.size() >= 3))
				throw new ParamsException("Parameter's size is not valid");
			if (!arguments.containsKey("startDate"))
				throw new ParamsException("Parameter 'startDate' is not valid");
			if (!arguments.containsKey("duration"))
				throw new ParamsException("Parameter 'duration' is not valid");
			if (!arguments.containsKey("threshold"))
				throw new ParamsException("Parameter 'threshold' is not valid");
			if (arguments.size() == 4 && !arguments.containsKey("accesslog"))
				throw new ParamsException("Parameter 'accesslog' is not valid");
		} catch (ParamsException e) {
			log.log(Level.SEVERE, e.getMessage());
			throw e;
		}

	}

	/**
	 *  To read Log File
	 * @throws IOException 
	 */
	private void getLogFile() throws IOException {
		System.out.println("Getting data from File: access.log");
		log.log(Level.INFO, "Getting data from File: access.log");
		
		String fileName = "src/main/access.log";
		if (arguments.containsKey("accesslog"))
			fileName = arguments.get("accesslog");
		
		// reading all lines from file 
		try {
			if (arguments.containsKey("accesslog")){
				fileName = arguments.get("accesslog");
				if (!new File(fileName).exists()) {
					log.log(Level.SEVERE, "File: " + fileName + " could not be read.");
					return;
				}
			}
			Files.lines(Paths.get(fileName)).forEach(
					line -> {
						String[] arr = line.split("\\|");
						file.add(arr);
					});
			
		} catch (IOException e) {
			log.log(Level.SEVERE, "File access.log could not be read", e);
			throw e;
		}
		
		System.out.println("Total Lines: " + file.size());
		log.log(Level.INFO, "Total Lines: " + file.size());
	}
		
	/**
	 *  Bulk massive storing in DB
	 */
	private void storeInDB(){
		System.out.println("Storing in DB...");
		log.log(Level.INFO, "Storing in DB...");
		
		//Manage of StringBuilder to improve performance
        StringBuilder queryInsertion = new StringBuilder();
        int count = 1;
        
        //Bulk insertion
        for (String[] logFromFile : file) {
        	if (count % 8000 == 0){
        		buildInsertion (queryInsertion);
        		queryInsertion = new StringBuilder();
        	}
        	
        	queryInsertion.append("(");
        	queryInsertion.append("'"+logFromFile[0]+"',");
        	queryInsertion.append("'"+logFromFile[1]+"',");
        	queryInsertion.append("'"+logFromFile[2]+"',");
        	queryInsertion.append("'"+logFromFile[3]+"',");
        	queryInsertion.append("'"+logFromFile[4]+"'),");
        	count ++;
		}
        
        // To insert the remaining data
        buildInsertion(queryInsertion);
        
        log.log(Level.INFO, "Stored in DB");
	}
	
	/**
	 * Build query of Massive Insertion
	 * @param queryInsertion
	 */
	private void buildInsertion(StringBuilder queryInsertion) {

		//Building Query
		String batchInsertion = "INSERT INTO log (datetime, ip, method, status_resp, client) values";
		queryInsertion.deleteCharAt(queryInsertion.lastIndexOf(","));
		queryInsertion.append(";");
		queryInsertion.insert(0, batchInsertion);
		
		
		//Customized Query
		executeHibernateQuery(queryInsertion, false);
	}
	
	/**
	 * Displays the ip according time and threshold
	 * @throws ParseException
	 */
	private void ipFilter() throws ParseException {
		String duration = arguments.get("duration");
		String startTime = arguments.get("startDate");
		
		//Setting date
		Calendar cal = Calendar.getInstance();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd'.'HH:mm:ss");
		cal.setTime(format.parse(startTime));
		switch (duration) {
		case "daily":
			cal.add(Calendar.DAY_OF_MONTH, 1);
			break;
		default:
			cal.add(Calendar.HOUR, 1);
		}
		cal.add(Calendar.MINUTE, -1);
		
		StringBuilder strQuery = new StringBuilder("select ip, count(ip) from log "+ 
			"where datetime between '"+startTime+"' and '"+format.format(cal.getTime())+"' "+ 
			"group by ip having count(ip)>= "+arguments.get("threshold"));
		
		List<Object> sqlResponse = executeHibernateQuery(strQuery, true);
		Object[] columnQuery;
        System.out.println("Forbidden IPs, access bigger than: " + arguments.get("threshold"));
        for (Object row : sqlResponse) {
        	columnQuery = (Object[]) row;
			System.out.println("\t - "+columnQuery[0] +"\t total accessed:"+ columnQuery[1]);
		}
		
	}
	
	/**
	 * Execute query provided
	 * @param strQuery
	 */
	@SuppressWarnings("unchecked")
	private List<Object> executeHibernateQuery(StringBuilder strQuery, boolean retrieve) {
  		//Variables to be start Hibernate connection:
		//SessionFactory, Session, Transaction
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();

        List<Object> results = null;
        
		//Execute customized Query
		SQLQuery query = session.createSQLQuery(strQuery.toString());
		
		if (retrieve){
			results = query.list();
		} else {
			query.executeUpdate();
		}
			
		
		//Closing Transaction and Session
		tx.commit();
        session.close();
        
        return results;
	}

	
}
