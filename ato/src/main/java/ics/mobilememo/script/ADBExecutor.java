package ics.mobilememo.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * an executor of adb commands
 * See <a herf http://developer.android.com/	tools/help/adb.html>Android Debug Bridge</a>
 * @author hengxin
 * @date Jul 1, 2014
 */
public class ADBExecutor
{
	public static final int ANDROID_PORT = 30000;
	public static final int HOST_BASE_PORT = 35000;
	
	private final Map<String, String> deviceid_hostname_map = new HashMap<>();
	
	/**
	 * directory of "adb.exe"
	 */
	private final String adb_directory;
	
	/**
	 * constructor of {@link ADBExecutor}
	 * @param adb_directory {@link #adb_directory}: directory of "adb.exe"
	 */
	public ADBExecutor(String adb_directory)
	{
		this.adb_directory = adb_directory;

		// refine execAdbDevices function to return id-name pair
		/*this.deviceid_hostname_map.put("429a40a2", "S4");
		this.deviceid_hostname_map.put("06701c69f0ec9b5b", "Nexus0");
		this.deviceid_hostname_map.put("067125a40acc819e", "Nexus3");
		this.deviceid_hostname_map.put("064839200acc763d", "Nexus33");
		this.deviceid_hostname_map.put("c1607e301251d4f", "Tablet");*/

		this.deviceid_hostname_map.putAll(execAdbDevices());
	}
	
	/**
	 * execute the ADB command "adb devices" to get online device ids 
	 * 
	 * @return a list of online device ids
	 */
	public Map<String, String> execAdbDevices()
	{
		System.out.println("adb devices -l");
		
		Map<String, String> ret_device_info_list = new HashMap<>();
		Process proc = null;
		try
		{
			proc = new ProcessBuilder(this.adb_directory, "devices").start();
			proc.waitFor();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		} catch (InterruptedException ire)
		{
			ire.printStackTrace();
		}
		
		String devices_result = this.collectResultFromProcess(proc);
	    String[] device_info_list = devices_result.split("\\r?\\n");

	    if (device_info_list.length <= 1)
	    {
	    	System.out.println("No Devices Attached.");
	    	return ret_device_info_list;
	    }
	    
	    /**
	     * collect the online devices 
	     */
	    String str_device_info = null;
	    String device_id = null;
		String device_name = null;
	    String[] str_device_info_parts = null;
	    // ignore the first line which is "List of devices attached"
	    for (int i = 1; i < device_info_list.length; i++)
		{
			str_device_info = device_info_list[i];
			str_device_info_parts = str_device_info.split("\\t| ");
			// add the online device
			for (int j = 0; j < str_device_info_parts.length; j++)
			{
				String info = str_device_info_parts[j];
				if (j == 0)
					device_id = info;
				String[] subparts = info.split(":");
				if (subparts[0].equals("model"))
					device_name = subparts[1];
			}
			ret_device_info_list.put(device_id, device_name);

		}
	    
	    return ret_device_info_list;
	}
	
	/**
	 * execute the "adb -s <device> forward" commands for each device with different ports on the host PC
	 * @return map of (device, hostport)
	 */
	public Map<String, Integer> execAdbOnlineDevicesPortForward()
	{
		List<String> device_id_list = new ArrayList<>(this.execAdbDevices().keySet());
		Map<String, Integer> device_hostport_map = new HashMap<String, Integer>();
		
		int index = 0;
		for (String device : device_id_list)
		{
			int host_port = ADBExecutor.HOST_BASE_PORT + index * 10;
			this.execAdbSingleDevicePortForward(device, host_port, ADBExecutor.ANDROID_PORT);
			device_hostport_map.put(device, host_port);
			index++;
		}
		return device_hostport_map;
	}
	
	/**
	 * forwarding the @param host_port of PC to the @param to_part of the device (e.g., an Android phone)
	 * @param device_id forwarding a port to which device
	 * @param host_port port of PC
	 * @param to_port port of device (e.g., an Android phone)
	 */
	public void execAdbSingleDevicePortForward(String device_id, int host_port, int to_port)
	{
		System.out.println("adb -s " + device_id + " forward tcp:" + host_port + " tcp:" + to_port);
		
		Process proc = null;
		try
		{
			proc = new ProcessBuilder(this.adb_directory, "-s", device_id, "forward", "tcp:" + host_port, "tcp:" + to_port).start();
			proc.waitFor();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		} catch (InterruptedException ire)
		{
			ire.printStackTrace();
		}
	}
	
