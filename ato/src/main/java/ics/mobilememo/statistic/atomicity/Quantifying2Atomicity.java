package ics.mobilememo.statistic.atomicity;

import ics.mobilememo.benchmark.workload.RequestRecord;
import ics.mobilememo.execution.Execution;
import ics.mobilememo.execution.ExecutionLogHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import android.R.integer;

/**
 * Given an execution satisfying 2-atomicity, the
 * statistics for 2-atomicity including:
 * (1) number of occurrences of "concurrency pattern" (CP)
 * (2) number of occurrences of "old-new inversion" (ONI)
 */
public class Quantifying2Atomicity
{
	// the execution to be quantified against 2-atomicity
	private Execution execution = null;
	
	// name of file to store the "concurrency pattern"s
	private final String CP_FILE_NAME = "cp.txt";
	// file to store the "concurrency pattern"s
	private File cp_file = null;
	
	private int cp_count = 0;
	private int oni_count = 0;
	
	/**
	 * Constructor of {@link Quantifying2Atomicity}
	 * 
	 * @param rr_list 
	 * 	a list of {@link RequestRecord}s constituting an execution
	 */
	public Quantifying2Atomicity(Execution execution)
	{
		this.execution = execution;
	}
	
	/**
	 * Constructor of {@link Quantifying2Atomicity}
	 * 
	 * @param execution_file
	 * 	file containing an execution
	 */
	public Quantifying2Atomicity(String execution_file_path)
	{
		this.execution = new Execution(new ExecutionLogHandler(execution_file_path).loadRequestRecords());
		
		// create file to store the "concurrency pattern"s
		File execution_file = new File(execution_file_path);
		this.cp_file = new File(execution_file.getParentFile(), this.CP_FILE_NAME);
	}
	
	/**
	 * Quantifying 2-atomicity in terms of numbers of "concurrency patterns"
	 * and "old-new inversions"
	 */
	public void quantify()
	{
		BufferedWriter bw = null;
		if (this.cp_file != null)
		{
			try
			{
				bw = new BufferedWriter(new FileWriter(this.cp_file));
			} catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
		
		for (RequestRecord read_request_record : this.execution.getReadRequestRecordList())
		{
			// scan all the write operations in their total order
			Iterator<RequestRecord> write_request_record_iter = this.execution.getWriteRequestRecordList().iterator();
			// skip the first one
			if (write_request_record_iter.hasNext())
				write_request_record_iter.next();
			
			while (write_request_record_iter.hasNext())
			{
				RequestRecord write_request_record = (RequestRecord) write_request_record_iter.next();
			
				/**
				 * Condition (1) for "concurrency pattern": the read operation starts during the interval
				 * of a write operation.
				 * 
				 * Find the concurrent write operation
				 */
				if (read_request_record.startWithin(write_request_record))
				{
					for (RequestRecord pre_read_request_record : this.execution.getReadRequestRecordList())
					{
						/**
						 * Condition (2): another read operation
						 */
						if (pre_read_request_record.finishWithin(write_request_record.getStartTime(), read_request_record.getStartTime()))
						{
							// catch an occurrence of concurrency pattern
							this.cp_count++;
							
							// store it in file
							if (this.cp_file != null)
							{
								try
								{
									bw.write("Number of concurrency patterns: " + this.cp_count + "\n" + 
											read_request_record.toString() + "\n" + write_request_record.toString() + "\n" 
											+ pre_read_request_record.toString() + "\n\n");
								} catch (IOException ioe)
								{
									ioe.printStackTrace();
								}
							}
							
							// Is this an old-new inversion (an violation of atomicity)
							if (this.isONI(read_request_record, write_request_record, pre_read_request_record))
								this.oni_count++;
						}
					}
					
					// there is only one concurrent write operation
					break;
				}
				
			}
		}
		
		try
		{
			bw.close();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	private boolean isONI(RequestRecord cur_read, RequestRecord write, RequestRecord pre_read)
	{
		int cur_read_version = cur_read.getVersion().getSeqno();
		int write_version = write.getVersion().getSeqno();
		int pre_read_version = pre_read.getVersion().getSeqno();
		
		return (pre_read_version == write_version) &&
				(pre_read_version == cur_read_version + 1);
	}
	
	/**
	 * @return {@link #cp_count}
	 * 	number of "concurrency patterns"
	 */
	public int getCPCount()
	{
		return this.cp_count;
	}
	
	/**
	 * @return {@link #oni_count}
	 * 	number of "old-new inversions"
	 */
	public int getONICount()
	{
		return this.oni_count;
	}
	
	/**
	 * Test
	 */
	public static void main(String[] args)
	{
		Quantifying2Atomicity quantifer = new Quantifying2Atomicity("C:\\Users\\ics-ant\\Desktop\\executions\\allinonetest\\execution.txt");
		
		System.out.println("Quantifying 2-atomicity");
		quantifer.quantify();
		System.out.println("The number of \"concurrency patterns\" is: " + quantifer.getCPCount());
		System.out.println("The number of \"old new inversions\" is: " + quantifer.getONICount());
	}
}