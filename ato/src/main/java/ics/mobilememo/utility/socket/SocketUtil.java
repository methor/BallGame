package ics.mobilememo.utility.socket;

import ics.mobilememo.service.timingservice.message.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

/**
 * Utility class for sending and waiting for messages via socket
 * @author hengxin
 * @date Jul 15, 2014
 */
public enum SocketUtil
{
	INSTANCE;
	
	private static final String TAG = SocketUtil.class.getName();
	
	private static final ExecutorService exec = Executors.newCachedThreadPool();
	
	public void sendMsg(final Message msg, final Socket host_socket)
	{
		ObjectOutputStream oos;
		try
		{
			oos = new ObjectOutputStream(host_socket.getOutputStream());
			oos.writeObject(msg);
			oos.flush();
		} catch (SocketTimeoutException stoe)
		{
			stoe.printStackTrace();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	public void sendMsgInNewThread(final Message msg, final Socket host_socket)
	{
		exec.execute(new SendMsgTask(msg, host_socket));
	}
	
	public Message receiveMsg(final Socket host_socket)
	{
		Message msg = null;
		
		try
		{
			final ObjectInputStream ois = new ObjectInputStream(host_socket.getInputStream());
			msg = (Message) ois.readObject();
			ois.close();
		} catch (StreamCorruptedException sce)
		{
			sce.printStackTrace();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		} catch (ClassNotFoundException cnfe)
		{
			cnfe.printStackTrace();
		}
		
		return msg;
	}
	
	public Message receiveMsgInNewThread(final Socket host_socket)
	{
		Future<Message> future = exec.submit(new ReceiveMsgTask(host_socket));
		try
		{
			return future.get();
		} catch (InterruptedException ie)
		{
			ie.printStackTrace();
		} catch (ExecutionException ee)
		{
			ee.printStackTrace();
		} catch (CancellationException ce)
		{
			ce.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Send message via specified socket
	 * @author hengxin
	 * @date Jul 15, 2014
	 */
	final class SendMsgTask implements Runnable
	{
		// message to send
		private final Message msg;
		// send message via this socket
		private final Socket host_socket;
		
		/**
		 * Constructor of {@link SendMsgTask}
		 * @param msg {@link Message} to send
		 * @param host_socket send message via this socket
		 */
		public SendMsgTask(final Message msg, final Socket host_socket)
		{
			this.msg = msg;
			this.host_socket = host_socket;
		}
		
		@Override
		public void run()
		{
			ObjectOutputStream oos;
			try
			{
				oos = new ObjectOutputStream(host_socket.getOutputStream());
				oos.writeObject(msg);
				oos.flush();
			} catch (SocketTimeoutException stoe)
			{
				stoe.printStackTrace();
			} catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}
	
	final class ReceiveMsgTask implements Callable<Message>
	{
		private final Socket host_socket;
		
		public ReceiveMsgTask(final Socket host_socket)
		{
			this.host_socket = host_socket;
		}
		
		@Override
		public final Message call() throws Exception
		{
			Message msg = null;
			
			try
			{
				final ObjectInputStream ois = new ObjectInputStream(host_socket.getInputStream());
				msg = (Message) ois.readObject();
			} catch (StreamCorruptedException sce)
			{
				sce.printStackTrace();
			} catch (IOException ioe)
			{
				ioe.printStackTrace();
			} catch (ClassNotFoundException cnfe)
			{
				cnfe.printStackTrace();
			}
			
			return msg;
		}
		
	}
}