	/**
	 * uninstall the specified apk from all the attached devices 
	 * @param apk apk to uninstall
	 */
	public void uninstall(String apk)
	{
		for (String device : new ArrayList<>(this.execAdbDevices().keySet()))
			this.uninstall(device, apk);
	}
	
	/**
	 * uninstall the specified apk from the specified device
	 * @param device_id the device from which some apk is uninstalled
	 * @param apk apk to uninstall
	 */
	public void uninstall(String device_id, String apk)
	{
		System.out.println("adb -s " + device_id + " shell pm uninstall " + apk);
		
		Process proc = null;
		try
		{
			proc = new ProcessBuilder(this.adb_directory, "-s", device_id, "shell", "pm", "uninstall", apk).start();
			proc.waitFor();
		} catch (InterruptedException ire)
		{
			ire.printStackTrace();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		String uninstall_result = this.collectResultFromProcess(proc);
		System.out.println(uninstall_result);
	}
	
	/**
	 * copy a file or directory (and its sub-directories) (specified by @param src_path)
	 * from the device specified by @param device_id
	 * to the destination directory in computer specified by @param dest_path
	 * @param device_id the device from which the file/directory is copied
	 * @param src_path path of the file or directory to copy
	 * @param dest_path path of directory in which copied file/directory is stored
	 */
	public void copy(String device_id, String src_path, String dest_path)
	{
		System.out.println("adb -s " + device_id + " pull " + src_path + " " + dest_path);
		
		Process proc = null;
		try
		{
			proc = new ProcessBuilder(this.adb_directory, "-s", device_id, "pull", src_path, dest_path).start();
			proc.waitFor();
		} catch (InterruptedException ire)
		{
			ire.printStackTrace();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		String result = this.collectResultFromProcess(proc);
		System.out.println(result);
	}
	
	/**
	 * For each device, copy a file or directory (and its sub-directories) 
	 * specified by @param src_path to the destination directory in computer
	 * specified by @param dest_path
	 * 
	 * @param src_path path of the file or directory to copy
	 * @param dest_path path of directory in which copied file/directory is stored
	 */
	public void copyFromAll(String src_path, String dest_path)
	{
		String sub_directory = null;
		for (String device : new ArrayList<>(this.execAdbDevices().keySet()))
		{
			sub_directory = this.deviceid_hostname_map.get(device);
			this.copy(device, src_path, dest_path + "\\" + sub_directory);
		}
	}
	
	/**
	 * remove file or directory (and its sub-directories recursively)
	 * from all attached devices
	 * @param path specified file or directory to remove
	 */
	public void removeFromAll(String path)
	{
		for (String device : new ArrayList<>(this.execAdbDevices().keySet()))
			this.remove(device, path);
	}
	
	/**
	 * remove file or directory (and its sub-directories recursively)
	 * specified by @param path from device specified by @param device_id 
	 * @param device_id device from which the file or directory is removed
	 * @param path file or directory to remove
	 */
	public void remove(String device_id, String path)
	{
		System.out.println("adb -s " + device_id + " shell rm -r " + path);
		
		Process proc = null;
		try
		{
			proc = new ProcessBuilder(this.adb_directory, "-s", device_id, "shell", "rm", "-r", path).start();
			proc.waitFor();
		} catch (InterruptedException ire)
		{
			ire.printStackTrace();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		
		String result = this.collectResultFromProcess(proc);
		System.out.println(result);
	}
	
	/**
	 * collect results (including error info.) from the process executing ADB command
	 * @param proc the process executing ADB command
	 * @return results collected from the process executing ADB command
	 */
	private String collectResultFromProcess(Process proc)
	{
		StringBuilder sb_result = new StringBuilder();
		
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String result_line = null;
        
        try
        {
	        while ((result_line = stdInput.readLine()) != null) 
	        {
	            sb_result.append(result_line);
	            sb_result.append("\n");
	        }
	        
	        while ((result_line = stdError.readLine()) != null) 
	        {
	            sb_result.append(result_line);
	            sb_result.append("\n");
	        }
        } catch(IOException ioe)
        {
        	ioe.printStackTrace();
        }
        
        return sb_result.toString();
	}
	
}
