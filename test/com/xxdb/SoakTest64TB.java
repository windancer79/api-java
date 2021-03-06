package com.xxdb;

import com.xxdb.data.BasicDateVector;
import com.xxdb.data.BasicTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SoakTest64TB {
	
	public void test(Path serverFile, Path symbolFile, Path dateFile, int sessions, int breath){
		ArrayList<DBConnection> connectionList = parseConnectionList(serverFile, sessions);
		List<String> symbolList = parseColumn(symbolFile);
		List<String> dateList = parseColumn(dateFile);
		
		if(connectionList.size() == 0){
			System.out.println("No servers collected, please put a list of servers in a text file with format: x.x.x.x:xxxx");
			return;
		}
		if(symbolList.size() == 0){
			System.out.println("No symbols collected, please put a list of symbols in a text file with format: symbol, count");
			return;
		}
		
		JobGenerator jobGenerator = new JobGenerator(symbolList, dateList);
		for(int i=0; i<sessions; ++i){
			new Thread(new Executor(jobGenerator, connectionList.get(i), breath)).start();
		}
	}
	
	private ArrayList<DBConnection> parseConnectionList(Path filePath, int numOfSession){
		ArrayList<DBConnection> connectionList = new ArrayList<DBConnection>();
		try {
			List<String> lines = Files.readAllLines(filePath);
			ArrayList<String> mylist = new ArrayList<String>();
			for(String line: lines){
				line = line.trim();
				if(!line.equals("")){
					mylist.add(line);
				}
			}
			if(mylist.size() < numOfSession){
				for(int i=0;i<numOfSession; i++){
					String line = mylist.get(i % mylist.size());
					String host = line.split(":")[0];
					int port = Integer.parseInt(line.split(":")[1]);
					DBConnection conn = new DBConnection();
					conn.connect(host, port);
					System.out.println("connected to " + line);
					connectionList.add(conn);
				}
			}
			else{
				while (connectionList.size() < numOfSession) {
					int idx = ThreadLocalRandom.current().nextInt(0, mylist.size());
					String line = mylist.get(idx % mylist.size());
					String host = line.split(":")[0];
					int port = Integer.parseInt(line.split(":")[1]);
					DBConnection conn = new DBConnection();
					conn.connect(host, port);
					System.out.println("connected to " + line);
					connectionList.add(conn);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return connectionList;
	}

	private ArrayList<String> parseColumn(Path filePath) {
		final ArrayList<String> list = new ArrayList<>();
		final NavigableMap<Long, String> map = new TreeMap<Long, String>();
		try {
			List<String> lines = Files.readAllLines(filePath);
			for (int i = 0; i < lines.size(); i++) {
				list.add(lines.get(i).split(",")[0]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}
	
    private class JobDesc {
    	private String category;
    	private ArrayList<String> symbols;
    	private String date;
    	
    	public JobDesc(String category, ArrayList<String> symbols, String date){
    		this.category = category;
    		this.symbols = symbols;
    		this.date = date;
    	}
    	
    	public String getCategory(){
    		return category;
    	}
    	
    	public ArrayList<String> getSymbols(){
    		return symbols;
    	}
    	
    	public String getDate(){
    		return date;
    	}
    }
    
    private class JobGenerator {
    	private List<String> symbolList;
		private List<String> dateList;
		private ArrayList <String> queryTypeList;
		private ArrayList<Integer> szList;
		private int totalSymbolCount;
		private int totalDateCount;
		
    	public JobGenerator(List<String> symbolList, List<String> dateList){
    		this.symbolList = symbolList;
    		this.dateList = dateList;
    		totalSymbolCount = symbolList.size();
    		totalDateCount = dateList.size();
    		
    		queryTypeList = new ArrayList<String>();
    	    queryTypeList.add("download");
    	    queryTypeList.add("download");
    	    queryTypeList.add("download");
    	    queryTypeList.add("download");
    	    queryTypeList.add("download");
    	    queryTypeList.add("download");
    	    queryTypeList.add("download");
    	    queryTypeList.add("download");
    	    queryTypeList.add("groupbyMinute");
    	    queryTypeList.add("groupbyDate");
    	    
    	    szList = new ArrayList<Integer>();
	  		szList.add(1);
	  		szList.add(1);
	  		szList.add(1);
	  		szList.add(1);
	  		szList.add(1);
	  		szList.add(1);
	  		szList.add(1);
	  		szList.add(2);
	  		szList.add(2);
	  		szList.add(3);
			szList.add(4);
    	}
    	
    	public synchronized JobDesc next(){
    		int symbolIdx = ThreadLocalRandom.current().nextInt(0, totalSymbolCount);
	    	String symbol = symbolList.get(symbolIdx);
	    	int dateIdx = ThreadLocalRandom.current().nextInt(0, totalDateCount);
	    	String queryType = queryTypeList.get(ThreadLocalRandom.current().nextInt(0, queryTypeList.size()));
	    	ArrayList<String> mysymbols = new ArrayList<String>();
	    	if(queryType.equalsIgnoreCase("groupbyDate")){
	    		int idx = ThreadLocalRandom.current().nextInt(0, szList.size());
	    		while(mysymbols.size()<szList.get(idx)){
	    			symbolIdx = ThreadLocalRandom.current().nextInt(0, totalSymbolCount);
	    			String tmpSymbol = symbolList.get(symbolIdx);
	    			if(mysymbols.contains(tmpSymbol))
	    				continue;
	    			mysymbols.add(tmpSymbol);
	    		}
	    	}
	    	else{
	    		mysymbols.add(symbol);  
	    	}
	    	String dateStr = dateList.get(dateIdx);
	    	return new JobDesc(queryType, mysymbols, dateStr);
    	}
    }
	
	private class Executor implements Runnable {
		private JobGenerator generator;
		private DBConnection conn;
		private int breath;
		private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		
		public Executor(JobGenerator generator, DBConnection conn, int breath){
			this.generator = generator;
			this.conn = conn;
			this.breath = breath;
		}
		
		private void print(String message){
			System.out.println(Thread.currentThread().getId() + " " + dateFormat.format(new Date()) + " " + message);
		}
		
		@Override
		public void run(){
			String connStr = conn.getHostName() + ":" + conn.getPort();
			try {
				conn.run("TAQ = database(\"dfs://OptionDFS50TB\").loadTable(\"TAQ\")");
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			while(true){
				if(breath > 0){
					try {
						Thread.sleep(breath);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
						print(connStr + " thread exit");
						break;
					}
				}
				
				JobDesc job = generator.next();
				String queryType = job.getCategory();
				String date = job.getDate();
				ArrayList<String> symbolList = job.getSymbols();

				long sum = 0;
				long total = 0;
				String sql;
				BasicTable table = null;
				
				try {
					long start = 0;
					int step = 100000; 
					if(queryType.equalsIgnoreCase("groupbyMinute")){
						sql = "select sum(bidSize), avg(bidPrice) as avgBidPrice,  avg(underlyerLastBidPrice) as avgUnderlyerPrice from TAQ where symbol=\"" +symbolList.get(0) + "\", date=" + date + " group by minute(time)";
						table = (BasicTable)conn.run(sql);
						print(connStr + "   " + sql);
					}
					else if(queryType.equalsIgnoreCase("groupbyDate")){
						BasicDateVector dateVec = (BasicDateVector) conn.run( "(" + date + "-2)..(" +date + "+2)");
						if(symbolList.size()>1)
							sql = "select  sum(bidSize), avg(bidPrice) as avgBidPrice,  avg(underlyerLastBidPrice) as avgUnderlyerPrice from TAQ where symbol in [\"" + String.join("\",\"", symbolList) + "\"], date>="+dateVec.get(0).toString() + " and date<=" + dateVec.get(4).toString() +" group by symbol, date" ;
						else
							sql = "select  sum(bidSize), avg(bidPrice) as avgBidPrice,  avg(underlyerLastBidPrice) as avgUnderlyerPrice from TAQ where symbol =\"" + symbolList.get(0) + "\", date>= " +dateVec.get(0).toString() + " and date<=" + dateVec.get(4).toString() +" group by date" ;
						print(connStr + "   " + sql);
						table = (BasicTable)conn.run(sql);
						
					}
					else{
						String symbol = symbolList.get(0);
						sql = "select  count(*) from TAQ where symbol=\"" + symbol + "\", date=" + date;
						table = (BasicTable)conn.run(sql);
						total = table.getColumn(0).get(0).getNumber().longValue();
						print(connStr + "   " + sql);
						while(sum < total){
							if(total<=step){
								sql = "select * from TAQ where symbol=\"" + symbol + "\", date=" + date;
								print(connStr + "   " + sql);
								table = (BasicTable)conn.run(sql);
							}
							else{
								sql = "select top " + start + ":" + (start+step) + " * from TAQ where symbol=\"" + symbol + "\", date=" + date;
								print(connStr + "   " + sql);
								table = (BasicTable)conn.run(sql);
							}
							
							start += step;
							sum += table.rows();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					print(connStr + ": " + String.join("\",\"", symbolList)  + ": " + date + " Failed ");
					continue;
				}

				if(queryType.equalsIgnoreCase("download")){
					print(connStr + ": " + String.join("\",\"", symbolList)  + ": " + date + ": " + String.valueOf(total) + "==" + String.valueOf(sum) + " is " + String.valueOf(total==sum));
				}
				else{
					print(connStr + ": " + String.join("\",\"", symbolList)  + ": " + date + ": " + queryType + ": " + table.rows());
				}
			}
		}
	}
	
	public static void main(String[] args){
		int sessions = args.length == 0 ? 32 : Integer.parseInt(args[1]);
		System.out.println("sessions " + sessions);
		SoakTest64TB dc = new SoakTest64TB();
		dc.test(Paths.get("soak_server.txt"), Paths.get("symbols.txt"), Paths.get("dates.txt"), sessions, 100);
	}
}
