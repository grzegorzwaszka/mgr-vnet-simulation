
package cloudsim.grzegorz;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.network.datacenter.HostPacket;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkVm;
import org.cloudbus.cloudsim.power.PowerVm;

/**
 * VnetVm 
 * 
 * based on NetworkVm by 
 * @author Saurabh Kumar Garg
 * @since CloudSim Toolkit 3.0
 */
public class VnetVm extends PowerVm implements Comparable<Object>{

	private String vnet;
	
	public ArrayList<NetworkCloudlet> cloudletlist;

	int type;

	public ArrayList<HostPacket> recvPktlist;

	public double memory;

	public boolean flagfree;

	public double finishtime;

	public boolean isFree() {
		return flagfree;
	}
	
	public VnetVm(
			final int id,
			final int userId,
			final double mips,
			final int pesNumber,
			final int ram,
			final long bw,
			final long size,
			final int priority,
			final String vmm,
			final CloudletScheduler cloudletScheduler,
			final double schedulingInterval,
			final String vnet) {
		
		super(id, userId, mips, pesNumber, ram, bw, size, priority, vmm, cloudletScheduler, schedulingInterval);
		cloudletlist = new ArrayList<NetworkCloudlet>();
		setVnet(vnet);
		recvPktlist = new ArrayList<HostPacket>();
		
		
	}
	
	public String getVnet() {
		return this.vnet;
	}
	
	public void setVnet(String vnet){
		this.vnet = vnet;
	}
	
	public ArrayList<HostPacket> getRecvPktlist(){
		return  this.recvPktlist;
	}
	
	@Override
	public int compareTo(Object arg0) {
		NetworkVm hs = (NetworkVm) arg0;
		if (hs.finishtime > finishtime) {
			return -1;
		}
		if (hs.finishtime < finishtime) {
			return 1;
		}
		return 0;
	}
}
