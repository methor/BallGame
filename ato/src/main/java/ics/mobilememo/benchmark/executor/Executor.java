package ics.mobilememo.benchmark.executor;

import consistencyinfrastructure.data.kvs.Key;
import ics.mobilememo.benchmark.workload.PoissonWorkloadGenerator;
import ics.mobilememo.benchmark.workload.Request;
import ics.mobilememo.benchmark.workload.RequestRecord;
import ics.mobilememo.service.timingservice.TimingService;
import ics.mobilememo.sharedmemory.atomicity.AbstractAtomicityRegisterClient;
import ics.mobilememo.sharedmemory.atomicity.AtomicityRegisterClientFactory;
import ics.mobilememo.sharedmemory.data.kvs.VersionValue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import log4android.ConfigureLog4J;

import org.apache.log4j.Logger;

import android.util.Log;

/**
 * @description executor responsible for issuing the requests from workload benchmarks
 * 
 * @author hengxin
 * @date 2014-04-24
 */
public class Executor implements Runnable
{
	private static final String TAG = Executor.class.getName();
	
	/**
	 * use "android-logging-log4j"
	 * <a href>https://code.google.com/p/android-logging-log4j/</a>
	 */
	private final Logger log4android = Logger.getLogger(Executor.class);
	
	// number of requests to execute
	private int request_number = -1;
	private BlockingQueue<Request> request_queue = new LinkedBlockingDeque<Request>();
	
	/**
	 * the appropriate client has been set when logging in. @see LoginActivity
	 */
	AbstractAtomicityRegisterClient client = AtomicityRegisterClientFactory.INSTANCE.getAtomicityRegisterClient();
	
	/**
	 * constructor of {@link Executor}
	 * 
	 * using the producer-consumer synchronization mechanism
	 * @param request_queue {@link #request_queue}: queue of {@link RequestRecord}s 
	 *  between producer {@link PoissonWorkloadGenerator} and consumer {@link Executor}
	 * @param request_number {@link #request_number}: number of requests to execute
	 */
	public Executor(BlockingQueue<Request> request_queue, int request_number)
	{
		ConfigureLog4J.INSTANCE.configure();
		
		this.request_queue = request_queue;
		this.request_number = request_number;
	}
	
	/**
	 * issue the request and record statistical information
	 * @param request request to issue
	 */
	private void issue(Request request)
	{
		int type = request.getType();
		Key key = request.getKey();
		String val = request.getValue();
		VersionValue vvalue = null;
		
//		long invocation_time = System.currentTimeMillis();
		
		long invocation_time = TimingService.INSTANCE.pollingTime();
		if (type == Request.WRITE_TYPE)	// it is W[0]
			vvalue = client.put(key, val);
		else // it is R[1]
			vvalue = client.get(key);
		long response_time = TimingService.INSTANCE.pollingTime();
		
//		long response_time = System.currentTimeMillis();

		// the delay = response_time - invocation_time is calculated and recorded
		RequestRecord rr = new RequestRecord(type, invocation_time, response_time, key, vvalue);
		log4android.debug(rr.toString());
	}
	
	/**
	 * take requests from workload benchmarks one by one and issue them
	 */
	@Override
	public void run()
	{
		int index = 0;
		while(index < this.request_number)
		{
			try
			{
				this.issue(request_queue.take());
			} catch (InterruptedException ie)
			{
				ie.printStackTrace();
			}
			
			Log.d(TAG, "The number of request: " + index);
			index++;
		}
		
		/**
		 * Shut down the logger and let all the buffered logs get flushed
		 * See http://stackoverflow.com/a/3078377/1833118
		 * 
		 * @author hengxin
		 * @date Jul 15, 2014
		 */
		ConfigureLog4J.INSTANCE.shutdown();
	}
}
