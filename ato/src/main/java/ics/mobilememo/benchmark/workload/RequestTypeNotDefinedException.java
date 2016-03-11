/**
 * @author hengxin
 * @date Jun 19, 2014
 * @description exception indicating that no such request type
 */
package ics.mobilememo.benchmark.workload;

public class RequestTypeNotDefinedException extends Exception
{
	private static final long serialVersionUID = -5325597649044168116L;

	public RequestTypeNotDefinedException(String msg)
	{
		super(msg);
	}
}